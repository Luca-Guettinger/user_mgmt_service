package com.example.jwt.domain.module;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * The only way into the module service. It is reached synchronously over REST through its
 * Kubernetes Service (MODULE_SERVICE_URL, which the Helm chart renders from the Service name),
 * never through its database.
 *
 * <p>Three things guard the call. The timeout sits on the RestClient below, because resilience4j's
 * TimeLimiter would force an asynchronous return type for no gain. Retry and CircuitBreaker are the
 * two annotations on {@link #assign}: one public method, so there is one proxy and one fallback.
 *
 * <p>The fallback belongs on {@code @Retry}, not on {@code @CircuitBreaker}. Retry is the outer
 * aspect: a fallback further in would turn the failure into its own exception before Retry ever got
 * to see - and retry - the original one.
 */
@Component
public class ModuleClient {

  private final RestClient http;
  private final Logger logger;

  public ModuleClient(
      @Value("${modules.base-url}") String baseUrl,
      @Value("${modules.connect-timeout}") Duration connectTimeout,
      @Value("${modules.read-timeout}") Duration readTimeout,
      Logger logger) {
    this.logger = logger;

    // Both timeouts, on the JDK's own HTTP client: connect bounds the handshake,
    // read bounds a module service that accepted the connection and then went quiet.
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
        HttpClient.newBuilder().connectTimeout(connectTimeout).build());
    factory.setReadTimeout(readTimeout);

    this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
  }

  /**
   * Checks that the module is available and then assigns it to the user.
   *
   * <p>A retry re-runs both calls, which is safe: the lookup is a GET and the assignment is
   * idempotent on the module service's side.
   */
  @Retry(name = "moduleService", fallbackMethod = "unavailable")
  @CircuitBreaker(name = "moduleService")
  public void assign(UUID userId, UUID moduleId) {
    if (!isAvailable(moduleId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          String.format("Module '%s' is not available", moduleId));
    }

    http.put()
        .uri("/api/v1/users/{userId}/modules/{moduleId}", userId, moduleId)
        .retrieve()
        .toBodilessEntity();
  }

  /** Every module the module service knows, for the catalogue in the portal. */
  @Retry(name = "moduleService", fallbackMethod = "listUnavailable")
  @CircuitBreaker(name = "moduleService")
  public List<ModuleDto> list() {
    return http.get()
        .uri("/api/v1/modules")
        .retrieve()
        .body(new ParameterizedTypeReference<List<ModuleDto>>() {
        });
  }

  @SuppressWarnings("unused")
  private List<ModuleDto> listUnavailable(Throwable cause) {
    logger.warn("Module service unreachable while listing modules: {}", cause.toString());
    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
        "Module service is currently unavailable");
  }

  private boolean isAvailable(UUID moduleId) {
    return http.get()
        .uri("/api/v1/modules/{moduleId}", moduleId)
        .retrieve()
        // A 404 is an answer, not a failure: it must not be retried and must not count
        // towards opening the breaker.
        .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
        })
        .toBodilessEntity()
        .getStatusCode()
        .is2xxSuccessful();
  }

  /**
   * Runs when the module service is unreachable or too slow and the retries are spent, or when the
   * breaker is open and the call was never attempted. The signature must match {@link #assign} plus
   * the throwable.
   */
  @SuppressWarnings("unused")
  private void unavailable(UUID userId, UUID moduleId, Throwable cause) {
    // "Module not available" is a verdict from the module service, not an outage. It reaches
    // here only because a fallback sees every exception, so hand it straight back.
    if (cause instanceof ResponseStatusException verdict) {
      throw verdict;
    }

    logger.warn("Module service unreachable while assigning {} to {}: {}",
        moduleId, userId, cause.toString());
    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
        "Module service is currently unavailable");
  }
}

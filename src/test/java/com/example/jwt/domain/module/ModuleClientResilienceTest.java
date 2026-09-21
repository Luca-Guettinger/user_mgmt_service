package com.example.jwt.domain.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.jwt.core.logging.LoggerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * The resilience around the module service call - the part of Aufgabe 6 that cannot be checked by
 * reading the code: whether the aspects are applied at all, and in which order.
 *
 * <p>No module service is started. The base URL points at port 1, which refuses the connection
 * immediately, so every call fails the way a dead module service would.
 *
 * <p>The thresholds mirror application.properties, except for a shorter wait between retries.
 */
@SpringBootTest(
    classes = ModuleClientResilienceTest.TestApp.class,
    properties = {
        "modules.base-url=http://127.0.0.1:1",
        "modules.connect-timeout=200ms",
        "modules.read-timeout=200ms",
        "resilience4j.retry.instances.moduleService.max-attempts=3",
        "resilience4j.retry.instances.moduleService.wait-duration=10ms",
        "resilience4j.retry.instances.moduleService.retry-exceptions="
            + "org.springframework.web.client.ResourceAccessException,"
            + "org.springframework.web.client.HttpServerErrorException",
        "resilience4j.circuitbreaker.instances.moduleService.sliding-window-size=10",
        "resilience4j.circuitbreaker.instances.moduleService.minimum-number-of-calls=5",
        "resilience4j.circuitbreaker.instances.moduleService.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.moduleService.record-exceptions="
            + "org.springframework.web.client.ResourceAccessException,"
            + "org.springframework.web.client.HttpServerErrorException",
        "resilience4j.circuitbreaker.instances.moduleService.register-health-indicator=false",
    })
@Import({ModuleClient.class, LoggerConfig.class})
@ImportAutoConfiguration({
    io.github.resilience4j.springboot.retry.autoconfigure.RetryAutoConfiguration.class,
    io.github.resilience4j.springboot.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration.class,
    org.springframework.boot.autoconfigure.aop.AopAutoConfiguration.class,
})
class ModuleClientResilienceTest {

  /** Just enough of an application for the two aspects to be applied to ModuleClient. */
  static class TestApp {

  }

  @Autowired
  ModuleClient client;
  @Autowired
  CircuitBreakerRegistry circuitBreakerRegistry;

  private CircuitBreaker freshBreaker() {
    CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("moduleService");
    breaker.reset();
    return breaker;
  }

  @Test
  void anUnreachableModuleServiceIsRetriedAndThenAnswered503() {
    CircuitBreaker breaker = freshBreaker();

    assertThatThrownBy(() -> client.assign(UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

    // Three failures recorded for one assign(): Retry is the outer aspect and really did
    // attempt the call max-attempts times before the fallback answered.
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(3);
    // Three is below minimum-number-of-calls, so one bad request does not trip the breaker.
    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
  }

  @Test
  void enoughFailuresOpenTheBreakerAndFurtherCallsStopHittingTheNetwork() {
    CircuitBreaker breaker = freshBreaker();

    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(() -> client.assign(UUID.randomUUID(), UUID.randomUUID()))
          .isInstanceOf(ResponseStatusException.class);
    }

    assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    long failuresSoFar = breaker.getMetrics().getNumberOfFailedCalls();

    assertThatThrownBy(() -> client.assign(UUID.randomUUID(), UUID.randomUUID()))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

    // Still a 503, but the breaker refused it outright instead of three more attempts.
    assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(failuresSoFar);
  }
}

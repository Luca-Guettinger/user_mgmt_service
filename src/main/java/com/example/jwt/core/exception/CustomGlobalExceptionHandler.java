package com.example.jwt.core.exception;

import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@RestControllerAdvice
public class CustomGlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ResponseError handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    return new ResponseError()
        .setTimeStamp(LocalDate.now())
        .setErrors(ex.getBindingResult().getFieldErrors().stream().collect(
            Collectors.toMap(error -> error.getField(), error -> error.getDefaultMessage())))
        .build();
  }

  /**
   * A body Jackson could not turn into the DTO: malformed JSON, or a field the DTO does not have.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(value = HttpStatus.BAD_REQUEST)
  public ResponseError handleUnreadableBody(HttpMessageNotReadableException ex) {
    String message = ex.getCause() instanceof UnrecognizedPropertyException unknown
        ? String.format("Unknown field '%s'", unknown.getPropertyName())
        : "Request body is not valid JSON";
    return error("body", message);
  }

  /**
   * A unique constraint. Only one of them is reachable from an endpoint - the email address on
   * register - but the message stays general, because the handler cannot tell which constraint
   * failed without parsing the driver's message.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(value = HttpStatus.CONFLICT)
  public ResponseError handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    return error("conflict", "A record with these values already exists");
  }

  private ResponseError error(String field, String message) {
    return new ResponseError()
        .setTimeStamp(LocalDate.now())
        .setErrors(Map.of(field, message))
        .build();
  }

}



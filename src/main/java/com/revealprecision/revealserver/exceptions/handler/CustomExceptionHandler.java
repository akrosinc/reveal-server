package com.revealprecision.revealserver.exceptions.handler;

import com.revealprecision.revealserver.exceptions.ConflictException;
import com.revealprecision.revealserver.exceptions.FileFormatException;
import com.revealprecision.revealserver.exceptions.InvalidDateFormatException;
import com.revealprecision.revealserver.exceptions.KeycloakException;
import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.WrongEnumException;
import com.revealprecision.revealserver.exceptions.dto.ApiErrorResponse;
import com.revealprecision.revealserver.exceptions.dto.ValidationErrorResponse;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  protected ResponseEntity<ApiErrorResponse> handleRegexError(MethodArgumentNotValidException ex) {

    var binding = ex.getBindingResult();
    var errors = binding.getFieldErrors()
        .stream()
        .map(fieldError -> ValidationErrorResponse.builder()
            .field(fieldError.getField())
            .rejectedValue(fieldError.getRejectedValue() == null ? ""
                : fieldError.getRejectedValue().toString())
            .messageKey(fieldError.getDefaultMessage()).build()).collect(Collectors.toList());
    ApiErrorResponse response = ApiErrorResponse.builder()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .fieldValidationErrors(errors)
        .timestamp(LocalDateTime.now()).build();

    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(NotFoundException.class)
  protected ResponseEntity<ApiErrorResponse> handleResourceNotFound(NotFoundException ex) {
    ApiErrorResponse response = ApiErrorResponse.builder()
        .statusCode(HttpStatus.NOT_FOUND.value())
        .timestamp(LocalDateTime.now())
        .message(ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(ConflictException.class)
  protected ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
    ApiErrorResponse response = ApiErrorResponse.builder()
        .statusCode(HttpStatus.CONFLICT.value())
        .timestamp(LocalDateTime.now())
        .message(ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(KeycloakException.class)
  protected ResponseEntity<ApiErrorResponse> handleKeycloakException(KeycloakException ex) {
    ApiErrorResponse response = ApiErrorResponse.builder()
        .statusCode(HttpStatus.CONFLICT.value())
        .timestamp(LocalDateTime.now())
        .message(ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(FileFormatException.class)
  protected ResponseEntity<ApiErrorResponse> handleFileFormatException(FileFormatException ex) {
    ApiErrorResponse response = ApiErrorResponse.builder()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .timestamp(LocalDateTime.now())
        .message(ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  protected ResponseEntity<ApiErrorResponse> handleViolationException(
      ConstraintViolationException ex) {
    ApiErrorResponse response = ApiErrorResponse.builder()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .timestamp(LocalDateTime.now())
        .message(ex.getMessage().split(":")[1].trim()).build();
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  protected ResponseEntity<ApiErrorResponse> handleDataIntegrityException(
      DataIntegrityViolationException ex) {
    ApiErrorResponse response = ApiErrorResponse.builder()
        .statusCode(HttpStatus.CONFLICT.value())
        .timestamp(LocalDateTime.now())
        .message(ex.getCause().getCause().getMessage().split("Detail:")[1].trim()).build();
    return new ResponseEntity<>(response, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(WrongEnumException.class)
  protected ResponseEntity<ApiErrorResponse> wrongEnumException(WrongEnumException ex) {
    ApiErrorResponse response = ApiErrorResponse.builder()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .timestamp(LocalDateTime.now())
        .message(ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(InvalidDateFormatException.class)
  protected ResponseEntity<ApiErrorResponse> handleKeycloakException(InvalidDateFormatException ex) {
    ApiErrorResponse response = ApiErrorResponse.builder()
        .statusCode(HttpStatus.BAD_REQUEST.value())
        .timestamp(LocalDateTime.now())
        .message(ex.getMessage()).build();
    return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
  }


}

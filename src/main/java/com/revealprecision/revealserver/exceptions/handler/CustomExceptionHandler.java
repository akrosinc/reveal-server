package com.revealprecision.revealserver.exceptions.handler;

import com.revealprecision.revealserver.exceptions.NotFoundException;
import com.revealprecision.revealserver.exceptions.dto.ApiErrorResponse;
import com.revealprecision.revealserver.exceptions.dto.ValidationErrorResponse;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<?> handleRegexError(MethodArgumentNotValidException ex){

        var binding = ex.getBindingResult();
        var errors = binding.getFieldErrors()
                .stream()
                .map(fieldError -> ValidationErrorResponse.builder()
                        .field(fieldError.getField())
                        .rejectedValue(fieldError.getRejectedValue() == null ? "" : fieldError.getRejectedValue().toString())
                        .messageKey(fieldError.getDefaultMessage()).build()).collect(Collectors.toList());
        ApiErrorResponse response = ApiErrorResponse.builder()
                                        .statusCode(HttpStatus.BAD_REQUEST.value())
                                        .fieldValidationErrors(errors)
                                        .timestamp(LocalDateTime.now()).build();

        return new ResponseEntity<ApiErrorResponse>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NotFoundException.class)
    protected ResponseEntity<ApiErrorResponse> handleResourceNotFound(NotFoundException ex) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .timestamp(LocalDateTime.now())
                .message(ex.getMessage()).build();
        return new ResponseEntity<ApiErrorResponse>(response, HttpStatus.NOT_FOUND);
    }
}

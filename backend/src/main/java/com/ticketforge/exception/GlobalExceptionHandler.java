package com.ticketforge.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized Global Exception Handler providing RFC 7807 Problem Details
 * for all REST API endpoints across TicketForge.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TIMESTAMP_KEY = "timestamp";

    @ExceptionHandler(SeatNotFoundException.class)
    public ProblemDetail handleSeatNotFound(SeatNotFoundException ex) {
        log.warn("Seat not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Seat Not Found");
        problem.setType(URI.create("https://ticketforge.com/errors/seat-not-found"));
        problem.setProperty(TIMESTAMP_KEY, Instant.now());
        return problem;
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ProblemDetail handleReservationNotFound(ReservationNotFoundException ex) {
        log.warn("Reservation not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Reservation Not Found");
        problem.setType(URI.create("https://ticketforge.com/errors/reservation-not-found"));
        problem.setProperty(TIMESTAMP_KEY, Instant.now());
        return problem;
    }

    @ExceptionHandler(UserAlreadyReservedException.class)
    public ProblemDetail handleUserAlreadyReserved(UserAlreadyReservedException ex) {
        log.warn("Reservation conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("User Already Has Reservation");
        problem.setType(URI.create("https://ticketforge.com/errors/user-already-reserved"));
        problem.setProperty(TIMESTAMP_KEY, Instant.now());
        return problem;
    }

    @ExceptionHandler(UserAlreadyInWaitlistException.class)
    public ProblemDetail handleUserAlreadyInWaitlist(UserAlreadyInWaitlistException ex) {
        log.warn("Waitlist conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("User Already In Waitlist");
        problem.setType(URI.create("https://ticketforge.com/errors/user-already-in-waitlist"));
        problem.setProperty(TIMESTAMP_KEY, Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Request");
        problem.setType(URI.create("https://ticketforge.com/errors/bad-request"));
        problem.setProperty(TIMESTAMP_KEY, Instant.now());
        return problem;
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimitExceeded(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setTitle("Rate Limit Exceeded");
        problem.setType(URI.create("https://ticketforge.com/errors/rate-limit-exceeded"));
        problem.setProperty(TIMESTAMP_KEY, Instant.now());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "60")
                .body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Access Denied: You do not have sufficient permissions to perform this operation");
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://ticketforge.com/errors/forbidden"));
        problem.setProperty(TIMESTAMP_KEY, Instant.now());
        return problem;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Authentication required: Invalid or missing bearer token");
        problem.setTitle("Unauthorized");
        problem.setType(URI.create("https://ticketforge.com/errors/unauthorized"));
        problem.setProperty(TIMESTAMP_KEY, Instant.now());
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        log.warn("Validation error on request: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for request parameters");
        problem.setTitle("Validation Error");
        problem.setType(URI.create("https://ticketforge.com/errors/validation-error"));
        problem.setProperty("invalidParams", errors);
        problem.setProperty(TIMESTAMP_KEY, Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex) {
        log.error("Unhandled server exception: ", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null ? ex.getMessage() : "An unexpected internal server error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://ticketforge.com/errors/internal-server-error"));
        problem.setProperty(TIMESTAMP_KEY, Instant.now());
        return problem;
    }
}

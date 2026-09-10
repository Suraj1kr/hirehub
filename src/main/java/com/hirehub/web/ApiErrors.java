package com.hirehub.web;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.*;
@RestControllerAdvice(assignableTypes=ApiController.class)
public class ApiErrors {
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<ProblemDetail> business(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(ProblemDetail.forStatusAndDetail(e.getStatusCode(),e.getReason()));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException e){String message=e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).distinct().reduce((a,b)->a+"; "+b).orElse("Invalid input");return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,message));}
 @ExceptionHandler({DataIntegrityViolationException.class,OptimisticLockingFailureException.class}) ResponseEntity<ProblemDetail> conflict(Exception e){return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Record already exists or changed; refresh and retry"));}
}

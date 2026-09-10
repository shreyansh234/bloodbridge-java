package com.bloodbridge;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> known(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("error",e.getReason()==null?"Request failed.":e.getReason()));}
 @ExceptionHandler(AuthenticationException.class) ResponseEntity<?> auth(){return ResponseEntity.status(401).body(Map.of("error","Invalid email or password."));}
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){var field=e.getBindingResult().getFieldErrors().stream().findFirst();return ResponseEntity.badRequest().body(Map.of("error",field.map(f->f.getField()+": "+f.getDefaultMessage()).orElse("Check your details.")));}
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class}) ResponseEntity<?> malformed(){return ResponseEntity.badRequest().body(Map.of("error","Check the submitted details."));}
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict(){return ResponseEntity.status(409).body(Map.of("error","These details already exist or have changed. Please refresh and try again."));}
 @ExceptionHandler(Exception.class) ResponseEntity<?> unavailable(Exception e){org.slf4j.LoggerFactory.getLogger(ApiErrors.class).error("BloodBridge service error: {}",e.getClass().getSimpleName());return ResponseEntity.status(503).body(Map.of("error","BloodBridge is temporarily unavailable. Please try again."));}
}

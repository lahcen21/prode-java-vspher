package local.ngcloud.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.rmi.RemoteException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(HttpMessageNotReadableException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("message", "Le corps de la requête JSON est manquant ou malformé.");
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Gère les erreurs de validation (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Error");
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        body.put("details", errors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Gère les RuntimeException lancées par le service (ex: VM non trouvée, Template invalide)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleServiceRuntimeException(RuntimeException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value()); // Souvent un 400 pour une erreur métier côté client
        body.put("error", "Service Error");
        body.put("message", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Gère les erreurs d'authentification vSphere (RemoteException avec message spécifique)
    @ExceptionHandler(RemoteException.class)
    public ResponseEntity<Map<String, Object>> handleVsphereAuthException(RemoteException ex) {
        Map<String, Object> body = new HashMap<>();
        String fullStackTrace = ex.toString(); // On cherche dans toute la chaîne d'erreur
        
        if (fullStackTrace.contains("Cannot complete login due to an incorrect user name or password.")) {
            body.put("status", HttpStatus.UNAUTHORIZED.value());
            body.put("error", "Unauthorized");
            body.put("message", "Échec de l'authentification vSphere : Nom d'utilisateur ou mot de passe incorrect.");
            return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
        }
        return handleAllExceptions(ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "vSphere API Error");
        body.put("message", ex.getMessage());
        
        // En environnement pro, on loggerait la stacktrace ici avec SLF4J
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
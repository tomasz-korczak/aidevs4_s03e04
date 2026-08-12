package pl.tomaszko.s03e04.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pl.tomaszko.s03e04.service.OutputConstraintValidator;

@RestControllerAdvice
public class SearchExceptionHandler {

    private final OutputConstraintValidator outputConstraintValidator;

    public SearchExceptionHandler(OutputConstraintValidator outputConstraintValidator) {
        this.outputConstraintValidator = outputConstraintValidator;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<SearchResponse> badRequest(Exception ex) {
        String output = outputConstraintValidator.enforce("params must be a non-empty JSON object");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new SearchResponse(output));
    }
}

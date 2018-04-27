package ml.pevgen.s5reactivetest1;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.concurrent.TimeoutException;

@ControllerAdvice
public class WebFluxErrorHandler {

    @ExceptionHandler(SpecificException.class)
    public SpecificException handleNotFoundException(SpecificException ex) {
        return ex;
    }

    @ExceptionHandler(TimeoutException.class)
    public TimeoutException handleNotFoundException(TimeoutException ex) {
        return ex;
    }

}

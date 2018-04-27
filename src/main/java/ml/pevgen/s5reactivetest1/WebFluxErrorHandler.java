package ml.pevgen.s5reactivetest1;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class WebFluxErrorHandler {

    @ExceptionHandler(SpecificException.class)
    public SpecificException handleNotFoundException(SpecificException ex) {
        return ex;
    }

}

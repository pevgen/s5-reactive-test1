package ml.pevgen.s5reactivetest1;

public class SpecificException extends RuntimeException {
    public SpecificException(String specific_timeout) {
        super(specific_timeout);
    }
}

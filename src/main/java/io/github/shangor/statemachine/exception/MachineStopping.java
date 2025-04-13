package io.github.shangor.statemachine.exception;

public class MachineStopping extends RuntimeException {
    public MachineStopping(String message) {
        super(message);
    }
}

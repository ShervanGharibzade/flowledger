package org.fl.flowledger.common.exception;

public class BadCredentialsException extends RuntimeException{
    public BadCredentialsException() {
        super("Email or password is wrong.");
    }
}

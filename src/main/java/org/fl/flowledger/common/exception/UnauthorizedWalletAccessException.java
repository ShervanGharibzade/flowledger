package org.fl.flowledger.common.exception;

public class UnauthorizedWalletAccessException extends RuntimeException {
    public UnauthorizedWalletAccessException(String message) {
        super(message);
    }
}
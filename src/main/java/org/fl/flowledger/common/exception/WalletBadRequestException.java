package org.fl.flowledger.common.exception;

public class WalletBadRequestException extends RuntimeException{
    public WalletBadRequestException() {
        super("Wallet balance is not zero");
    }

    public WalletBadRequestException(String message) {
        super(message);
    }
}

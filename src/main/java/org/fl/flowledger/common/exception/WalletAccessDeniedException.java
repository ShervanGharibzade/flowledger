package org.fl.flowledger.common.exception;

public class WalletAccessDeniedException extends RuntimeException{
    public WalletAccessDeniedException() {
        super("User is not the owner of this wallet");
    }
}

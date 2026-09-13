package org.fl.flowledger.common.exception;

public class EmailAlreadyUsedException extends RuntimeException{
    public EmailAlreadyUsedException(){
        super("Email already used");
    }
}

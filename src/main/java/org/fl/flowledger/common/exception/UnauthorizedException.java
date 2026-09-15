package org.fl.flowledger.common.exception;

public class UnauthorizedException extends  RuntimeException{
    public UnauthorizedException(){
        super("User is not authenticated.");
    }
}

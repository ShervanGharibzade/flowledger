package org.fl.flowledger.common.exception;

public class UserNotFoundedException extends RuntimeException{
    public UserNotFoundedException(){
        super("User not founded");
    }
}

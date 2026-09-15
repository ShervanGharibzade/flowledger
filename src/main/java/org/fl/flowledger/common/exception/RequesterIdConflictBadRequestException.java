package org.fl.flowledger.common.exception;

public class RequesterIdConflictBadRequestException extends RuntimeException{
    public RequesterIdConflictBadRequestException() {
        super("Requester and target user must be different.");
    }
}

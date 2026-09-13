package org.fl.flowledger.common.exception;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String source, UUID uuid) {
        super("%s not found with uuid: %s".formatted(source, uuid));
    }

    public ResourceNotFoundException(String source, Long id) {
        super("%s not found with id: %d".formatted(source, id));
    }

    public ResourceNotFoundException(String resource, String field, String value) {
        super("%s not found with %s: %s".formatted(resource, field, value));
    }
}
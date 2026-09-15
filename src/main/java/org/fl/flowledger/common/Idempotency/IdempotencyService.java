package org.fl.flowledger.common.Idempotency;

import java.util.UUID;

public interface IdempotencyService {
    boolean reserve(String key);
    void complete(String key, UUID transferId);
    void delete(String key);
    String get(String key);
}

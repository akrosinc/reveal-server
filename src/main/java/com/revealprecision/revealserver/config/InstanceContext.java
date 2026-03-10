package com.revealprecision.revealserver.config;

import java.util.Optional;
import java.util.UUID;

public class InstanceContext {

    private static final ThreadLocal<UUID> CURRENT_INSTANCE =
        new ThreadLocal<>();

    public static void set(UUID instanceId) {
        CURRENT_INSTANCE.set(instanceId);
    }

    public static UUID get() {
        UUID instanceId = CURRENT_INSTANCE.get();
        if (instanceId == null) {
            throw new IllegalStateException("No instance context set");
        }
        return instanceId;
    }

    public static Optional<UUID> getOptional() {
        return Optional.ofNullable(CURRENT_INSTANCE.get());
    }

    public static void clear() {
        CURRENT_INSTANCE.remove();
    }
}
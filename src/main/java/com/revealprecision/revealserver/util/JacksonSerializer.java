package com.revealprecision.revealserver.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.ehcache.spi.serialization.Serializer;
import org.ehcache.core.spi.service.FileBasedPersistenceContext; // Only if using Disk
import java.nio.ByteBuffer;

public class JacksonSerializer<T> implements Serializer<T> {
    private final ObjectMapper mapper = new ObjectMapper();

    // 1. Mandatory constructor for transient caches
    public JacksonSerializer(ClassLoader classLoader) {
        BasicPolymorphicTypeValidator basicPolymorphicTypeValidator =
            BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.revealprecision.revealserver")
                .allowIfSubType("java.util")
                .allowIfSubType("java.lang")
                .build();

        this.mapper.activateDefaultTyping(
            basicPolymorphicTypeValidator,
            ObjectMapper.DefaultTyping.NON_FINAL
        );

        this.mapper.setTypeFactory(
            this.mapper.getTypeFactory()
                .withClassLoader(classLoader)
        );
    }

    // 2. Mandatory constructor for disk/persistent caches
    public JacksonSerializer(ClassLoader classLoader, FileBasedPersistenceContext persistenceContext) {
        this(classLoader);
    }

    @Override
    public ByteBuffer serialize(T object) {
        try {
            return ByteBuffer.wrap(mapper.writeValueAsBytes(object));
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    @Override
    public T read(ByteBuffer binary) throws ClassNotFoundException {
        try {
            byte[] bytes = new byte[binary.remaining()];
            binary.get(bytes);
            // Use the classLoader if needed for custom type resolution
            return (T) mapper.readValue(bytes, Object.class);
        } catch (Exception e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }

    @Override
    public boolean equals(T object, ByteBuffer binary) {
        try {
            return object.equals(read(binary));
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}

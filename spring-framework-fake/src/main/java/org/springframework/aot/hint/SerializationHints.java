package org.springframework.aot.hint;

import java.io.Serializable;

public class SerializationHints {

//    public Stream<JavaSerializationHint> javaSerializationHints() {
//        return null;
//    }
//
//    public SerializationHints registerType(TypeReference type, @Nullable Consumer<JavaSerializationHint.Builder> serializationHint) {
//        return this;
//    }

    public SerializationHints registerType(TypeReference type) {
        return this;
    }

//    public SerializationHints registerType(Class<? extends Serializable> type, @Nullable Consumer<JavaSerializationHint.Builder> serializationHint) {
//        return this;
//    }

    public SerializationHints registerType(Class<? extends Serializable> type) {
        return this;
    }

}

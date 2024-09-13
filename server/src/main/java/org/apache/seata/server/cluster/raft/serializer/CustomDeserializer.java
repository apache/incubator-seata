package org.apache.seata.server.cluster.raft.serializer;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * @author jianbin@apache.org
 */
public class CustomDeserializer extends JsonDeserializer<Class<?>> {

	String oldPackage = "io.seata.server";

	String currentPackage = "org.apache.seata.server";

	@Override
	public Class<?> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
		throws IOException {
		String className = jsonParser.getValueAsString();
		if (className.startsWith(oldPackage)) {
			className = className.replaceFirst(oldPackage, currentPackage);
		}
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

}

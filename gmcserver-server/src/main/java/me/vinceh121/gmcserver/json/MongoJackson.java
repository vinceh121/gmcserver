package me.vinceh121.gmcserver.json;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.vertx.core.json.jackson.DatabindCodec;

public final class MongoJackson {
	public static void registerSerializers() {
		final SimpleModule mod = new SimpleModule("MongoJackson");
		mod.addSerializer(ObjectId.class, new ObjectIdSerializer());
		mod.addDeserializer(ObjectId.class, new ObjectIdDeserializer());
		DatabindCodec.mapper().registerModule(mod);
	}

	public static class ObjectIdSerializer extends StdSerializer<ObjectId> {
		private static final long serialVersionUID = -7569231277640818925L;

		protected ObjectIdSerializer() {
			super(ObjectId.class);
		}

		@Override
		public void serialize(final ObjectId value, final JsonGenerator gen, final SerializerProvider provider)
				throws IOException {
			gen.writeString(value.toHexString());
		}
	}

	public static class ObjectIdDeserializer extends StdDeserializer<ObjectId> {
		private static final long serialVersionUID = 8138353122646405546L;

		protected ObjectIdDeserializer() {
			super(ObjectId.class);
		}

		@Override
		public ObjectId deserialize(final JsonParser p, final DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			return new ObjectId(p.getValueAsString());
		}
	}
}

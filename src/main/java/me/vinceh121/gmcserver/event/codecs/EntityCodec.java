package me.vinceh121.gmcserver.event.codecs;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.entities.AbstractEntity;

public class EntityCodec implements MessageCodec<AbstractEntity, AbstractEntity> {

	@Override
	public void encodeToWire(Buffer buffer, AbstractEntity s) {
		Buffer encoded = s.toJson().toBuffer();
		buffer.appendInt(encoded.length());
		buffer.appendBuffer(encoded);
	}

	@Override
	public AbstractEntity decodeFromWire(int pos, Buffer buffer) {
		int length = buffer.getInt(pos);
		pos += 4;
		return new JsonObject(buffer.slice(pos, pos + length)).mapTo(AbstractEntity.class);
	}

	@Override
	public AbstractEntity transform(AbstractEntity s) {
		return s;
	}

	@Override
	public String name() {
		return "gmcrecord";
	}

	@Override
	public byte systemCodecID() {
		return -1;
	}
}

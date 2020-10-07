package me.vinceh121.gmcserver.event;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.entities.AbstractEntity;

public class EntityCodec<T extends AbstractEntity> implements MessageCodec<T, T> {
	private final Class<T> clazz;

	public EntityCodec(final Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public void encodeToWire(final Buffer buffer, final T s) {
		final Buffer encoded = s.toJson().toBuffer();
		buffer.appendInt(encoded.length());
		buffer.appendBuffer(encoded);
	}

	@Override
	public T decodeFromWire(int pos, final Buffer buffer) {
		final int length = buffer.getInt(pos);
		pos += 4;
		return new JsonObject(buffer.slice(pos, pos + length)).mapTo(this.clazz);
	}

	@Override
	public T transform(final T s) {
		return s;
	}

	@Override
	public String name() {
		return "gmcrecord-" + this.clazz.getCanonicalName();
	}

	@Override
	public byte systemCodecID() {
		return -1;
	}
}

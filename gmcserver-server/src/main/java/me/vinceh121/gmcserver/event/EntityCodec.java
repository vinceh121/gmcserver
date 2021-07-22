/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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

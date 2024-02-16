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
package me.vinceh121.gmcserver.entities;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.jackson.DatabindCodec;

public abstract class AbstractEntity {
	private UUID id = UUID.randomUUID();

	public AbstractEntity() {
	}

	public UUID getId() {
		return this.id;
	}

	public void setId(final UUID id) {
		this.id = id;
	}

	public JsonObject toJson() {
		final JsonObject obj = JsonObject.mapFrom(this);
		return obj;
	}

	@JsonIgnore
	public JsonObject toPublicJson() {
		return this.toJson();
	}

	public static String sqlFields(final Class<? extends AbstractEntity> cls) {
		final List<BeanPropertyDefinition> list = DatabindCodec.mapper()
			.writerFor(cls)
			.getConfig()
			.introspect(DatabindCodec.mapper().constructType(cls))
			.findProperties();

		return list.stream().map(p -> "#{" + p.getName() + "}").collect(Collectors.joining(","));
	}
}

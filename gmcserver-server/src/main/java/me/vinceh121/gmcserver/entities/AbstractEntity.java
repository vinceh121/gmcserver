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

import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.vertx.core.json.JsonObject;

public abstract class AbstractEntity {
	private ObjectId id = new ObjectId();

	public AbstractEntity() {
		this.id = new ObjectId();
	}

	public ObjectId getId() {
		return this.id;
	}

	public void setId(final ObjectId id) {
		this.id = id;
	}

	public JsonObject toJson() {
		final JsonObject obj = JsonObject.mapFrom(this);
		return obj;
	}

	@JsonIgnore
	@BsonIgnore
	public JsonObject toPublicJson() {
		return this.toJson();
	}
}

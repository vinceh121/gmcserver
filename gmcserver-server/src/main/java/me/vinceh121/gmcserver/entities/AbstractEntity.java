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

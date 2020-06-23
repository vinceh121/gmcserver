package me.vinceh121.gmcserver.entities;

import java.util.Objects;

import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.vertx.core.json.JsonObject;

public abstract class AbstractEntity {
	private ObjectId id;

	public AbstractEntity() {
		id = new ObjectId();
	}

	public ObjectId getId() {
		return this.id;
	}

	public void setId(final ObjectId id) {
		this.id = id;
	}

	public JsonObject toJson() {
		final JsonObject obj = JsonObject.mapFrom(this);
		obj.put("id", Objects.toString(id));
		return obj;
	}

	@JsonIgnore
	@BsonIgnore
	public JsonObject toPublicJson() {
		return this.toJson();
	}
}

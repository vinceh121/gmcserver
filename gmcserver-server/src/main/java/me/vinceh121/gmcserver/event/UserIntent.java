package me.vinceh121.gmcserver.event;

import java.util.Objects;

import org.bson.types.ObjectId;

import io.vertx.core.json.JsonObject;

public class UserIntent {
	private final String name;
	private final ObjectId destination;
	private final JsonObject extras;

	public UserIntent(final String name, final ObjectId destination, final JsonObject extras) {
		this.name = name;
		this.destination = destination;
		this.extras = extras;
	}

	public UserIntent(final String name, final ObjectId destination) {
		this(name, destination, new JsonObject());
	}

	public String getName() {
		return this.name;
	}

	public ObjectId getDestination() {
		return destination;
	}

	public JsonObject getExtras() {
		return this.extras;
	}

	@Override
	public int hashCode() {
		return Objects.hash(destination, extras, name);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UserIntent)) {
			return false;
		}
		UserIntent other = (UserIntent) obj;
		return Objects.equals(destination, other.destination) && Objects.equals(extras, other.extras)
				&& Objects.equals(name, other.name);
	}

	public JsonObject toJson() {
		return JsonObject.mapFrom(this);
	}

	@Override
	public String toString() {
		return "Intent [name=" + this.name + ", extras=" + this.extras + "]";
	}

}

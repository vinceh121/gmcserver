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
		return this.destination;
	}

	public JsonObject getExtras() {
		return this.extras;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.destination, this.extras, this.name);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UserIntent)) {
			return false;
		}
		final UserIntent other = (UserIntent) obj;
		return Objects.equals(this.destination, other.destination) && Objects.equals(this.extras, other.extras)
				&& Objects.equals(this.name, other.name);
	}

	public JsonObject toJson() {
		return JsonObject.mapFrom(this);
	}

	@Override
	public String toString() {
		return "Intent [name=" + this.name + ", extras=" + this.extras + "]";
	}

}

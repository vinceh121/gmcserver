package me.vinceh121.gmcserver.event;

import org.bson.types.ObjectId;

import io.vertx.core.json.JsonObject;

public enum StandardUserIntent {
	HANDSHAKE_COMPLETE, LOG2_RECORD, LOG_CLASSIC_RECORD;

	public UserIntent create(final ObjectId destination) {
		return this.create(destination, new JsonObject());
	}

	public UserIntent create(final ObjectId destination, final JsonObject extras) {
		return new UserIntent(this.name(), destination, extras);
	}
}

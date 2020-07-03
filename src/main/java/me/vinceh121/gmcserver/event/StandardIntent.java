package me.vinceh121.gmcserver.event;

import io.vertx.core.json.JsonObject;

public enum StandardIntent {
	HANDSHAKE_COMPLETE, LOG2_RECORD, LOG_CLASSIC_RECORD;

	public Intent create(final JsonObject extras) {
		return new Intent(this.name(), extras);
	}
}

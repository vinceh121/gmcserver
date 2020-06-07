package me.vinceh121.gmcserver.entities;

import org.bson.types.ObjectId;

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
}

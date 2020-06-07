package me.vinceh121.gmcserver.entities;

import org.bson.types.ObjectId;

public class Device extends AbstractEntity {
	private String model;
	private ObjectId owner;
	private long gmcId;

	public long getGmcId() {
		return this.gmcId;
	}

	public void setGmcId(final long gmcId) {
		this.gmcId = gmcId;
	}

	public String getModel() {
		return this.model;
	}

	public void setModel(final String model) {
		this.model = model;
	}

	public ObjectId getOwner() {
		return this.owner;
	}

	public void setOwner(final ObjectId owner) {
		this.owner = owner;
	}

}

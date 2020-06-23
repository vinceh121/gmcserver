package me.vinceh121.gmcserver.entities;

import org.bson.types.ObjectId;

import com.mongodb.client.model.geojson.Point;

import io.vertx.core.json.JsonObject;

public class Device extends AbstractEntity {
	private String model, name;
	private Point location;
	private ObjectId owner;
	private long gmcId;

	public long getGmcId() {
		return this.gmcId;
	}

	public void setGmcId(final long gmcId) {
		this.gmcId = gmcId;
	}

	public ObjectId getOwner() {
		return this.owner;
	}

	public void setOwner(final ObjectId owner) {
		this.owner = owner;
	}

	public String getModel() {
		return this.model;
	}

	public void setModel(final String model) {
		this.model = model;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Point getLocation() {
		return location;
	}

	public void setLocation(Point location) {
		this.location = location;
	}

	@Override
	public JsonObject toJson() {
		final JsonObject obj = super.toJson();
		if (this.location != null)
			obj.put("location", this.location.getPosition().getValues());
		obj.put("owner", getOwner().toHexString());
		return obj;
	}

	@Override
	public JsonObject toPublicJson() {
		final JsonObject obj = super.toPublicJson();
		obj.remove("gmcId");
		return obj;
	}

}

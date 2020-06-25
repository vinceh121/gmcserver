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
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Point getLocation() {
		return this.location;
	}

	public void setLocation(final Point location) {
		this.location = location;
	}

	@Override
	public JsonObject toJson() {
		final JsonObject obj = super.toJson();
		if (this.location != null) {
			obj.put("location", this.location.getPosition().getValues());
		}
		obj.put("owner", this.getOwner().toHexString());
		return obj;
	}

	@Override
	public JsonObject toPublicJson() {
		final JsonObject obj = super.toPublicJson();
		obj.remove("gmcId");
		return obj;
	}

	public JsonObject toMapJson() {
		final JsonObject obj = new JsonObject();
		obj.put("id", this.getId().toHexString());
		obj.put("location", location.getCoordinates().getValues());
		return obj;
	}

}

/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.vinceh121.gmcserver.entities;

import java.util.Date;
import java.util.Map;

import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import com.mongodb.client.model.geojson.Point;

import io.vertx.core.json.JsonObject;

public class Device extends AbstractEntity {
	private String model, name, importedFrom;
	private Point location;
	private ObjectId owner, lastRecordId;
	private long gmcId;
	private boolean disabled;
	private Date lastEmailAlert = new Date(0L);
	private double stdDevAlertLimit = Double.NaN;
	private Map<String, Map<String, Object>> proxiesSettings;
	private Record lastRecord;

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

	public boolean isDisabled() {
		return this.disabled;
	}

	public void setDisabled(final boolean disabled) {
		this.disabled = disabled;
	}

	public String getImportedFrom() {
		return this.importedFrom;
	}

	public void setImportedFrom(final String importedFrom) {
		this.importedFrom = importedFrom;
	}

	public Date getLastEmailAlert() {
		return this.lastEmailAlert;
	}

	public void setLastEmailAlert(final Date lastEmailAlert) {
		this.lastEmailAlert = lastEmailAlert;
	}

	public double getStdDevAlertLimit() {
		return this.stdDevAlertLimit;
	}

	public void setStdDevAlertLimit(final double stdDevAlertLimit) {
		this.stdDevAlertLimit = stdDevAlertLimit;
	}

	/**
	 * @return map where keys are proxy names (their simple class names, also keys
	 *         in GMCServer#getProxies) and values are proxy-specific settings
	 */
	public Map<String, Map<String, Object>> getProxiesSettings() {
		return this.proxiesSettings;
	}

	/**
	 * @return map where keys are proxy names (their simple class names, also keys
	 *         in GMCServer#getProxies) and values are proxy-specific settings
	 */
	public void setProxiesSettings(final Map<String, Map<String, Object>> proxiesSettings) {
		this.proxiesSettings = proxiesSettings;
	}

	public ObjectId getLastRecordId() {
		return lastRecordId;
	}

	public Device setLastRecordId(ObjectId lastRecordId) {
		this.lastRecordId = lastRecordId;
		return this;
	}

	/**
	 * Aggregated field
	 */
	@BsonIgnore
	public Record getLastRecord() {
		return lastRecord;
	}

	public Device setLastRecord(Record lastRecord) {
		this.lastRecord = lastRecord;
		return this;
	}

	@Override
	public JsonObject toJson() {
		final JsonObject obj = super.toJson();
		if (this.location != null) {
			obj.put("location", this.location.getCoordinates().getValues());
		}
		obj.put("owner", this.getOwner().toHexString());
		return obj;
	}

	@Override
	public JsonObject toPublicJson() {
		final JsonObject obj = super.toPublicJson();
		obj.remove("gmcId");
		obj.remove("proxiesSettings");
		obj.remove("lastEmailAlert");
		obj.remove("stdDevAlertLimit");
		obj.remove("lastRecordId");
		if (this.getLastRecord() == null) {
			obj.remove("lastRecord");
		}
		return obj;
	}

	public JsonObject toMapJson() {
		final JsonObject obj = new JsonObject();
		obj.put("id", this.getId().toHexString());
		obj.put("name", this.getName());
		obj.put("location", this.location.getCoordinates().getValues());
		obj.put("lastRecord", this.getLastRecord().toPublicJson());
		return obj;
	}
}

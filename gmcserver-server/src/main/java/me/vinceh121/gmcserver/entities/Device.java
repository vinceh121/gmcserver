package me.vinceh121.gmcserver.entities;

import java.util.Date;
import java.util.Map;

import org.bson.types.ObjectId;

import com.mongodb.client.model.geojson.Point;

import io.vertx.core.json.JsonObject;

public class Device extends AbstractEntity {
	private String model, name, importedFrom;
	private Point location;
	private ObjectId owner;
	private long gmcId;
	private boolean disabled;
	private Date lastEmailAlert = new Date(0L);
	private double stdDevAlertLimit = Double.NaN;
	private Map<String, Map<String, Object>> proxiesSettings;

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
		return proxiesSettings;
	}

	/**
	 * @return map where keys are proxy names (their simple class names, also keys
	 *         in GMCServer#getProxies) and values are proxy-specific settings
	 */
	public void setProxiesSettings(final Map<String, Map<String, Object>> proxiesSettings) {
		this.proxiesSettings = proxiesSettings;
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
		return obj;
	}

	public JsonObject toMapJson() {
		final JsonObject obj = new JsonObject();
		obj.put("id", this.getId().toHexString());
		obj.put("name", this.getName());
		obj.put("location", this.location.getCoordinates().getValues());
		return obj;
	}
}

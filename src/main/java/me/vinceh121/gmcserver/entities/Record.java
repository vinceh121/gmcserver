package me.vinceh121.gmcserver.entities;

import java.util.Date;
import java.util.Objects;

import org.bson.codecs.pojo.annotations.BsonIgnore;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.vertx.core.json.JsonObject;

public class Record extends AbstractEntity {
	private ObjectId userId, deviceId;
	private double cpm, acpm, usv;
	private Date date;
	private String ip;

	public ObjectId getUserId() {
		return this.userId;
	}

	public void setUserId(final ObjectId userId) {
		this.userId = userId;
	}

	public ObjectId getDeviceId() {
		return this.deviceId;
	}

	public void setDeviceId(final ObjectId deviceId) {
		this.deviceId = deviceId;
	}

	public double getCpm() {
		return this.cpm;
	}

	public void setCpm(final double cpm) {
		this.cpm = cpm;
	}

	public double getAcpm() {
		return this.acpm;
	}

	public void setAcpm(final double acpm) {
		this.acpm = acpm;
	}

	public double getUsv() {
		return this.usv;
	}

	public void setUsv(final double usv) {
		this.usv = usv;
	}

	public Date getDate() {
		return this.date;
	}

	public void setDate(final Date date) {
		this.date = date;
	}

	public String getIp() {
		return this.ip;
	}

	public void setIp(final String ip) {
		this.ip = ip;
	}

	@Override
	@JsonIgnore
	@BsonIgnore
	public JsonObject toJson() {
		final JsonObject obj = super.toJson();
		obj.put("deviceId", Objects.toString(this.deviceId));
		obj.put("userId", Objects.toString(this.userId));
		return obj;
	}

	@Override
	@JsonIgnore
	@BsonIgnore
	public JsonObject toPublicJson() {
		final JsonObject obj = this.toJson();
		obj.remove("id");
		obj.remove("deviceId");
		obj.remove("userId");
		obj.remove("ip");
		return obj;
	}

	@Override
	public String toString() {
		return "Record [userId="
				+ this.userId
				+ ", deviceId="
				+ this.deviceId
				+ ", cpm="
				+ this.cpm
				+ ", acpm="
				+ this.acpm
				+ ", usv="
				+ this.usv
				+ ", date="
				+ this.date
				+ "]";
	}
}

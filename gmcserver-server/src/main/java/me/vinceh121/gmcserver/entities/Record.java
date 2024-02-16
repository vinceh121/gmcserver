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

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.data.Point;

public class Record extends AbstractEntity {
	public static final Collection<String> STAT_FIELDS = Arrays
		.asList("cpm", "acpm", "usv", "co2", "hcho", "tmp", "ap", "hmdt", "accy");

	// Taken from
	// https://github.com/radhoo/uradmonitor_kit1/blob/master/code/misc/expProtocol.h
	public static final List<String> URADMONITOR_FIELDS = Arrays.asList("date", // compulsory: local time in seconds
			"tmp", // optional: temperature in degrees celsius
			"press", // optional: barometric pressure in pascals TODO
			"hmdt", // optional: humidity as relative humidity in percentage %
			"lum", // optional: luminosity as relative luminosity in percentage ‰ TODO
			"voc", // optional: volatile organic compounds in ohms TODO
			"co2", // optional: carbon dioxide in ppm
			"hcho", // optional: formaldehyde in ppm
			"pm25", // optional: particulate matter in micro grams per cubic meter TODO
			"batt", // optional: device battery voltage in volts TODO
			"cpm", // optional: radiation measured on geiger tube in cpm
			"invertVolt", // optional: high voltage geiger tube inverter voltage in volts TODO
			"invertDuty", // optional: high voltage geiger tube inverter duty in ‰ TODO
			"versionHw", // optional: hardware version TODO
			"versionSw", // optional: software firmware version TODO
			"idTube" // optional: tube type ID TODO
	);

	private UUID deviceId;
	private double cpm = Double.NaN, acpm = Double.NaN, usv = Double.NaN, co2 = Double.NaN, hcho = Double.NaN,
			tmp = Double.NaN, ap = Double.NaN, hmdt = Double.NaN, accy = Double.NaN;
	private Date date;
	private String ip, type;
	private Point location; // FIXME Point is mutable, should be final

	public UUID getDeviceId() {
		return this.deviceId;
	}

	public void setDeviceId(final UUID deviceId) {
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

	public double getCo2() {
		return this.co2;
	}

	public void setCo2(final double co2) {
		this.co2 = co2;
	}

	public double getHcho() {
		return this.hcho;
	}

	public void setHcho(final double hcho) {
		this.hcho = hcho;
	}

	public double getTmp() {
		return this.tmp;
	}

	public void setTmp(final double tmp) {
		this.tmp = tmp;
	}

	public double getAp() {
		return this.ap;
	}

	public void setAp(final double ap) {
		this.ap = ap;
	}

	public double getHmdt() {
		return this.hmdt;
	}

	public void setHmdt(final double hmdt) {
		this.hmdt = hmdt;
	}

	public double getAccy() {
		return this.accy;
	}

	public void setAccy(final double accy) {
		this.accy = accy;
	}

	public String getType() {
		return this.type;
	}

	public void setType(final String type) {
		this.type = type;
	}

	public Point getLocation() {
		return this.location;
	}

	public void setLocation(final Point location) {
		this.location = location;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(this.accy);
		result = prime * result + (int) (temp ^ temp >>> 32);
		temp = Double.doubleToLongBits(this.acpm);
		result = prime * result + (int) (temp ^ temp >>> 32);
		temp = Double.doubleToLongBits(this.ap);
		result = prime * result + (int) (temp ^ temp >>> 32);
		temp = Double.doubleToLongBits(this.co2);
		result = prime * result + (int) (temp ^ temp >>> 32);
		temp = Double.doubleToLongBits(this.cpm);
		result = prime * result + (int) (temp ^ temp >>> 32);
		result = prime * result + (this.date == null ? 0 : this.date.hashCode());
		result = prime * result + (this.deviceId == null ? 0 : this.deviceId.hashCode());
		temp = Double.doubleToLongBits(this.hcho);
		result = prime * result + (int) (temp ^ temp >>> 32);
		temp = Double.doubleToLongBits(this.hmdt);
		result = prime * result + (int) (temp ^ temp >>> 32);
		result = prime * result + (this.ip == null ? 0 : this.ip.hashCode());
		result = prime * result + (this.location == null ? 0 : this.location.hashCode());
		temp = Double.doubleToLongBits(this.tmp);
		result = prime * result + (int) (temp ^ temp >>> 32);
		result = prime * result + (this.type == null ? 0 : this.type.hashCode());
		temp = Double.doubleToLongBits(this.usv);
		result = prime * result + (int) (temp ^ temp >>> 32);
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Record)) {
			return false;
		}
		final Record other = (Record) obj;
		if (Double.doubleToLongBits(this.accy) != Double.doubleToLongBits(other.accy)) {
			return false;
		}
		if (Double.doubleToLongBits(this.acpm) != Double.doubleToLongBits(other.acpm)) {
			return false;
		}
		if (Double.doubleToLongBits(this.ap) != Double.doubleToLongBits(other.ap)) {
			return false;
		}
		if (Double.doubleToLongBits(this.co2) != Double.doubleToLongBits(other.co2)) {
			return false;
		}
		if (Double.doubleToLongBits(this.cpm) != Double.doubleToLongBits(other.cpm)) {
			return false;
		}
		if (!Objects.equals(this.date, other.date)) {
			return false;
		}
		if (!Objects.equals(this.deviceId, other.deviceId)) {
			return false;
		}
		if (Double.doubleToLongBits(this.hcho) != Double.doubleToLongBits(other.hcho)) {
			return false;
		}
		if (Double.doubleToLongBits(this.hmdt) != Double.doubleToLongBits(other.hmdt)) {
			return false;
		}
		if (!Objects.equals(this.ip, other.ip)) {
			return false;
		}
		if (!Objects.equals(this.location, other.location)) {
			return false;
		}
		if (Double.doubleToLongBits(this.tmp) != Double.doubleToLongBits(other.tmp)) {
			return false;
		}
		if (!Objects.equals(this.type, other.type)) {
			return false;
		}
		if (Double.doubleToLongBits(this.usv) != Double.doubleToLongBits(other.usv)) {
			return false;
		}
		return true;
	}

	@Override
	public JsonObject toJson() {
		final JsonObject obj = super.toJson();
		if (this.location != null) {
			obj.put("location", new double[] { this.location.x, this.location.y });
		}
		return obj;
	}

	@Override
	@JsonIgnore
	public JsonObject toPublicJson() {
		final JsonObject obj = this.toJson();
		obj.remove("id");
		obj.remove("deviceId");
		obj.remove("userId");
		obj.remove("ip");
		for (final String field : new Vector<>(obj.fieldNames())) {
			if ("NaN".equals(String.valueOf(obj.getValue(field)))) {
				obj.remove(field);
			}
		}
		return obj;
	}

	public String toURadMonitorUrl() {
		final SortedMap<Integer, Object> map = new TreeMap<>();
		final StringBuilder sb = new StringBuilder();
		final JsonObject obj = this.toPublicJson();
		obj.remove("location");
		obj.remove("type");
		for (final String field : obj.fieldNames()) {
			map.put(URADMONITOR_FIELDS.indexOf(field) + 1, obj.getValue(field));
		}
		for (final Integer key : map.keySet()) {
			sb.append("/");
			sb.append(String.format("%1$02X", key));
			sb.append("/");
			if (key.equals(1)) { // date from ms to s
				sb.append((long) map.get(1) / 1000L);
			} else {
				sb.append(map.get(key));
			}
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return "Record [deviceId=" + this.deviceId + ", cpm=" + this.cpm + ", acpm=" + this.acpm + ", usv=" + this.usv
				+ ", co2=" + this.co2 + ", hcho=" + this.hcho + ", tmp=" + this.tmp + ", ap=" + this.ap + ", hmdt="
				+ this.hmdt + ", accy=" + this.accy + ", date=" + this.date + ", ip=" + this.ip + ", type=" + this.type
				+ ", location=" + this.location + "]";
	}

	public static class Builder { // XXX this will need a big clean up but at least it splits stuff
		private final Record record = new Record();

		public Builder() {
		}

		public Builder withDevice(final UUID id) {
			this.record.setDeviceId(id);
			return this;
		}

		public Builder withCurrentDate() {
			this.record.setDate(new Date());
			return this;
		}

		public Builder withIp(final String ip) {
			this.record.setIp(ip);
			return this;
		}

		public Builder withGmcParams(final MultiMap params) {
			// for (final Field f : this.record.getClass().getDeclaredFields()) {
			// if (f.getType().equals(double.class)) {
			// this.buildDoubleParameter(f.getName());
			// }
			// }

			for (final String s : Record.STAT_FIELDS) {
				this.buildDoubleParameter(params, s);
			}

			if (params.contains("type")) {
				this.record.setType(params.get("type"));
			}
			return this;
		}

		private void buildDoubleParameter(final MultiMap params, final String name) {
			final String str = params.get(name);
			if (str == null) {
				return;
			}
			final double value = Double.parseDouble(str);
			this.reflectSetField(name, value);
		}

		public Builder withGmcPosition(final MultiMap params) {
			final String rawLon = params.get("lon");
			final String rawLat = params.get("lat");
			// FIXME here I removed the altitude since Postgres doesn't support it. Might
			// wanna add it as a separate field.

			if (rawLat == null || rawLon == null) {
				return this;
			}

			final double lon = Double.parseDouble(rawLon);
			final double lat = Double.parseDouble(rawLat);

			final List<Double> values = new Vector<>();
			values.add(lon);
			values.add(lat);

			final Point point = new Point(lon, lat);
			this.record.setLocation(point);
			return this;
		}

		public Builder withURadMonitorUrl(final String url) {
			final String[] parts = url.split(Pattern.quote("/"));

			if ((parts.length - 1) % 2 == 1) {
				throw new IllegalArgumentException("URL has odd number of slash");
			}

			for (int i = 1; i < parts.length; i += 2) {
				final int field = Integer.parseInt(parts[i], 16) - 1;
				final String fieldName = Record.URADMONITOR_FIELDS.get(field);
				final String value = parts[i + 1];

				if (Record.STAT_FIELDS.contains(fieldName)) {
					this.reflectSetField(fieldName, Double.parseDouble(value));
				} else if (fieldName.equals("date")) {
					this.record.setDate(new Date(Long.parseLong(value) * 1000L)); // from s to ms
				} else {
					this.reflectSetField(fieldName, value);
				}
			}

			return this;
		}

		public Record build() {
			return this.record;
		}

		private void reflectSetField(final String name, final Object value) {
			try {
				final Field f = this.record.getClass().getDeclaredField(name);
				f.setAccessible(true);
				f.set(this.record, value);
			} catch (final IllegalArgumentException | IllegalAccessException | NoSuchFieldException
					| SecurityException e) {
				throw new IllegalStateException(e);
			}
		}
	}
}

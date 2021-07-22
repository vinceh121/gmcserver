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

import org.bson.types.ObjectId;

import io.vertx.core.json.JsonObject;

public class DeviceStats extends AbstractEntity {
	private String field;
	private ObjectId device;
	private double avg, min, max, stdDev;
	private int sampleSize;

	public String getField() {
		return this.field;
	}

	public void setField(final String field) {
		this.field = field;
	}

	public ObjectId getDevice() {
		return this.device;
	}

	public void setDevice(final ObjectId device) {
		this.device = device;
	}

	public double getAvg() {
		return this.avg;
	}

	public void setAvg(final double avg) {
		this.avg = avg;
	}

	public double getMin() {
		return this.min;
	}

	public void setMin(final double min) {
		this.min = min;
	}

	public double getMax() {
		return this.max;
	}

	public void setMax(final double max) {
		this.max = max;
	}

	public double getStdDev() {
		return this.stdDev;
	}

	public void setStdDev(final double stdDev) {
		this.stdDev = stdDev;
	}

	public int getSampleSize() {
		return this.sampleSize;
	}

	public void setSampleSize(final int sampleSize) {
		this.sampleSize = sampleSize;
	}

	@Override
	public JsonObject toJson() {
		final JsonObject obj = super.toJson();
		obj.remove("id");
		return obj;
	}
}

package me.vinceh121.gmcserver.entities;

import org.bson.types.ObjectId;

public class DeviceStats extends AbstractEntity {
	private String field;
	private ObjectId device;
	private double avg, min, max, stdDev;

	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}

	public ObjectId getDevice() {
		return device;
	}

	public void setDevice(ObjectId device) {
		this.device = device;
	}

	public double getAvg() {
		return avg;
	}

	public void setAvg(double avg) {
		this.avg = avg;
	}

	public double getMin() {
		return min;
	}

	public void setMin(double min) {
		this.min = min;
	}

	public double getMax() {
		return max;
	}

	public void setMax(double max) {
		this.max = max;
	}

	public double getStdDev() {
		return stdDev;
	}

	public void setStdDev(double stdDev) {
		this.stdDev = stdDev;
	}
}

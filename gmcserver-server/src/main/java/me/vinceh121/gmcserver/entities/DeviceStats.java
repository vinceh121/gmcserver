package me.vinceh121.gmcserver.entities;

import org.bson.types.ObjectId;

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
}

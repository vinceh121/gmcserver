package me.vinceh121.gmcserver.entities;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

public class DeviceCalendar extends AbstractEntity {
	private ObjectId deviceId;
	private Date lastCalculationDate = new Date(0L);
	private List<Document> recs = Collections.emptyList();
	private boolean inProgress;

	public ObjectId getDeviceId() {
		return this.deviceId;
	}

	public void setDeviceId(final ObjectId deviceId) {
		this.deviceId = deviceId;
	}

	public Date getLastCalculationDate() {
		return this.lastCalculationDate;
	}

	public void setLastCalculationDate(final Date lastCalculationDate) {
		this.lastCalculationDate = lastCalculationDate;
	}

	public List<Document> getRecs() {
		return this.recs;
	}

	public void setRecs(final List<Document> recs) {
		this.recs = recs;
	}

	public boolean isInProgress() {
		return this.inProgress;
	}

	public void setInProgress(final boolean inProgress) {
		this.inProgress = inProgress;
	}
}

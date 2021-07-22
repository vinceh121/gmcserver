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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.bson.Document;
import org.bson.types.ObjectId;

public class DeviceCalendar extends AbstractEntity {
	private ObjectId deviceId;
	private Date createdAt = new Date(0L);
	private List<Document> recs = Collections.emptyList();
	private boolean inProgress;

	public ObjectId getDeviceId() {
		return this.deviceId;
	}

	public void setDeviceId(final ObjectId deviceId) {
		this.deviceId = deviceId;
	}

	public Date getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(final Date createdAt) {
		this.createdAt = createdAt;
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

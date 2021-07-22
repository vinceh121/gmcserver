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
package me.vinceh121.gmcserver.managers.email;

import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.entities.User;

public class Email {
	private final JsonObject context = new JsonObject();
	private String template, subject;
	private User to;

	public JsonObject getContext() {
		return this.context;
	}

	public String getTemplate() {
		return this.template;
	}

	public void setTemplate(final String template) {
		this.template = template;
	}

	public User getTo() {
		return this.to;
	}

	public void setTo(final User to) {
		this.to = to;
		this.context.put("user", to.toPublicJson());
	}

	public String getSubject() {
		return this.subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

}

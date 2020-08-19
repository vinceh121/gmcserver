package me.vinceh121.gmcserver.managers.email;

import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.entities.User;

public class Email {
	private JsonObject context = new JsonObject();
	private String template, subject;
	private User to;

	public JsonObject getContext() {
		return this.context;
	}

	public void setContext(final JsonObject context) {
		this.context = context;
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

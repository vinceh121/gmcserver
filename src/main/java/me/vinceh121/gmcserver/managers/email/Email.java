package me.vinceh121.gmcserver.managers.email;

import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.entities.User;

public class Email {
	private JsonObject context = new JsonObject();
	private String template, subject;
	private User to;

	public JsonObject getContext() {
		return context;
	}

	public void setContext(JsonObject context) {
		this.context = context;
	}

	public String getTemplate() {
		return template;
	}

	public void setTemplate(String template) {
		this.template = template;
	}

	public User getTo() {
		return to;
	}

	public void setTo(User to) {
		this.to = to;
		this.context.put("user", to.toPublicJson());
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

}

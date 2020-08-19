package me.vinceh121.gmcserver;

import java.lang.reflect.Field;

public final class GMCBuild {
	public static final String CONFIG_PATH = "${gmc.config.path}", VERTX_CONFIG_PATH = "${gmc.vertx.config.path}",
			VERSION = "${project.version}", GIT_COMMIT = "${git.commit.id.abbrev}",
			GIT_COMMIT_DATE = "${git.commit.time}", VERTX_MAIL_CONFIG_PATH = "${gmc.vertx.mail.config.path}",
			MAIL_TEMPLATES_PATH = "${gmc.vertx.mail.templates.path}";

	public static String buildOptions() {
		final StringBuilder sb = new StringBuilder();
		for (final Field f : GMCBuild.class.getDeclaredFields()) {
			sb.append(f.getName());
			sb.append(" = ");
			try {
				sb.append(f.get(""));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
				sb.append(e.toString());
			}
			sb.append('\n');
		}
		return sb.toString();
	}
}

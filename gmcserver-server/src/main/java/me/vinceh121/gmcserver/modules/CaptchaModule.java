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
package me.vinceh121.gmcserver.modules;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.codec.BodyCodec;
import me.vinceh121.gmcserver.GMCServer;

public class CaptchaModule extends AbstractModule {
	private final String captchaUrl, inputType, level, media;

	public CaptchaModule(final GMCServer srv) {
		super(srv);
		this.captchaUrl = this.srv.getConfig().getProperty("captcha.url");
		this.inputType = this.srv.getConfig().getProperty("captcha.input-type");
		this.level = this.srv.getConfig().getProperty("captcha.level");
		this.media = this.srv.getConfig().getProperty("captcha.media");

		this.registerRoute(HttpMethod.GET, "/captcha", this::handleCaptchaGet);
	}

	private void handleCaptchaGet(final RoutingContext ctx) {
		if (ctx.request().params().contains("id")) {
			this.handleCaptchaMedia(ctx);
		} else {
			this.handleCaptchaRequest(ctx);
		}
	}

	private void handleCaptchaRequest(final RoutingContext ctx) {
		this.srv.getWebClient()
			.postAbs(this.captchaUrl + "/captcha")
			.as(BodyCodec.jsonObject())
			.sendJsonObject(new JsonObject().put("input_type", this.inputType)
				.put("level", this.level)
				.put("media", this.media))
			.onSuccess(res -> {
				final String captchaId = res.body().getString("id");
				ctx.response().end(new JsonObject().put("id", captchaId).toBuffer());
			})
			.onFailure(t -> {
				this.log.error("Error while generating captcha", t);
				this.error(ctx, 502, "Error while getting captcha: " + t);
			});
	}

	private void handleCaptchaMedia(final RoutingContext ctx) {
		final String captchaId = ctx.request().getParam("id");
		this.srv.getWebClient()
			.getAbs(this.captchaUrl + "/media")
			.as(BodyCodec.buffer())
			.addQueryParam("id", captchaId)
			.send()
			.onSuccess(mediaRes -> {
				ctx.response().putHeader("Content-Type", this.media).end(mediaRes.bodyAsBuffer());
			})
			.onFailure(t -> {
				this.log.error("Failed to retreive captcha image");
				this.error(ctx, 502, "Failed to retreive captcha image: " + t);
			});
	}

}

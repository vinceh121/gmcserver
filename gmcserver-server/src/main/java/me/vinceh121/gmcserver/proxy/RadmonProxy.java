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
package me.vinceh121.gmcserver.proxy;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;

public class RadmonProxy extends AbstractProxy {

	public RadmonProxy(final GMCServer srv) {
		super(srv);
	}

	@Override
	public Future<Void> validateSettings(final Device dev, final JsonObject obj) {
		return Future.future(p -> {
			if (obj.size() != 2) {
				p.fail("Invalid number of arguments");
				return;
			}

			if (!obj.containsKey("user") || !obj.containsKey("password")) {
				p.fail("The following fields are required: user, password");
				return;
			}

			if (!(obj.getValue("user") instanceof String) || !(obj.getValue("password") instanceof String)) {
				p.fail("Both user and password must be strings");
				return;
			}

			p.complete();
		});
	}

	@Override
	public Future<Void> proxyRecord(final Record r, final Device dev, final Map<String, Object> proxySettings) {
		// GET
		// https://radmon.org/radmon.php?function=submit&user=vinceh121&password=owo&value=26&unit=CPM
		return Future.future(p -> {
			this.srv.getWebClient()
				.get("radmon.org", "/radmon.php")
				.as(BodyCodec.string())
				.addQueryParam("function", "submit")
				.addQueryParam("user", String.valueOf(proxySettings.get("user")))
				.addQueryParam("password", String.valueOf(proxySettings.get("password")))
				.addQueryParam("value", Double.toString(r.getCpm()))
				.addQueryParam("unit", "CPM")
				.send()
				.onSuccess(res -> {
					final String msg = res.body();
					if (!msg.equals("OK<br>")) {
						p.fail(msg);
					}
				})
				.onFailure(p::fail);
		});
	}

}

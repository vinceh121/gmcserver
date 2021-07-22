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
import me.vinceh121.gmcserver.GMCServer;

public class InstanceModule extends AbstractModule {

	public InstanceModule(final GMCServer srv) {
		super(srv);
		this.registerRoute(HttpMethod.GET, "/instance/info", this::handleInstanceInfo);
	}

	private void handleInstanceInfo(final RoutingContext ctx) {
		ctx.response().end(JsonObject.mapFrom(this.srv.getInstanceInfo()).toBuffer());
	}

}

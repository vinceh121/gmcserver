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
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;

public abstract class AbstractProxy {
	protected final GMCServer srv;

	public AbstractProxy(final GMCServer srv) {
		this.srv = srv;
	}

	public abstract Future<Void> validateSettings(final Device dev, final JsonObject obj);

	public abstract Future<Void> proxyRecord(final Record r, final Device dev, final Map<String, Object> proxySettings);
}

/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.vinceh121.gmcserver.modules;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;

public class GeoModule extends AbstractModule {

	public GeoModule(final GMCServer srv) {
		super(srv);
		this.registerRoute(HttpMethod.GET, "/map/:boundingBox", this::handleMap);
	}

	private void handleMap(final RoutingContext ctx) {
		// lowerLeftX
		// lowerLeftY
		// upperRightX
		// upperRightY
		final String rawBB = ctx.pathParam("boundingBox");
		if (rawBB == null) {
			this.error(ctx, 400, "Missing BB");
			return;
		}

		final JsonArray bb;
		try {
			bb = new JsonArray(rawBB);
		} catch (final Exception e) {
			this.error(ctx, 400, "Invalid BB");
			return;
		}

		if (bb.size() != 4) {
			this.error(ctx, 400, "Invalid BB");
			return;
		}

		for (final Object obj : bb) {
			if (!(obj instanceof Number)) {
				this.error(ctx, 400, "Invalid BB");
				return;
			}
		}

		// final List<Position> ext = new Vector<>();
		//
		// for (final Object arr : bb) {
		// if (!(arr instanceof JsonArray)) {
		// this.error(ctx, 400, "Invalid BB");
		// return;
		// }
		// ext.add(new Position(((JsonArray) arr).getList()));
		// }
		//
		// final PolygonCoordinates coords = new PolygonCoordinates(ext);
		//
		// final Polygon poly = new Polygon(coords);

		final FindIterable<Device> it = this.srv.getDatabaseManager()
			.getCollection(Device.class)
			.find(Filters.geoWithinBox("location", bb.getDouble(0), bb.getDouble(1), bb.getDouble(2), bb.getDouble(3)));

		final JsonArray res = new JsonArray();
		it.forEach(d -> {
			final JsonObject mapJson = d.toMapJson();
			res.add(mapJson);

			final Record lastRec = this.srv.getDatabaseManager()
				.getCollection(Record.class)
				.find(Filters.eq("deviceId", d.getId()))
				.sort(Sorts.descending("date"))
				.first();
			if (lastRec != null && !Double.isNaN(lastRec.getCpm())) {
				mapJson.put("cpm", lastRec.getCpm());
			}
		});
		ctx.response().end(res.toBuffer());
	}
}

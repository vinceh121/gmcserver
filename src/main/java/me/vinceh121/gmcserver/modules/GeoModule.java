package me.vinceh121.gmcserver.modules;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.DatabaseManager;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;

public class GeoModule extends AbstractModule {

	public GeoModule(final GMCServer srv) {
		super(srv);
		this.registerAuthedRoute(HttpMethod.GET, "/map/:boundingBox", this::handleMap);
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

		final FindIterable<Device> it = this.srv.getManager(DatabaseManager.class)
				.getCollection(Device.class)
				.find(Filters.geoWithinBox("location", bb.getDouble(1), bb.getDouble(0), bb.getDouble(3),
						bb.getDouble(2)));

		final JsonArray res = new JsonArray();
		it.forEach(d -> {
			final JsonObject mapJson = d.toMapJson();
			res.add(mapJson);

			final Record lastRec = this.srv.getManager(DatabaseManager.class)
					.getCollection(Record.class)
					.find(Filters.eq("deviceId", d.getId()))
					.sort(Sorts.descending("date"))
					.first();
			if (lastRec != null && !Double.isNaN(lastRec.getAcpm())) {
				mapJson.put("cpm", lastRec.getAcpm());
			}
		});
		ctx.response().end(res.toBuffer());
	}
}

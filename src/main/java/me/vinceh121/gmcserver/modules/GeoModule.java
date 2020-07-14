package me.vinceh121.gmcserver.modules;

import java.util.List;
import java.util.Vector;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;

import me.vinceh121.gmcserver.entities.Device;

public class GeoModule extends AbstractModule {

	public GeoModule(final GMCServer srv) {
		super(srv);
		this.registerAuthedRoute(HttpMethod.GET, "/map/:boundingBox", this::handleMap);
	}

	@SuppressWarnings("unchecked")
	private void handleMap(final RoutingContext ctx) {
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

		if (bb.size() != 5) {
			this.error(ctx, 400, "Invalid BB");
			return;
		}

		final List<Position> ext = new Vector<>();

		for (final Object arr : bb) {
			if (!(arr instanceof JsonArray)) {
				this.error(ctx, 400, "Invalid BB");
				return;
			}
			ext.add(new Position(((JsonArray) arr).getList()));
		}

		final PolygonCoordinates coords = new PolygonCoordinates(ext);

		final Polygon poly = new Polygon(coords);

		final FindIterable<Device> it
				= this.srv.getDatabaseManager().getCollection(Device.class).find(Filters.geoWithin("location", poly));

		final JsonArray res = new JsonArray();
		it.forEach(d -> {
			res.add(d.toMapJson());
		});
		ctx.response().end(res.toBuffer());
	}

}

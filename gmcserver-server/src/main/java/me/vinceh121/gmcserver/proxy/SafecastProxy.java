package me.vinceh121.gmcserver.proxy;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.codec.BodyCodec;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;

public class SafecastProxy extends AbstractProxy {
	private static final DateFormat DATE_FORMAT_ISO_8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

	public SafecastProxy(final GMCServer srv) {
		super(srv);
	}

	@Override
	public Future<Void> validateSettings(final Device dev, final JsonObject obj) {
		return Future.future(p -> {
			if (obj.size() != 2) {
				p.fail("Invalid number of arguments");
				return;
			}

			if (!obj.containsKey("deviceId") || !obj.containsKey("apiKey")) {
				p.fail("The following fields are required: deviceId, apiKey");
				return;
			}

			if (!(obj.getValue("deviceId") instanceof Number) || !(obj.getValue("apiKey") instanceof String)) {
				p.fail("deviceId must be a number and apiKey a String");
				return;
			}

			if (dev.getLocation() == null) {
				p.fail("Safecast requires devices to have a location set");
				return;
			}

			p.complete();
		});
	}

	@Override
	public Future<Void> proxyRecord(final Record r, final Device dev, final Map<String, Object> proxySettings) {
		return Future.future(p -> {
			final double latitude, longitude;

			if (r.getLocation() != null) { // prioritize location from individual record, else use device's location
				longitude = r.getLocation().getPosition().getValues().get(0);
				latitude = r.getLocation().getPosition().getValues().get(1);
			} else if (dev.getLocation() != null) {
				longitude = dev.getLocation().getPosition().getValues().get(0);
				latitude = dev.getLocation().getPosition().getValues().get(1);
			} else {
				p.fail("Nor record or device have position set");
				return;
			}

			this.srv.getWebClient()
				.post("api.safecast.org", "/measurements.json")
				.as(BodyCodec.jsonObject())
				.addQueryParam("api_key", String.valueOf(proxySettings.get("apiKey")))
				.sendJsonObject(
						new JsonObject().put("captured_at", SafecastProxy.DATE_FORMAT_ISO_8601.format(r.getDate()))
							.put("device_id", proxySettings.get("deviceId"))
							.put("value", r.getCpm())
							.put("unit", "cpm")
							.put("longitude", longitude)
							.put("latitude", latitude))
				.onSuccess(res -> {
					if (res.statusCode() != 201) {
						p.fail("Safecast returned non-201: " + res.statusCode() + ": " + res.body().toString());
					} else {
						p.complete();
					}
				})
				.onFailure(p::fail);
		});
	}

}

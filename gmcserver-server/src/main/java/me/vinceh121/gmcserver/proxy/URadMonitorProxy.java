package me.vinceh121.gmcserver.proxy;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.codec.BodyCodec;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;

public class URadMonitorProxy extends AbstractProxy {

	public URadMonitorProxy(final GMCServer srv) {
		super(srv);
	}

	@Override
	public Future<Void> validateSettings(final Device dev, final JsonObject obj) {
		return Future.future(p -> {
			if (obj.size() != 3) {
				p.fail("Invalid number of arguments");
				return;
			}

			if (!obj.containsKey("userId") || !obj.containsKey("userHash") || !obj.containsKey("deviceId")) {
				p.fail("The following fields are required: userId, userHash, deviceId");
				return;
			}

			if (!(obj.getValue("userId") instanceof Number) || !(obj.getValue("deviceId") instanceof Number)) {
				p.fail("Both userId and deviceId must be integers");
				return;
			}

			if (!(obj.getValue("userHash") instanceof String)) {
				p.fail("userHash must be a string");
				return;
			}

			p.complete();
		});
	}

	@Override
	public Future<Void> proxyRecord(final Record r, final Device dev, final Map<String, Object> proxySettings) {
		return Future.future(p -> {
			final HttpRequest<JsonObject> req = this.srv.getWebClient()
				.post("data.uradmonitor.com", "/api/v1/upload/exp" + r.toURadMonitorUrl())
				.as(BodyCodec.jsonObject());
			req.send().onSuccess(res -> {
				if (res.statusCode() != 200) {
					p.fail(res.statusCode() + ": " + res.body());
				} else {
					p.complete();
				}
			}).onFailure(p::fail);
		});
	}

}

package me.vinceh121.gmcserver.proxy;

import java.util.Map;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.codec.BodyCodec;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Record;

public class GmcmapProxy extends AbstractProxy {
	public GmcmapProxy(final GMCServer srv) {
		super(srv);
	}

	@Override
	public Future<Void> proxyRecord(final Record r, final Map<String, Object> proxySettings) {
		return Future.future(p -> {
			final HttpRequest<String> req = this.srv.getWebClient()
				.post("gmcmap.com", "/log2.asp")
				.as(BodyCodec.string());
			req.setQueryParam("AID", String.valueOf(proxySettings.get("userId")))
				.setQueryParam("GID", String.valueOf(proxySettings.get("deviceId")));

			final JsonObject rObj = r.toPublicJson();
			for (final String field : Record.STAT_FIELDS) {
				if (rObj.containsKey(field)) {
					req.setQueryParam(field, Double.toString(rObj.getDouble(field)));
				}
			}

			if (r.getLocation() != null) {
				req.setQueryParam("lon", Double.toString(r.getLocation().getPosition().getValues().get(0)));
				req.setQueryParam("lat", Double.toString(r.getLocation().getPosition().getValues().get(1)));
				if (r.getLocation().getPosition().getValues().size() > 2) {
					req.setQueryParam("alt", Double.toString(r.getLocation().getPosition().getValues().get(2)));
				}
			}

			req.send().onSuccess(res -> p.complete()).onFailure(p::fail);
		});
	}

	@Override
	public Future<Void> validateSettings(final JsonObject obj) {
		// TODO Auto-generated method stub
		return null;
	}

}

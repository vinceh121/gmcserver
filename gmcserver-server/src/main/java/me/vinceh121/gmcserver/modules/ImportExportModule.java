package me.vinceh121.gmcserver.modules;

import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Vector;

import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.mongodb.client.model.Filters;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import me.vinceh121.gmcserver.DatabaseManager;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;

public class ImportExportModule extends AbstractModule {
	public static final DateFormat GMCMAP_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	public static final String GMCMAP_HISTORY_URI = "/historyData.asp", GMCMAP_HOST = "www.gmcmap.com";
	private static final SecureRandom DEV_RANDOM = new SecureRandom();

	public ImportExportModule(final GMCServer srv) {
		super(srv);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/import/gmcmap", this::importGmcMap);
	}

	private void importGmcMap(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();
		if (obj == null) {
			this.error(ctx, 400, "Invalid JSON");
			return;
		}
		if (!obj.containsKey("gmcmapId")) {
			this.error(ctx, 400, "Missing 'gmcmapId' parameter");
			return;
		}

		final String gmcmapId = obj.getString("gmcmapId");

		final User user = ctx.get(AuthHandler.USER_KEY);

		final int deviceLimit;
		if (user.getDeviceLimit() != -1) {
			deviceLimit = user.getDeviceLimit();
		} else {
			deviceLimit = Integer.parseInt(this.srv.getConfig().getProperty("device.user-limit"));
		}

		if (deviceLimit <= this.srv.getManager(DatabaseManager.class)
				.getCollection(Device.class)
				.countDocuments(Filters.eq("ownerId", user.getId()))) {
			this.error(ctx, 403, "Device limit reached");
			return;
		}

		final Device dev = new Device();
		dev.setDisabled(true);
		dev.setImportedFrom(gmcmapId);
		dev.setOwner(user.getId());
		dev.setName("Imported from gmcmap ID " + gmcmapId);
		dev.setGmcId(ImportExportModule.DEV_RANDOM.nextLong());

		this.srv.getManager(DatabaseManager.class).getCollection(Device.class).insertOne(dev);

		this.getRecords(gmcmapId, 0, dev.getId()).onComplete(res -> {
			if (res.failed()) {
				this.error(ctx, 502, "Failure: " + res.cause().getMessage());
				return;
			}

			if (res.result().size() == 0) {
				this.error(ctx, 502, "Returned data table is empty");
				return;
			}

			ctx.response().end();

			final List<Record> recs = res.result();
			this.srv.getManager(DatabaseManager.class).getCollection(Record.class).insertMany(recs);

			this.importPageRecurse(gmcmapId, 1, dev.getId());
		});
	}

	private void importPageRecurse(final String gmcmapId, final int page, final ObjectId deviceId) {
		this.getRecords(gmcmapId, page, deviceId).onComplete(ares -> {
			if (ares.failed()) {
				this.log.error("Error while importing device {} at page {}", gmcmapId, page);
				return;
			}

			this.log.info("Got {} records from device import {}, page {}", ares.result().size(), gmcmapId, page);

			if (ares.result().size() != 0) {
				this.srv.getManager(DatabaseManager.class).getCollection(Record.class).insertMany(ares.result());
				this.importPageRecurse(gmcmapId, page + 1, deviceId);
			} else {
				this.log.info("Finished import for {}", gmcmapId);
			}
		});
	}

	private Future<List<Record>> getRecords(final String gmcmapId, final int page, final ObjectId deviceId) {
		return Future.future(p -> {
			this.srv.getWebClient()
					.get(ImportExportModule.GMCMAP_HOST, ImportExportModule.GMCMAP_HISTORY_URI)
					.setQueryParam("param_ID", gmcmapId)
					.setQueryParam("curpage", String.valueOf(page))
					.send(a -> {
						if (a.failed()) {
							p.fail("Http request failed: " + a.cause().getMessage());
							return;
						}

						final HttpResponse<Buffer> res = a.result();
						final Document doc = Jsoup.parse(res.bodyAsString());
						final Element table = doc.getElementsByTag("table").first();

						if (table == null) {
							p.fail("No data table found");
							return;
						}

						final List<Record> records = new Vector<>();

						for (final Element c : table.children()) {
							if (!c.tagName().equals("tbody")) {
								continue;
							}

							final Element tr = c.getElementsByTag("tr").first();

							final Element elmDate = tr.child(0);
							final Element elmCpm = tr.child(1);
							final Element elmAcpm = tr.child(2);
							final Element elmUsv = tr.child(3);
							// final Element elmLat = tr.child(4);
							// final Element elmLon = tr.child(5);

							final Record r = new Record();
							r.setDeviceId(deviceId);
							try {
								r.setDate(ImportExportModule.GMCMAP_DATE_FMT.parse(elmDate.text()));
							} catch (final ParseException e) {
								this.log.error("Error while parsing date for record during import", e);
								return;
							}
							r.setCpm(Double.parseDouble(elmCpm.text()));
							r.setAcpm(Double.parseDouble(elmAcpm.text()));
							r.setUsv(Double.parseDouble(elmUsv.text()));
							records.add(r);
						}
						p.complete(records);
					});
		});
	}

}

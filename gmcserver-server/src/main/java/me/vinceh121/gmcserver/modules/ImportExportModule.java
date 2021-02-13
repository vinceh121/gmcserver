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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Filters;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;

public class ImportExportModule extends AbstractModule {
	private static final Logger LOG = LoggerFactory.getLogger(ImportExportModule.class);
	public static final DateFormat GMCMAP_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	public static final String GMCMAP_HISTORY_URI = "/historyData.asp", GMCMAP_HOST = "www.gmcmap.com";
	private static final SecureRandom DEV_RANDOM = new SecureRandom();

	public ImportExportModule(final GMCServer srv) {
		super(srv);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/import/gmcmap", this::handleImportGmcMap);
		this.registerAuthedRoute(HttpMethod.GET, "/device/:deviceId/export/csv", this::handleCsvExport);
	}

	private void handleImportGmcMap(final RoutingContext ctx) {
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

		if (deviceLimit <= this.srv.getDatabaseManager()
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

		this.srv.getDatabaseManager().getCollection(Device.class).insertOne(dev);

		this.getRecords(gmcmapId, 0, dev.getId()).onSuccess(recs -> {
			if (recs.size() == 0) {
				this.error(ctx, 502, "Returned data table is empty");
				return;
			}

			ctx.response().end();

			this.srv.getDatabaseManager().getCollection(Record.class).insertMany(recs);

			this.importPageRecurse(gmcmapId, 1, dev.getId());
		}).onFailure(t -> {
			this.error(ctx, 502, "Failure: " + t.getMessage());
		});
	}

	private void importPageRecurse(final String gmcmapId, final int page, final ObjectId deviceId) {
		this.getRecords(gmcmapId, page, deviceId).onSuccess(recs -> {
			this.log.info("Got {} records from device import {}, page {}", recs.size(), gmcmapId, page);

			if (recs.size() != 0) {
				this.srv.getDatabaseManager().getCollection(Record.class).insertMany(recs);
				this.importPageRecurse(gmcmapId, page + 1, deviceId);
			} else {
				this.log.info("Finished import for {}", gmcmapId);
			}
		}).onFailure(t -> {
			this.log.error("Error while importing device {} at page {}", gmcmapId, page);
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

	private void handleCsvExport(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId");
		final ObjectId deviceId;
		try {
			deviceId = new ObjectId(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		ctx.response().setChunked(true);

		final User user = ctx.get(AuthHandler.USER_KEY);

		this.srv.getDeviceManager().getDevice().setId(deviceId).execute().onSuccess(device -> {
			this.srv.getDeviceManager().deviceFullTimeline().setFull(true).setDev(device).execute().onSuccess(recs -> {
				if (user == null) {
					ctx.response().write("CPM,ACPM,USV,DATE,TYPE,LAT,LON\n");
					for (final Record r : recs) {
						ctx.response()
								.write(String.format("%f,%f,%f,%s,%s,%s,%s\n", r.getCpm(), r.getAcpm(), r.getUsv(),
										r.getDate().getTime(), r.getType(),
										r.getLocation() != null ? r.getLocation().getPosition().getValues().get(0) : "",
										r.getLocation() != null ? r.getLocation().getPosition().getValues().get(1)
												: ""));
					}
				} else {
					ctx.response().write("ID,DEVICEID,CPM,ACPM,USV,CO2,HCHO,TMP,AP,HMDT,ACCY,DATE,IP,TYPE,LAT,LON\n");
					for (final Record r : recs) {
						ctx.response()
								.write(String.format("%s,%s,%f,%f,%f,%f,%f,%f,%f,%f,%f,%s,%s,%s,%s,%s\n", r.getId(),
										r.getDeviceId(), r.getCpm(), r.getAcpm(), r.getUsv(), r.getCo2(), r.getHcho(),
										r.getTmp(), r.getAp(), r.getHmdt(), r.getAccy(), r.getDate().getTime(),
										r.getIp(), r.getType(),
										r.getLocation() != null ? r.getLocation().getPosition().getValues().get(0)
												: null,
										r.getLocation() != null ? r.getLocation().getPosition().getValues().get(1)
												: null));
					}
				}
				ctx.response().end();
			}).onFailure(t -> {
				LOG.error("Failed to get device " + deviceId + " timeline for export", t);
				this.error(ctx, 500, "Failed to get device timeline for export");
			});
		}).onFailure(t -> { // TODO differentiate DB error and not found
			this.error(ctx, 404, "Device not found");
		});
	}

}

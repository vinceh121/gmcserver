package me.vinceh121.gmcserver.modules;

import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;
import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

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
	private static final Logger LOG = LogManager.getLogger(ImportExportModule.class);
	public static final DateFormat GMCMAP_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	static {
		GMCMAP_DATE_FMT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
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
			this.log.error(new FormattedMessage("Error while importing device {} at page {}", gmcmapId, page), t);
		});
	}

	private Future<List<Record>> getRecords(final String gmcmapId, final int page, final ObjectId deviceId) {
		return Future.future(p -> {
			this.srv.getWebClient()
				.get(ImportExportModule.GMCMAP_HOST, ImportExportModule.GMCMAP_HISTORY_URI)
				.setQueryParam("param_ID", gmcmapId)
				.setQueryParam("curpage", String.valueOf(page))
				.setQueryParam("systemTimeZone", "0")
				.send(a -> {
					if (a.failed()) {
						p.fail("Http request failed: " + a.cause().getMessage());
						return;
					}
					try {

						final HttpResponse<Buffer> res = a.result();
						final Document doc = Jsoup.parse(res.bodyAsString());
						final Element table = doc.getElementsByTag("table").first();

						if (table == null) {
							p.fail("No data table found");
							return;
						}

						final Map<String, Integer> colIndexes = new Hashtable<>();
						final Element thead = table.children().select("thead").first().selectFirst("tr");
						for (int i = 0; i < thead.childrenSize(); i++) {
							final Element c = thead.child(i);
							if (!c.tagName().equals("th")) {
								continue;
							}
							if (c.text().startsWith("Date (")) {
								c.text("Date");
							}

							colIndexes.put(c.text(), i);
						}

						final List<Record> records = new Vector<>();

						for (final Element c : table.children()) {
							if (!c.tagName().equals("tbody")) {
								continue;
							}

							final Element tr = c.getElementsByTag("tr").first();

							// Date, CPM, ACPM and uSv are guaranteed to be present
							final Element elmDate = tr.child(colIndexes.get("Date"));
							final Element elmCpm = tr.child(colIndexes.get("CPM"));
							final Element elmAcpm = tr.child(colIndexes.get("ACPM"));
							final Element elmUsv = tr.child(colIndexes.get("uSv/h"));

							Element elmLon = null;
							if (colIndexes.containsKey("Longitude")) {
								elmLon = tr.child(colIndexes.get("Longitude"));
							}

							Element elmLat = null;
							if (colIndexes.containsKey("Latitude")) {
								elmLat = tr.child(colIndexes.get("Latitude"));
							}

							Element elmAlt = null;
							if (colIndexes.containsKey("Altitude")) {
								elmAlt = tr.child(colIndexes.get("Altitude"));
							}

							Element elmCO2 = null;
							if (colIndexes.containsKey("CO2")) {
								elmCO2 = tr.child(colIndexes.get("CO2"));
							}

							Element elmHCHO = null;
							if (colIndexes.containsKey("HCHO")) {
								elmHCHO = tr.child(colIndexes.get("HCHO"));
							}

							Element elmTemperature = null;
							if (colIndexes.containsKey("Temperature")) {
								elmTemperature = tr.child(colIndexes.get("Temperature"));
							}

							Element elmHumidity = null;
							if (colIndexes.containsKey("Humidity")) {
								elmHumidity = tr.child(colIndexes.get("Humidity"));
							}

							final Record r = new Record();
							r.setDeviceId(deviceId);

							try {
								r.setDate(ImportExportModule.GMCMAP_DATE_FMT.parse(elmDate.text()));
							} catch (final ParseException e) {
								this.log.error("Error while parsing date for record during import", e);
								return;
							}

							if (!elmCpm.text().isEmpty()) {
								r.setCpm(Double.parseDouble(elmCpm.text()));
							}
							if (!elmAcpm.text().isEmpty()) { // for some reason this wasn't a problem before
								r.setAcpm(Double.parseDouble(elmAcpm.text()));
							}
							if (!elmUsv.text().isEmpty()) {
								r.setUsv(Double.parseDouble(elmUsv.text()));
							}

							final List<Double> pos = new ArrayList<>(3);

							if (elmLon != null) {
								pos.set(0, Double.parseDouble(elmLon.text()));
							}

							if (elmLat != null) {
								pos.set(1, Double.parseDouble(elmLat.text()));
							}

							if (elmAlt != null) {
								pos.set(2, Double.parseDouble(elmAlt.text()));
							}

							if (pos.size() > 0) {
								r.setLocation(new Point(new Position(pos)));
							}

							if (elmCO2 != null) {
								r.setCo2(Double.parseDouble(elmCO2.text()));
							}

							if (elmHCHO != null) {
								r.setHcho(Double.parseDouble(elmHCHO.text()));
							}

							if (elmTemperature != null) {
								r.setTmp(Double.parseDouble(elmTemperature.text()));
							}

							if (elmHumidity != null) {
								r.setHmdt(Double.parseDouble(elmHumidity.text()));
							}

							records.add(r);
						}
						p.complete(records);
					} catch (final NumberFormatException e) {
						p.fail(new RuntimeException("Failed to import device", e));
					} catch (Exception e) {
						log.error(
								new FormattedMessage("ohno oopsie fucky wucky with import {} at page {}",
										gmcmapId,
										page),
								e);
					}
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

		ctx.response()
			.putHeader("Content-Type", "text/csv")
			.putHeader("Content-Disposition", "attachment; filename=\"gmcserver-" + deviceId.toHexString() + ".csv\"");

		ctx.response().setChunked(true);

		final User user = ctx.get(AuthHandler.USER_KEY);

		this.srv.getDeviceManager().getDevice().setId(deviceId).execute().onSuccess(device -> {
			this.srv.getDeviceManager().deviceFullTimeline().setFull(true).setDev(device).execute().onSuccess(recs -> {
				if (user == null) {
					ctx.response().write("CPM,ACPM,USV,DATE,TYPE,LON,LAT\n");
					for (final Record r : recs) {
						ctx.response()
							.write(String.format("%f,%f,%f,%s,%s,%s,%s\n",
									r.getCpm(),
									r.getAcpm(),
									r.getUsv(),
									r.getDate().getTime(),
									r.getType(),
									r.getLocation() != null ? r.getLocation().getPosition().getValues().get(0) : "",
									r.getLocation() != null ? r.getLocation().getPosition().getValues().get(1) : ""));
					}
				} else {
					ctx.response().write("ID,DEVICEID,CPM,ACPM,USV,CO2,HCHO,TMP,AP,HMDT,ACCY,DATE,IP,TYPE,LON,LAT\n");
					for (final Record r : recs) {
						ctx.response()
							.write(String.format("%s,%s,%f,%f,%f,%f,%f,%f,%f,%f,%f,%s,%s,%s,%s,%s\n",
									r.getId(),
									r.getDeviceId(),
									r.getCpm(),
									r.getAcpm(),
									r.getUsv(),
									r.getCo2(),
									r.getHcho(),
									r.getTmp(),
									r.getAp(),
									r.getHmdt(),
									r.getAccy(),
									r.getDate().getTime(),
									r.getIp(),
									r.getType(),
									r.getLocation() != null ? r.getLocation().getPosition().getValues().get(0) : null,
									r.getLocation() != null ? r.getLocation().getPosition().getValues().get(1) : null));
					}
				}
				ctx.response().end();
			}).onFailure(t -> {
				ImportExportModule.LOG
					.error(new FormattedMessage("Failed to get device {} timeline for export", deviceId), t);
				this.error(ctx, 500, "Failed to get device timeline for export");
			});
		}).onFailure(t -> { // TODO differentiate DB error and not found
			this.error(ctx, 404, "Device not found");
		});
	}

}

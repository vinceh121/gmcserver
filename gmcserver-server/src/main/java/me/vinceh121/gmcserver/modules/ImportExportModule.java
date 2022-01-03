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
package me.vinceh121.gmcserver.modules;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;
import org.bson.types.ObjectId;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.exceptions.EntityNotFoundException;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.managers.ImportManager;

public class ImportExportModule extends AbstractModule {
	private static final Logger LOG = LogManager.getLogger(ImportExportModule.class);
	public static final DateFormat GMCMAP_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	static {
		ImportExportModule.GMCMAP_DATE_FMT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	public static final String GMCMAP_HISTORY_URI = "/historyData.asp", GMCMAP_HOST = "www.gmcmap.com";

	public ImportExportModule(final GMCServer srv) {
		super(srv);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/import/gmcmap", this::handleImportGmcMap);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/import/safecast", this::handleImportSafecast);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/import/uradmonitor", this::handleImportURadMonitor);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/import/radmon", this::handleImportRadmon);
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

		this.srv.getDeviceManager()
			.createDevice()
			.setUser(user)
			.setName("Imported from gmcmap ID " + gmcmapId)
			.setImportedFrom(gmcmapId)
			.setDisabled(false)
			.execute()
			.onSuccess(dev -> {
				this.srv.getImportManager()
					.importGmcmap()
					.setDeviceId(dev.getId())
					.setGmcmapId(gmcmapId)
					.execute()
					.onSuccess(v -> this.responseImportStarted(ctx, dev.getId()))
					.onFailure(t -> this.error(ctx, 500, "Failed to start import: " + t));
			})
			.onFailure(t -> this.error(ctx, 500, "Failed to create device"));
	}

	private void handleImportSafecast(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();
		if (obj == null) {
			this.error(ctx, 400, "Invalid JSON");
			return;
		}

		if (!obj.containsKey("safecastId")) {
			this.error(ctx, 400, "Missing 'safecastId' parameter");
			return;
		}

		final String safecastId = obj.getString("safecastId");

		final User user = ctx.get(AuthHandler.USER_KEY);

		this.srv.getDeviceManager()
			.createDevice()
			.setUser(user)
			.setName("Imported from Safecast:" + safecastId)
			.setImportedFrom(safecastId + "@safecast")
			.execute()
			.onSuccess(dev -> {
				this.srv.getImportManager()
					.importSafecastMetadata()
					.setDeviceId(dev.getId())
					.setSafeCastId(safecastId)
					.execute()
					.onSuccess(v -> {
						this.srv.getImportManager()
							.importSafecastTimeline()
							.setDeviceId(dev.getId())
							.setSafeCastId(safecastId)
							.execute()
							.onSuccess(v2 -> this.responseImportStarted(ctx, dev.getId()))
							.onFailure(t -> this.error(ctx, 500, "Failed to start import: " + t));
					})
					.onFailure(t -> this.error(ctx, 500, "Failed to import metadata: " + t));
			})
			.onFailure(t -> {
				this.error(ctx, 500, "Failed to create device");
			});
	}

	private void handleImportURadMonitor(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();
		if (obj == null) {
			this.error(ctx, 400, "Invalid JSON");
			return;
		}

		if (!obj.containsKey("uradmonitorId")) {
			this.error(ctx, 400, "Missing 'uradmonitorId' parameter");
			return;
		}

		final String uradmonitorId = obj.getString("uradmonitorId");
		if (!ImportManager.PATTERN_URADMONITOR_ID.matcher(uradmonitorId).matches()) {
			this.error(ctx, 400, "Invalid uradmonitorId");
			return;
		}

		final User user = ctx.get(AuthHandler.USER_KEY);

		this.srv.getDeviceManager()
			.createDevice()
			.setUser(user)
			.setName("Imported from uRadMonitor:" + uradmonitorId)
			.setImportedFrom(uradmonitorId + "@uradmonitor")
			.execute()
			.onSuccess(dev -> {
				this.srv.getImportManager()
					.importURadMonitorMetadata()
					.setDeviceId(dev.getId())
					.setURadMonitorId(uradmonitorId)
					.execute()
					.onSuccess(v -> {
						this.srv.getImportManager()
							.importURadMonitor()
							.setDeviceId(dev.getId())
							.setuRadMonitorId(uradmonitorId)
							.execute()
							.onSuccess(v1 -> this.responseImportStarted(ctx, dev.getId()))
							.onFailure(t -> this.error(ctx, 500, "Failed to start import: " + t));
					});
			})
			.onFailure(t -> {
				this.error(ctx, 500, "Failed to create device");
			});
	}

	private void handleImportRadmon(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();
		if (obj == null) {
			this.error(ctx, 400, "Invalid JSON");
			return;
		}

		if (!obj.containsKey("radmonUsername")) {
			this.error(ctx, 400, "Missing 'radmonUsername' parameter");
			return;
		}

		final String radmonUsername = obj.getString("radmonUsername");
		if (!ImportManager.PATTERN_RADMON_USERNAME.matcher(radmonUsername).matches()) {
			this.error(ctx, 400, "Invalid radmonUsername");
			return;
		}

		final User user = ctx.get(AuthHandler.USER_KEY);

		this.srv.getDeviceManager()
			.createDevice()
			.setUser(user)
			.setName("Imported from Radmon: " + radmonUsername)
			.setImportedFrom(radmonUsername + "@radmon")
			.execute()
			.onSuccess(dev -> {
				this.srv.getImportManager()
					.importRadmon()
					.setDeviceId(dev.getId())
					.setUsername(radmonUsername)
					.execute()
					.onSuccess(v -> this.responseImportStarted(ctx, dev.getId()))
					.onFailure(t -> this.error(ctx, 500, "Failed to start import: " + t));
			})
			.onFailure(t -> {
				this.error(ctx, 500, "Failed to create device");
			});
	}

	private void responseImportStarted(RoutingContext ctx, ObjectId device) {
		ctx.end(new JsonObject().put("deviceId", device).toBuffer());
	}

	private void handleCsvExport(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId");
		final ObjectId deviceId;
		final Date start, end;

		try {
			deviceId = new ObjectId(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		if (ctx.request().params().contains("start")) {
			try {
				start = new Date(Long.parseLong(ctx.request().getParam("start")));
			} catch (final NumberFormatException e) {
				this.error(ctx, 400, "Format error in start date");
				return;
			}
		} else {
			start = null;
		}

		if (ctx.request().params().contains("end")) {
			try {
				end = new Date(Long.parseLong(ctx.request().getParam("end")));
			} catch (final NumberFormatException e) {
				this.error(ctx, 400, "Format error in end date");
				return;
			}
		} else {
			end = null;
		}

		ctx.response()
			.putHeader("Content-Type", "text/csv")
			.putHeader("Content-Disposition", "attachment; filename=\"gmcserver-" + deviceId.toHexString() + ".csv\"");

		ctx.response().setChunked(true);

		final User user = ctx.get(AuthHandler.USER_KEY);

		this.srv.getDeviceManager().getDevice().setId(deviceId).execute().onSuccess(device -> {
			this.srv.getDeviceManager()
				.deviceFullTimeline()
				.setFull(true)
				.setStart(start)
				.setEnd(end)
				.setDev(device)
				.execute()
				.onSuccess(recs -> {
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
										r.getLocation() != null ? r.getLocation().getPosition().getValues().get(1)
												: ""));
						}
					} else {
						ctx.response()
							.write("ID,DEVICEID,CPM,ACPM,USV,CO2,HCHO,TMP,AP,HMDT,ACCY,DATE,IP,TYPE,LON,LAT\n");
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
										r.getLocation() != null ? r.getLocation().getPosition().getValues().get(0)
												: null,
										r.getLocation() != null ? r.getLocation().getPosition().getValues().get(1)
												: null));
						}
					}
					ctx.response().end();
				})
				.onFailure(t -> {
					ImportExportModule.LOG
						.error(new FormattedMessage("Failed to get device {} timeline for export", deviceId), t);
					this.error(ctx, 500, "Failed to get device timeline for export");
				});
		}).onFailure(t -> {
			if (t instanceof EntityNotFoundException) {
				this.error(ctx, 404, "Device not found: " + t);
			} else {
				this.error(ctx, 500, "Failed to fetch device: " + t);
			}
		});
	}

}

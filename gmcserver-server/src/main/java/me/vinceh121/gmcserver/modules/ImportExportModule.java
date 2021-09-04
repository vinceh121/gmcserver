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

import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;
import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;

public class ImportExportModule extends AbstractModule {
	private static final Logger LOG = LogManager.getLogger(ImportExportModule.class);
	public static final DateFormat GMCMAP_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	static {
		ImportExportModule.GMCMAP_DATE_FMT.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
	public static final String GMCMAP_HISTORY_URI = "/historyData.asp", GMCMAP_HOST = "www.gmcmap.com";
	private static final SecureRandom DEV_RANDOM = new SecureRandom();

	public ImportExportModule(final GMCServer srv) {
		super(srv);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/import/gmcmap", this::handleImportGmcMap);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/import/safecast", this::handleImportSafecast);
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

		this.srv.getImportManager()
			.importGmcmap()
			.setDeviceId(dev.getId())
			.setGmcmapId(gmcmapId)
			.execute()
			.onSuccess(v -> this.error(ctx, 200, "Import started"))
			.onFailure(t -> this.error(ctx, 500, "Failed to start import: " + t));
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
					.importSafecast()
					.setDeviceId(dev.getId())
					.setSafeCastId(safecastId)
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

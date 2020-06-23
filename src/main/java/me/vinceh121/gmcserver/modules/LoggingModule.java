package me.vinceh121.gmcserver.modules;

import java.util.Date;

import com.mongodb.client.model.Filters;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.event.StandardIntent;

public class LoggingModule extends AbstractModule {
	public static final String ERROR_SYNTAX = "The syntax of one of the logging parameters is incorrect";

	public LoggingModule(final GMCServer srv) {
		super(srv);
		this.registerRoute(HttpMethod.GET, "/log2", this::handleLog2);
	}

	private void handleLog2(final RoutingContext ctx) {
		// AID: user id
		// GID: device id
		// CPM
		// ACPM
		// uSV
		final long gmcUserId;
		try {
			gmcUserId = Long.parseLong(ctx.request().getParam("AID"));
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, GMCServer.ERROR_USER_ID);
			return;
		}

		final long gmcDeviceId;
		try {
			gmcDeviceId = Long.parseLong(ctx.request().getParam("GID"));
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, GMCServer.ERROR_DEVICE_ID);
			return;
		}

		final double cpm;
		try {
			cpm = Double.parseDouble(ctx.request().getParam("CPM"));
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final double acpm;
		try {
			acpm = Double.parseDouble(ctx.request().getParam("ACPM"));
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final double usv;
		try {
			usv = Double.parseDouble(ctx.request().getParam("uSv"));
		} catch (final NumberFormatException e) {
			this.error(ctx, 400, LoggingModule.ERROR_SYNTAX);
			return;
		}

		final User user = this.srv.getColUsers().find(Filters.eq("gmcId", gmcUserId)).first();
		if (user == null) {
			this.error(ctx, 404, GMCServer.ERROR_USER_ID);
			return;
		}

		final Device device = this.srv.getColDevices().find(Filters.eq("gmcId", gmcDeviceId)).first();
		if (device == null) {
			this.error(ctx, 404, GMCServer.ERROR_DEVICE_ID);
			return;
		}

		if (device.getOwner() == null) {
			this.log.error("Device with no owner: {}", device.getId());
			this.error(ctx, 403, GMCServer.ERROR_DEVICE_NOT_OWNED);
			return;
		}

		if (!device.getOwner().equals(user.getId())) {
			this.error(ctx, 403, GMCServer.ERROR_DEVICE_NOT_OWNED);
			return;
		}

		final Record rec = new Record();
		rec.setDate(new Date());
		rec.setAcpm(acpm);
		rec.setCpm(cpm);
		rec.setDeviceId(device.getId());
		rec.setUserId(user.getId());
		rec.setUsv(usv);

		if (Boolean.parseBoolean(this.srv.getConfig().getProperty("geiger.log-ip"))) {
			rec.setIp(ctx.request().remoteAddress().host());
		}

		this.log.debug("Inserting record {}", rec);

		this.srv.getColRecords().insertOne(rec);
		ctx.response().setStatusCode(200).end();

		this.srv.getWebsocketManager()
				.sendIntent(user.getId(),
						StandardIntent.LOG2_RECORD.create(new JsonObject().put("record", rec.toJson())));
	}
}

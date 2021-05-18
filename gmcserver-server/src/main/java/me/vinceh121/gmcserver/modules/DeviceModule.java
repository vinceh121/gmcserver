package me.vinceh121.gmcserver.modules;

import java.util.Date;
import java.util.Iterator;

import org.bson.types.ObjectId;

import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.managers.DeviceManager.CreateDeviceAction;
import me.vinceh121.gmcserver.managers.DeviceManager.DeviceFullTimelineAction;
import me.vinceh121.gmcserver.managers.DeviceManager.DeviceStatsAction;
import me.vinceh121.gmcserver.managers.DeviceManager.GetDeviceAction;
import me.vinceh121.gmcserver.managers.DeviceManager.UpdateDeviceAction;
import me.vinceh121.gmcserver.managers.UserManager.GetUserAction;

public class DeviceModule extends AbstractModule {

	public DeviceModule(final GMCServer srv) {
		super(srv);
		this.registerStrictAuthedRoute(HttpMethod.POST, "/device", this::handleCreateDevice);
		this.registerStrictAuthedRoute(HttpMethod.DELETE, "/device/:deviceId", this::handleRemoveDevice);
		this.registerStrictAuthedRoute(HttpMethod.PUT, "/device/:deviceId", this::handleUpdateDevice);
		this.registerAuthedRoute(HttpMethod.GET, "/device/:deviceId", this::handleDevice);
		this.registerAuthedRoute(HttpMethod.GET, "/device/:deviceId/timeline", this::handleDeviceHistory);
		this.registerRoute(HttpMethod.GET, "/device/:deviceId/stats/:field", this::handleStats);
		this.registerRoute(HttpMethod.GET, "/device/:deviceId/live", this::handleLive);
		this.registerRoute(HttpMethod.GET, "/device/:deviceId/calendar", this::handleCalendar);
	}

	private void handleCreateDevice(final RoutingContext ctx) {
		final JsonObject obj = ctx.getBodyAsJson();

		final User user = ctx.get(AuthHandler.USER_KEY);

		final String name = obj.getString("name");
		if (name == null || name.length() > 2 && name.length() < 64) {
			this.error(ctx, 400, "Invalid name");
			return;
		}

		final String model = obj.getString("model");
		if (model == null || name.length() > 2 && name.length() < 64) {
			this.error(ctx, 400, "Invalid model");
			return;
		}

		final JsonArray arrLoc = obj.getJsonArray("position");
		if (arrLoc == null || arrLoc.size() != 2) {
			this.error(ctx, 400, "Invalid position");
			return;
		}

		final JsonObject objProxies = obj.getJsonObject("proxiesSettings");
		this.srv.getProxyManager().validateProxiesSettings().setProxiesSettings(objProxies).execute().onSuccess(v -> {
			final CreateDeviceAction action = this.srv.getDeviceManager()
				.createDevice()
				.setModel(model)
				.setUser(user)
				.setArrLocation(arrLoc)
				.setName(name);
			action.execute().onSuccess(dev -> {
				ctx.response().end(dev.toJson().toBuffer());
			}).onFailure(t -> {
				this.error(ctx, 400, t.getMessage());
			});
		}).onFailure(t -> {
			this.error(ctx, 400, "Couldn't validate proxiesSettings: " + t.getMessage());
		});
	}

	private void handleRemoveDevice(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId");

		final ObjectId deviceId;
		try {
			deviceId = new ObjectId(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final User user = ctx.get(AuthHandler.USER_KEY);

		this.srv.getDeviceManager().getDevice().setId(deviceId).execute().onSuccess(dev -> {
			if (user.getId().equals(dev.getOwner())) {
				this.error(ctx, 403, "Device not owned");
				return;
			}

			final JsonObject obj = ctx.getBodyAsJson();
			final boolean delete = obj.getBoolean("delete");

			this.srv.getDeviceManager()
				.deleteDevice()
				.setDelete(delete)
				.setDeviceId(deviceId)
				.execute()
				.onSuccess(res -> {
					ctx.response().end(new JsonObject().put("delete", delete).toBuffer());
				})
				.onFailure(t -> {
					this.error(ctx, 400, t.getMessage());
				});
		}).onFailure(t -> {
			this.error(ctx, 404, "Device not found");
		});
	}

	private void handleUpdateDevice(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId"); // TODO make action somehow

		final ObjectId deviceId;
		try {
			deviceId = new ObjectId(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final JsonObject obj = ctx.getBodyAsJson();

		final GetDeviceAction getAction = this.srv.getDeviceManager().getDevice().setId(deviceId);
		getAction.execute().onSuccess(dev -> {
			final User user = ctx.get(AuthHandler.USER_KEY);

			if (!user.getId().equals(dev.getOwner())) {
				this.error(ctx, 403, "Not owner of the device");
				return;
			}

			final UpdateDeviceAction action = this.srv.getDeviceManager()
				.updateDevice()
				.setDevice(dev)
				.setArrLocation(obj.getJsonArray("location"))
				.setModel(obj.getString("model"))
				.setName(obj.getString("name"))
				.setProxiesSettings(obj.getJsonObject("proxiesSettings"));
			action.execute().onSuccess(upRes -> {
				ctx.response().end(new JsonObject().put("changed", upRes).toBuffer());
			}).onFailure(t -> {
				this.error(ctx, 500, t.getMessage());
			});
		}).onFailure(t -> {
			this.error(ctx, 404, t.getMessage());
		});

	}

	private void handleDevice(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId");

		final ObjectId deviceId;
		try {
			deviceId = new ObjectId(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final GetDeviceAction action = this.srv.getDeviceManager().getDevice().setId(deviceId);

		action.execute().onSuccess(dev -> {
			final User user = ctx.get(AuthHandler.USER_KEY);

			final GetUserAction getOwnerAction = this.srv.getUserManager().getUser().setId(dev.getOwner());
			getOwnerAction.execute().onSuccess(ures -> {
				final boolean own = user != null && user.getId().equals(dev.getOwner());

				final JsonObject obj = own ? dev.toJson() : dev.toPublicJson();
				obj.put("own", own);
				obj.put("owner", ures.toPublicJson());

				ctx.response().end(obj.toBuffer());
			});
		}).onFailure(t -> {
			this.error(ctx, 404, t.getMessage());
		});
	}

	private void handleDeviceHistory(final RoutingContext ctx) {
		final User user = ctx.get(AuthHandler.USER_KEY);
		final String rawDeviceId = ctx.pathParam("deviceId");

		final ObjectId deviceId;
		try {
			deviceId = new ObjectId(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final GetDeviceAction getAction = this.srv.getDeviceManager().getDevice().setId(deviceId);
		getAction.execute().onSuccess(dev -> {
			final Date start, end;

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

			final boolean full = "y".equals(ctx.request().getParam("full"));

			final DeviceFullTimelineAction histAction = this.srv.getDeviceManager()
				.deviceFullTimeline()
				.setStart(start)
				.setEnd(end)
				.setFull(full)
				.setDev(dev);
			histAction.execute().onSuccess(hist -> {
				ctx.response().setChunked(true);

				ctx.response().write("[");

				final Iterator<Record> recs = hist.iterator();

				recs.forEachRemaining(r -> {
					if (user != null && user.getId().equals(dev.getOwner())) {
						ctx.response().write(r.toJson().toString());
					} else {
						ctx.response().write(r.toPublicJson().toString());
					}
					if (recs.hasNext()) {
						ctx.response().write(",");
					}
				});
				ctx.response().write("]");
				ctx.response().end();
			}).onFailure(t -> {
				this.error(ctx, 500, t.getMessage());
			});
		}).onFailure(t -> {
			this.error(ctx, 404, "Device not found");
		});

	}

	private void handleStats(final RoutingContext ctx) {
		final String rawDevId = ctx.pathParam("deviceId");

		final ObjectId devId;
		try {
			devId = new ObjectId(rawDevId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid ID");
			return;
		}

		final String field = ctx.pathParam("field");
		if (!Record.STAT_FIELDS.contains(field)) {
			this.error(ctx, 400, "Invalid field");
			return;
		}

		final GetDeviceAction getDevAction = this.srv.getDeviceManager().getDevice().setId(devId);
		getDevAction.execute().onSuccess(dev -> {
			final DeviceStatsAction action = this.srv.getDeviceManager()
				.deviceStats()
				.setDevId(dev.getId())
				.setField(field);

			action.execute().onSuccess(res -> {
				final JsonObject obj = res.toJson();
				obj.remove("id");

				ctx.response().end(obj.toBuffer());
			}).onFailure(t -> {
				this.error(ctx, 204, "No statistical data for this field");
			});
		}).onFailure(t -> {
			this.error(ctx, 404, "Device not found");
		});
	}

	private void handleLive(final RoutingContext ctx) {
		final String rawDevId = ctx.pathParam("deviceId");

		final ObjectId devId;
		try {
			devId = new ObjectId(rawDevId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid ID");
			return;
		}

		final GetDeviceAction getDevAction = this.srv.getDeviceManager().getDevice().setId(devId);
		getDevAction.execute().onSuccess(getRes -> {
			ctx.request().toWebSocket(webRes -> {
				if (webRes.failed()) {
					this.error(ctx, 500, "Failed to open websocket: " + webRes.cause());
					return;
				}
				final ServerWebSocket sock = webRes.result();

				final MessageConsumer<Record> consumer = this.srv.getEventBus()
					.consumer(LoggingModule.ADDRESS_PREFIX_RECORD_LOG + devId.toHexString());

				consumer.handler(msg -> sock.writeTextMessage(msg.body().toPublicJson().encode()));

				sock.closeHandler(v -> consumer.unregister());
				sock.exceptionHandler(t -> {
					consumer.unregister();
					sock.close((short) 1011);
				});
			});
		}).onFailure(t -> {
			this.error(ctx, 404, "Device not found");
		});
	}

	private void handleCalendar(final RoutingContext ctx) {
		final String rawDevId = ctx.pathParam("deviceId");

		final ObjectId devId;
		try {
			devId = new ObjectId(rawDevId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid ID");
			return;
		}

		this.srv.getDeviceCalendarManager().getCalendar().setDeviceId(devId).execute().onSuccess(cal -> {
			if (cal == null || cal.isInProgress()) {
				this.error(ctx, 202, "Calendar is loading");
				return;
			}

			ctx.response().end(cal.toPublicJson().toBuffer());
		}).onFailure(t -> {
			this.error(ctx, 500, "Failed to get calendar: " + t);
		});
	}
}

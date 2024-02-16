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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.stream.Stream;

import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.pgclient.data.Point;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.exceptions.EntityNotFoundException;
import me.vinceh121.gmcserver.exceptions.LimitReachedException;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.managers.DeviceManager.CreateDeviceAction;
import me.vinceh121.gmcserver.managers.DeviceManager.DeviceFullTimelineAction;
import me.vinceh121.gmcserver.managers.DeviceManager.DeviceStatsAction;
import me.vinceh121.gmcserver.managers.DeviceManager.GetDeviceAction;
import me.vinceh121.gmcserver.managers.DeviceManager.UpdateDeviceAction;
import me.vinceh121.gmcserver.managers.LoggingManager;
import me.vinceh121.gmcserver.managers.UserManager.GetUserAction;

public class DeviceModule extends AbstractModule {
	public static final DateFormat LAST_MODIFIED_DATE = new SimpleDateFormat("EEE, dd MMM yyyy kk:mm:ss ZZZ");

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
		final JsonObject obj = ctx.body().asJsonObject();

		final User user = ctx.get(AuthHandler.USER_KEY);

		final String name = obj.getString("name");
		if (name == null || name.length() < 2 && name.length() > 64) {
			this.error(ctx, 400, "Invalid name");
			return;
		}

		final String model = obj.getString("model");
		if (model != null && name.length() > 64) {
			this.error(ctx, 400, "Invalid model");
			return;
		}

		final JsonArray arrLoc = obj.getJsonArray("position");
		if (arrLoc == null || arrLoc.size() != 2 && arrLoc.size() != 3) {
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
				if (t instanceof LimitReachedException) {
					this.error(ctx, 406, "Device limit reached");
				} else {
					this.error(ctx, 500, "Failed to create device: " + t.getMessage());
				}
			});
		}).onFailure(t -> {
			if (t instanceof EntityNotFoundException || t instanceof IllegalArgumentException) {
				this.error(ctx, 400, t.getMessage());
			} else {
				this.error(ctx, 500, "Failed to validate proxy settings");
			}
		});
	}

	private void handleRemoveDevice(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId");

		final UUID deviceId;
		try {
			deviceId = UUID.fromString(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final User user = ctx.get(AuthHandler.USER_KEY);

		this.srv.getDeviceManager().getDevice().setId(deviceId).execute().onSuccess(dev -> {
			if (!user.getId().equals(dev.getOwner())) {
				this.error(ctx, 403, "Device not owned");
				return;
			}

			final JsonObject obj = ctx.body().asJsonObject();
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
					if (t instanceof EntityNotFoundException) {
						this.error(ctx, 404, "Device not found");
					} else {
						this.error(ctx, 500, "Failed to delete device: " + t);
					}
				});
		}).onFailure(t -> {
			this.error(ctx, 404, "Device not found");
		});
	}

	private void handleUpdateDevice(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId"); // TODO make action somehow

		final UUID deviceId;
		try {
			deviceId = UUID.fromString(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final JsonObject obj = ctx.body().asJsonObject();

		final String name = obj.getString("name");
		if (name != null && name.length() > 2 && name.length() < 64) {
			this.error(ctx, 400, "Invalid name");
			return;
		}

		final String model = obj.getString("model");
		if (model != null && model.length() > 64) {
			this.error(ctx, 400, "Invalid model");
			return;
		}

		if (obj.containsKey("proxiesSettings") && !(obj.getValue("proxiesSettings") instanceof JsonObject)) {
			this.error(ctx, 400, "Invalid proxiesSettings");
			return;
		}

		final Point loc;
		if (obj.containsKey("location") && obj.getValue("location") instanceof JsonArray) {
			final JsonArray arr = obj.getJsonArray("location");

			if (arr.size() != 2 && arr.size() != 3) {
				this.error(ctx, 400, "Invalid location");
				return;
			}
			for (final Object o : obj.getJsonArray("location")) {
				if (!(o instanceof Number)) {
					this.error(ctx, 400, "Invalid location");
					return;
				}
			}

			final double longitude = arr.getDouble(0);
			final double latitude = arr.getDouble(1);
//			final double altitude = arr.getDouble(2);

//			if (longitude != 0 && latitude != 0 && altitude != 0) {
//				loc = new Point(longitude, latitude, altitude);
//			} else
			if (longitude != 0 && latitude != 0) {
				loc = new Point(longitude, latitude);
			} else {
				loc = null;
			}
		} else {
			loc = null;
		}

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
				.setLocation(loc)
				.setModel(model)
				.setName(name)
				.setProxiesSettings(obj.getJsonObject("proxiesSettings"));
			action.execute().onSuccess(upRes -> {
				ctx.response().end(new JsonObject().put("changed", upRes).toBuffer());
			}).onFailure(t -> {
				if (t instanceof IllegalArgumentException) {
					this.error(ctx, 404, t.getMessage());
				} else {
					this.error(ctx, 500, t.getMessage());
				}
			});
		}).onFailure(t -> {
			if (t instanceof EntityNotFoundException) {
				this.error(ctx, 404, "Device not found");
			} else {
				this.error(ctx, 500, "Failed to fetch device: " + t.getMessage());
			}
		});

	}

	private void handleDevice(final RoutingContext ctx) {
		final String rawDeviceId = ctx.pathParam("deviceId");

		final UUID deviceId;
		try {
			deviceId = UUID.fromString(rawDeviceId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid device ID");
			return;
		}

		final GetDeviceAction action = this.srv.getDeviceManager().getDevice().setFetchLastRecord(true).setId(deviceId);

		action.execute().onSuccess(dev -> {
			final User user = ctx.get(AuthHandler.USER_KEY);

			final GetUserAction getOwnerAction = this.srv.getUserManager().getUser().setId(dev.getOwner());
			getOwnerAction.execute().onSuccess(ures -> {
				final boolean own = user != null && user.getId().equals(dev.getOwner());

				final JsonObject obj = own ? dev.toJson() : dev.toPublicJson();
				obj.put("own", own);
				obj.put("owner", ures.toPublicJson());

				ctx.response().end(obj.toBuffer());
			}).onFailure(t -> {
				if (t instanceof EntityNotFoundException) {
					this.error(ctx, 500, "User not found"); // means we have an owner-less device
				} else {
					this.error(ctx, 500, "Failed to fetch user: " + t.getMessage());
				}
			});
		}).onFailure(t -> {
			if (t instanceof EntityNotFoundException) {
				this.error(ctx, 404, "Device not found");
			} else {
				this.error(ctx, 500, "Failed to fetch device: " + t.getMessage());
			}
		});
	}

	private void handleDeviceHistory(final RoutingContext ctx) {
		final User user = ctx.get(AuthHandler.USER_KEY);
		final String rawDeviceId = ctx.pathParam("deviceId");

		final UUID deviceId;
		try {
			deviceId = UUID.fromString(rawDeviceId);
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
			histAction.execute().onSuccess(histIter -> {
				final Iterator<Record> hist = histIter.iterator();
				final Record firstRec = hist.next();

				if (firstRec != null) {
					final Date lastDate = firstRec.getDate();
					ctx.response().putHeader("Last-Modified", LAST_MODIFIED_DATE.format(lastDate));

					if (ctx.request().headers().contains("If-Modified-Since")) {
						try {
							final Date reqDate = LAST_MODIFIED_DATE.parse(ctx.request().getHeader("If-Modified-Since"));
							if (lastDate.after(reqDate) || lastDate.equals(reqDate)) {
								// timeline hasn't changed since last request
								ctx.response().setStatusCode(304).end();
								return;
							}
						} catch (ParseException e) {
							this.error(ctx, 400, "Invalid If-Modified-Since date");
							return;
						}
					}
				}

				ctx.response().setChunked(true);

				ctx.response().write("[");

				// use this stream utility in order to keep the first record
				Stream.iterate(firstRec, r -> hist.hasNext(), r -> hist.next()).forEach(r -> {
					if (user != null && user.getId().equals(dev.getOwner())) {
						ctx.response().write(r.toJson().toString());
					} else {
						ctx.response().write(r.toPublicJson().toString());
					}

					if (hist.hasNext()) {
						ctx.response().write(",");
					}
				});

				ctx.response().write("]");
				ctx.response().end();
			}).onFailure(t -> this.error(ctx, 500, t.getMessage()));
		}).onFailure(t -> {
			if (t instanceof EntityNotFoundException) {
				this.error(ctx, 404, "Device not found");
			} else {
				this.error(ctx, 500, "Failed to fetch device: " + t.getMessage());
			}
		});

	}

	private void handleStats(final RoutingContext ctx) {
		final String rawDevId = ctx.pathParam("deviceId");

		final UUID devId;
		try {
			devId = UUID.fromString(rawDevId);
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

			final DeviceStatsAction action = this.srv.getDeviceManager()
				.deviceStats()
				.setDevId(dev.getId())
				.setField(field)
				.setStart(start)
				.setEnd(end);

			action.execute().onSuccess(res -> {
				final JsonObject obj = res.toJson();
				obj.remove("id");

				ctx.response().end(obj.toBuffer());
			}).onFailure(t -> {
				if (t instanceof IllegalStateException) {
					this.error(ctx, 204, "No statistical data for this field");
				} else {
					this.error(ctx, 500, "Failed to fetch stats: " + t.getMessage());
				}
			});
		}).onFailure(t -> {
			if (t instanceof EntityNotFoundException) {
				this.error(ctx, 404, "Device not found");
			} else {
				this.error(ctx, 500, "Failed to fetch device: " + t.getMessage());
			}
		});
	}

	private void handleLive(final RoutingContext ctx) {
		final String rawDevId = ctx.pathParam("deviceId");

		final UUID devId;
		try {
			devId = UUID.fromString(rawDevId);
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
					.consumer(LoggingManager.ADDRESS_PREFIX_RECORD_LOG + devId.toString());

				consumer.handler(msg -> sock.writeTextMessage(msg.body().toPublicJson().encode()));

				sock.closeHandler(v -> consumer.unregister());
				sock.exceptionHandler(t -> {
					consumer.unregister();
					sock.close((short) 1011);
				});
			});
		}).onFailure(t -> {
			if (t instanceof EntityNotFoundException) {
				this.error(ctx, 404, "Device not found");
			} else {
				this.error(ctx, 500, "Failed to fetch device: " + t.getMessage());
			}
		});
	}

	private void handleCalendar(final RoutingContext ctx) {
		final String rawDevId = ctx.pathParam("deviceId");

		final UUID devId;
		try {
			devId = UUID.fromString(rawDevId);
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid ID");
			return;
		}

		this.srv.getDeviceManager().getDevice().setId(devId).execute().onSuccess(dev -> {
			this.srv.getDeviceCalendarManager().getCalendar().setDeviceId(dev.getId()).execute().onSuccess(cal -> {
				if (cal == null || cal.isInProgress()) {
					this.error(ctx, 202, "Calendar is loading");
					return;
				}

				ctx.response().end(cal.toPublicJson().toBuffer());
			}).onFailure(t -> {
				this.error(ctx, 500, "Failed to get calendar: " + t);
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

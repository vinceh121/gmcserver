package me.vinceh121.gmcserver.modules;

import org.bson.types.ObjectId;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.AuthHandler;

public class UserModule extends AbstractModule {

	public UserModule(final GMCServer srv) {
		super(srv);
		this.registerAuthedRoute(HttpMethod.GET, "/user/me", this::handleMe);
		this.registerAuthedRoute(HttpMethod.GET, "/user/:id", this::handleUser);
	}

	private void handleMe(final RoutingContext ctx) {
		final User user = ctx.get(AuthHandler.USER_KEY);
		if (user != null) {
			ctx.reroute(HttpMethod.GET, "/user/" + user.getId().toHexString());
		} else {
			this.error(ctx, 404, "Not logged in");
		}
	}

	private void handleUser(final RoutingContext ctx) {
		final User authUser = ctx.get(AuthHandler.USER_KEY);

		final ObjectId requestedId;
		try {
			requestedId = new ObjectId(ctx.pathParam("id"));
		} catch (final IllegalArgumentException e) {
			this.error(ctx, 400, "Invalid ID");
			return;
		}

		final User user;
		if (authUser != null && requestedId.equals(authUser.getId())) {
			user = authUser;
		} else {
			user = this.srv.getDatabaseManager().getCollection(User.class).find(Filters.eq(requestedId)).first();
		}

		if (user == null) {
			this.error(ctx, 404, "User not found");
			return;
		}

		final JsonObject obj;
		if (authUser != null && requestedId.equals(authUser.getId())) {
			obj = user.toJson();
			obj.put("self", true);
		} else {
			obj = user.toPublicJson();
		}

		final JsonArray devs = new JsonArray();

		final FindIterable<Device> it
				= this.srv.getDatabaseManager().getCollection(Device.class).find(Filters.eq("owner", user.getId()));
		it.forEach(d -> devs.add(d.toPublicJson()));

		obj.put("devices", devs);

		ctx.response().end(obj.toBuffer());
	}

}

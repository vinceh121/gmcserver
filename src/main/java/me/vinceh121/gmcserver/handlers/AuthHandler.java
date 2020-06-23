package me.vinceh121.gmcserver.handlers;

import java.security.SignatureException;

import org.bson.types.ObjectId;

import com.mongodb.client.model.Filters;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import xyz.bowser65.tokenize.IAccount;
import xyz.bowser65.tokenize.Token;

public class AuthHandler implements Handler<RoutingContext> {
	public static final String TOKEN_KEY = "me.vinceh121.gmcserver.token";
	public static final String USER_KEY = "me.vinceh121.gmcserver.user";
	private final GMCServer srv;

	public AuthHandler(final GMCServer srv) {
		this.srv = srv;
	}

	@Override
	public void handle(final RoutingContext ctx) {
		final String auth = ctx.request().getHeader("Authorization");
		if (auth == null) {
			ctx.next(); // non strict auth
			return;
		}

		final Token token;
		try {
			token = this.srv.getTokenize().validateToken(auth, this::fetchAccount);
		} catch (final SignatureException e) {
			ctx.next(); // non strict auth
			return;
		}

		final User user = (User) token.getAccount();
		ctx.put(AuthHandler.TOKEN_KEY, token);
		ctx.put(AuthHandler.USER_KEY, user);

		ctx.next();
	}

	private IAccount fetchAccount(final String id) {
		return this.srv.getColUsers().find(Filters.eq(new ObjectId(id))).first();
	}
}

package me.vinceh121.gmcserver.event;

import java.security.SignatureException;
import java.util.Hashtable;
import java.util.Objects;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Filters;

import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import xyz.bowser65.tokenize.IAccount;
import xyz.bowser65.tokenize.Token;

public class WebsocketManager implements Handler<ServerWebSocket> {
	private static final Logger LOG = LoggerFactory.getLogger(WebsocketManager.class);
	private final GMCServer srv;
	private final Hashtable<ObjectId, WebsocketSession> sessions = new Hashtable<>();

	public WebsocketManager(final GMCServer srv) {
		this.srv = srv;
	}

	@Override
	public void handle(final ServerWebSocket socket) {
		if (!socket.path().equals("/ws")) {
			socket.close((short) 404);
			return;
		}

		final String auth = socket.headers().get("Authorization");
		if (auth == null) { // dupe dupe code
			socket.close((short) 403, "Invalid token");
			return;
		}

		final Token token;
		try {
			token = this.srv.getTokenize().validateToken(auth, this::fetchAccount);
		} catch (final SignatureException e) {
			socket.close((short) 403, "Invalid token");
			return;
		}

		final User user = (User) token.getAccount();
		LOG.info("User {} opened a websocket session", user);
		final WebsocketSession sess = new WebsocketSession(user, socket);
		sessions.put(user.getId(), sess);

		this.sendIntent(user.getId(), StandardIntent.HANDHAKE_COMPLETE.create(new JsonObject()));
	}

	public Hashtable<ObjectId, WebsocketSession> getSessions() {
		return sessions;
	}

	public WebsocketSession getSession(final ObjectId userId) {
		return getSessions().get(userId);
	}

	public void sendIntent(final ObjectId oid, final Intent intent) {
		Objects.requireNonNull(oid);
		Objects.requireNonNull(intent);

		final WebsocketSession session = getSession(oid);
		if (session == null)
			return;
		session.getSocket().writeTextMessage(intent.toJson().encode());
	}

	private IAccount fetchAccount(final String id) {
		return this.srv.getColUsers().find(Filters.eq(new ObjectId(id))).first();
	}

}

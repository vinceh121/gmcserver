package me.vinceh121.gmcserver.event;

import java.security.SignatureException;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.collections4.multimap.AbstractMultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
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
	private final int SESSION_LIMIT;
	private final GMCServer srv;
	private final AbstractMultiValuedMap<ObjectId, WebsocketSession> sessions = new ArrayListValuedHashMap<>();

	public WebsocketManager(final GMCServer srv) {
		this.srv = srv;
		this.SESSION_LIMIT = Integer.parseInt(this.srv.getConfig().getProperty("server.session-limit"));
	}

	@Override
	public void handle(final ServerWebSocket socket) {
		if (!socket.path().equals("/api/v1/ws")) {
			socket.close((short) 404);
			return;
		}

		final String auth = socket.headers().get("Sec-WebSocket-Protocol");
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
		if (user == null) {
			socket.close((short) 403, "Invalid token");
			return;
		}
		WebsocketManager.LOG.debug("User {} opened a websocket session", user);

		if (this.sessions.get(user.getId()).size() >= this.SESSION_LIMIT) {
			socket.close((short) 403, "Reacher Websocket session limit for user");
			WebsocketManager.LOG.info("User {} reached the limit of opened sessions", user);
			return;
		}

		final WebsocketSession sess = new WebsocketSession(user, socket);
		this.sessions.put(user.getId(), sess);

		this.sendIntent(user.getId(), StandardIntent.HANDSHAKE_COMPLETE.create(new JsonObject()));

		sess.getSocket().closeHandler(v -> {
			WebsocketManager.LOG.debug("User {} closed his websocket session", user.toString());
			this.sessions.remove(user.getId());
		});
	}

	public AbstractMultiValuedMap<ObjectId, WebsocketSession> getSessions() {
		return this.sessions;
	}

	public Collection<WebsocketSession> getSessions(final ObjectId userId) {
		return this.getSessions().get(userId);
	}

	public void sendIntent(final ObjectId oid, final Intent intent) {
		Objects.requireNonNull(oid);
		Objects.requireNonNull(intent);

		final Collection<WebsocketSession> sess = this.getSessions(oid);
		if (sess == null) {
			return;
		}
		for (final WebsocketSession session : sess) {
			session.getSocket().writeTextMessage(intent.toJson().encode());
		}
	}

	private IAccount fetchAccount(final String id) {
		return this.srv.getDatabaseManager()
				.getCollection(User.class)
				.find(Filters.eq(new ObjectId(id)))
				.first();
	}

}

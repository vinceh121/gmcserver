package me.vinceh121.gmcserver.event;

import java.util.Collection;
import java.util.Objects;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.http.ServerWebSocket;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.managers.AbstractManager;
import me.vinceh121.gmcserver.managers.UserManager;
import me.vinceh121.gmcserver.managers.UserManager.VerifyTokenAction;
import xyz.bowser65.tokenize.Token;

/**
 * Note on using vert.x eventbus: no
 *
 * As of this writing the storing of sessions is done locally so using the event
 * bus would not work properly in cases of clustered instances
 */
public class WebsocketManager extends AbstractManager implements Handler<ServerWebSocket> {
	public static final String EVENT_INTENT_BUS = WebsocketManager.class.getCanonicalName() + ".EVENT_INTENT_BUS";
	private static final Logger LOG = LoggerFactory.getLogger(WebsocketManager.class);
	private final int SESSION_LIMIT;
	private final MultiValuedMap<ObjectId, WebsocketSession> sessions = new ArrayListValuedHashMap<>();

	public WebsocketManager(final GMCServer srv) {
		super(srv);
		this.SESSION_LIMIT = Integer.parseInt(this.srv.getConfig().getProperty("server.session-limit"));
	}

	@Override
	public void handle(final ServerWebSocket socket) {
		if (!socket.path().equals("/api/v1/ws")) {
			socket.close((short) 404);
			return;
		}

		final String auth = socket.headers().get("Sec-WebSocket-Protocol");

		final VerifyTokenAction tokenAction = this.srv.getManager(UserManager.class).verifyToken().setTokenString(auth);
		tokenAction.execute().onComplete(res -> {
			if (res.failed()) {
				socket.close((short) 403);
				return;
			}
			final Token token = res.result();

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

			this.sendIntent(user.getId(), StandardIntent.HANDSHAKE_COMPLETE.create());

			sess.getSocket().closeHandler(v -> {
				WebsocketManager.LOG.debug("User {} closed his websocket session", user.toString());
				this.sessions.remove(user.getId());
			});
		});
	}

	public MultiValuedMap<ObjectId, WebsocketSession> getSessions() {
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
}

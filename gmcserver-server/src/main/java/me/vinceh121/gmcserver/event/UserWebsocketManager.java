package me.vinceh121.gmcserver.event;

import java.util.Collection;
import java.util.Objects;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.managers.AbstractManager;
import me.vinceh121.gmcserver.managers.UserManager.VerifyTokenAction;
import xyz.bowser65.tokenize.Token;

public class UserWebsocketManager extends AbstractManager implements Handler<RoutingContext> {
	/**
	 * Must be suffixed by hex User ID
	 */
	public static final String ADDRESS_PREFIX_USER_INTENT = "me.vinceh121.gmcserver.ADDRESS_USER_INTENT.";
	private static final Logger LOG = LoggerFactory.getLogger(UserWebsocketManager.class);
	private final int SESSION_LIMIT;
	private final MultiValuedMap<ObjectId, UserWebsocketSession> sessions = new ArrayListValuedHashMap<>();

	public UserWebsocketManager(final GMCServer srv) {
		super(srv);
		this.SESSION_LIMIT = Integer.parseInt(this.srv.getConfig().getProperty("server.session-limit"));
	}

	@Override
	public void handle(final RoutingContext ctx) {
		ctx.request().toWebSocket(sockRes -> {
			if (sockRes.failed()) {
				ctx.response().setStatusCode(500).end();
				return;
			}

			final ServerWebSocket socket = sockRes.result();

			final String auth = socket.headers().get("Sec-WebSocket-Protocol");

			final VerifyTokenAction tokenAction = this.srv.getUserManager().verifyToken().setTokenString(auth);
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
				UserWebsocketManager.LOG.debug("User {} opened a websocket session", user);

				if (this.sessions.get(user.getId()).size() >= this.SESSION_LIMIT) {
					socket.close((short) 403, "Reached Websocket session limit for user");
					UserWebsocketManager.LOG.info("User {} reached the limit of opened sessions", user);
					return;
				}

				final UserWebsocketSession sess = new UserWebsocketSession(user, socket);
				this.sessions.put(user.getId(), sess);

				final String userAddress = UserWebsocketManager.ADDRESS_PREFIX_USER_INTENT + user.getId().toHexString();

				final MessageConsumer<UserIntent> intentConsumer = this.srv.getEventBus().consumer(userAddress, msg -> {
					final UserIntent intent = msg.body();
					this.sendIntentLocal(intent);
				});

				this.srv.getEventBus().publish(userAddress, StandardUserIntent.HANDSHAKE_COMPLETE.create(user.getId()));

				sess.getSocket().closeHandler(v -> {
					UserWebsocketManager.LOG.debug("User {} closed his websocket session", user.toString());
					intentConsumer.unregister();
					this.sessions.remove(user.getId());
				});
			});
		});
	}

	public MultiValuedMap<ObjectId, UserWebsocketSession> getSessions() {
		return this.sessions;
	}

	public Collection<UserWebsocketSession> getSessions(final ObjectId userId) {
		return this.getSessions().get(userId);
	}

	private void sendIntentLocal(final UserIntent intent) {
		Objects.requireNonNull(intent);

		final Collection<UserWebsocketSession> sess = this.getSessions(intent.getDestination());
		if (sess == null) {
			return;
		}
		for (final UserWebsocketSession session : sess) {
			session.getSocket().writeTextMessage(intent.toJson().encode());
		}
	}
}

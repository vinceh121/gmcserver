package me.vinceh121.gmcserver.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.LdapEntry;
import org.ldaptive.LdapException;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.SearchDnResolver;
import org.ldaptive.auth.SimpleBindAuthenticationHandler;

import com.mongodb.client.model.Filters;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.managers.UserManager.CreateUserAction;

public class LdapAuthenticator extends AbstractAuthenticator {
	private static final Logger LOG = LogManager.getLogger(LdapAuthenticator.class);
	private final Authenticator auth;
	private final String fieldEmail, fieldUid;

	public LdapAuthenticator(final GMCServer srv) {
		super(srv);

		this.fieldEmail = srv.getConfig().getProperty("auth.ldap.field.email", "mail");
		this.fieldUid = srv.getConfig().getProperty("auth.ldap.field.uid", "uid");

		final ConnectionConfig conConfig = ConnectionConfig.builder()
				.url(srv.getConfig().getProperty("auth.ldap.url"))
				.useStartTLS(Boolean.parseBoolean(srv.getConfig().getProperty("auth.ldap.startTls")))
				.connectionInitializers(new BindConnectionInitializer(srv.getConfig().getProperty("auth.ldap.bindDn"),
						new Credential(srv.getConfig().getProperty("auth.ldap.password"))))
				.build();

		final SearchDnResolver dnResolver = SearchDnResolver.builder()
				.factory(new DefaultConnectionFactory(conConfig))
				.dn(srv.getConfig().getProperty("auth.ldap.baseDn"))
				.filter("(" + this.fieldUid + "={user})")
				.build();

		final SimpleBindAuthenticationHandler authHandler
				= new SimpleBindAuthenticationHandler(new DefaultConnectionFactory(conConfig));

		this.auth = new Authenticator(dnResolver, authHandler);
	}

	@Override
	public Future<User> login(final String username, final String password) {
		return Future.future(promise -> {
			final AuthenticationResponse res;
			try {
				res = this.auth.authenticate(
						new AuthenticationRequest(username, new Credential(password), this.fieldEmail, this.fieldUid));
			} catch (final LdapException e) {
				promise.fail(e);
				return;
			}

			if (res.isSuccess()) {
				final LdapEntry e = res.getLdapEntry();
				this.fetchOrCreateInternalUser(e, promise);
			} else {
				promise.fail(res.getDiagnosticMessage());
			}
		});
	}

	private void fetchOrCreateInternalUser(final LdapEntry e, final Promise<User> promise) {
		final String uid = e.getAttribute(this.fieldUid).getStringValue();
		final String email = e.getAttribute(this.fieldEmail).getStringValue();

		final User user
				= this.srv.getDatabaseManager().getCollection(User.class).find(Filters.eq("username", uid)).first();

		if (user != null) {
			promise.complete(user);
		} else {
			LdapAuthenticator.LOG.info("First login for LDAP user {} <{}>", uid, email);
			final CreateUserAction userCreate = this.srv.getUserManager()
					.createUser()
					.setEmail(email)
					.setGenerateGmcId(true)
					.setPassword(null)
					.setUsername(uid);
			userCreate.execute().onSuccess(promise::complete).onFailure(promise::fail);
		}
	}

	@Override
	public Future<User> register(final String username, final String email, final String password) {
		return Future.failedFuture(new UnsupportedOperationException("LDAP doesn't support creating an account"));
	}

}

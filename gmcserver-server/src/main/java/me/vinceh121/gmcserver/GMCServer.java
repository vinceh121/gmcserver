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
package me.vinceh121.gmcserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.FileSystemAccess;
import io.vertx.ext.web.handler.StaticHandler;
import me.vinceh121.gmcserver.auth.AbstractAuthenticator;
import me.vinceh121.gmcserver.auth.InternalAuthenticator;
import me.vinceh121.gmcserver.entities.AbstractEntity;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.event.EntityCodec;
import me.vinceh121.gmcserver.handlers.APIHandler;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.handlers.CorsHandler;
import me.vinceh121.gmcserver.handlers.StrictAuthHandler;
import me.vinceh121.gmcserver.managers.AlertManager;
import me.vinceh121.gmcserver.managers.DeviceCalendarManager;
import me.vinceh121.gmcserver.managers.DeviceManager;
import me.vinceh121.gmcserver.managers.ImportManager;
import me.vinceh121.gmcserver.managers.LoggingManager;
import me.vinceh121.gmcserver.managers.ProxyManager;
import me.vinceh121.gmcserver.managers.UserManager;
import me.vinceh121.gmcserver.managers.email.EmailManager;
import me.vinceh121.gmcserver.mfa.MFAManager;
import me.vinceh121.gmcserver.modules.AbstractModule;
import me.vinceh121.gmcserver.modules.AdminModule;
import me.vinceh121.gmcserver.modules.AuthModule;
import me.vinceh121.gmcserver.modules.CaptchaModule;
import me.vinceh121.gmcserver.modules.DeviceModule;
import me.vinceh121.gmcserver.modules.GeoModule;
import me.vinceh121.gmcserver.modules.ImportExportModule;
import me.vinceh121.gmcserver.modules.InstanceModule;
import me.vinceh121.gmcserver.modules.LoggingModule;
import me.vinceh121.gmcserver.modules.UserModule;
import xyz.bowser65.tokenize.Tokenize;

public class GMCServer {
	private static final Logger LOG = LogManager.getLogger(GMCServer.class);
	private final Properties config = new Properties();
	private final InstanceInfo instanceInfo = new InstanceInfo();

	private final Vertx vertx;

	private final HttpServer srv;
	private final Router baseRouter, apiRouter;
	private final HttpClient httpClient;
	private final WebClient webClient;

	private final Tokenize tokenize;
	private final Argon2 argon;

	private final BodyHandler bodyHandler;
	private final CorsHandler corsHandler;
	private final APIHandler apiHandler;
	private final AuthHandler authHandler;
	private final StrictAuthHandler strictAuthHandler;

	private final AbstractAuthenticator authenticator;

	private final Collection<AbstractModule> modules = new ArrayList<>();

	//// Managers
	private DatabaseManager databaseManager;
	private MFAManager mfaManager;
	private UserManager userManager;
	private DeviceManager deviceManager;
	private EmailManager emailManager;
	private AlertManager alertManager;
	private DeviceCalendarManager deviceCalendarManager;
	private LoggingManager loggingManager;
	private ProxyManager proxyManager;
	private ImportManager importManager;

	public static void main(final String[] args) {
		GMCServer.LOG.info("Build options:\n{}", GMCBuild.buildOptions());
		final GMCServer srv = new GMCServer();
		srv.start();
	}

	public GMCServer() {
		try (final FileInputStream configInput = new FileInputStream(GMCBuild.CONFIG_PATH)) {
			this.config.load(configInput);
		} catch (final IOException e) {
			GMCServer.LOG.error("Failed to load config", e);
			System.exit(-1);
		}

		for (String key : System.getenv().keySet()) {
			if (key.startsWith("GMC_")) {
				final String configKey = key.substring(4).toLowerCase().replaceAll(Pattern.quote("_"), ".");
				final String val = System.getenv(key);
				this.config.setProperty(configKey, val);
				LOG.info("Overriding from envvar {}={}", key, val);
			}
		}

		this.instanceInfo.fromProperties(this.config);

		byte[] secret;
		try {
			final String strSecret = this.config.getProperty("auth.secret");
			if (strSecret == null || strSecret.isEmpty()) {
				throw new DecoderException();
			}
			secret = Hex.decodeHex(strSecret);
		} catch (final DecoderException e) {
			GMCServer.LOG.error("Could not decode secret, generating random one", e);
			secret = new byte[1024];
			new SecureRandom().nextBytes(secret);
			this.config.setProperty("auth.secret", Hex.encodeHexString(secret));
			GMCServer.LOG.warn("New temporary secret is {}", this.config.getProperty("auth.secret"));
			GMCServer.LOG.warn("Please set the secret in the config file");
		}

		this.tokenize = new Tokenize(secret);
		this.argon = Argon2Factory.create();

		try {
			this.authenticator = (AbstractAuthenticator) Class
				.forName(this.config.getProperty("auth.authenticator", InternalAuthenticator.class.getCanonicalName()))
				.getConstructor(GMCServer.class)
				.newInstance(this);
		} catch (final Exception e) {
			GMCServer.LOG.error("Failed to initiate authenticator", e);
			throw new IllegalStateException(e);
		}

		final VertxOptions options;
		try {
			options = new VertxOptions(
					new JsonObject(new String(Files.readAllBytes(Paths.get(GMCBuild.VERTX_CONFIG_PATH)))));
		} catch (final IOException e) {
			GMCServer.LOG.error("Failed to read vertx config", e);
			System.exit(-2);
			throw new IllegalStateException(e);
		}

		this.vertx = Vertx.vertx(options);
		this.srv = this.vertx.createHttpServer();
		this.srv.exceptionHandler(t -> GMCServer.LOG.error("Unexpected error", t));

		this.setupEventBusCodecs();

		this.registerManagers();

		this.baseRouter = Router.router(this.vertx);
		this.baseRouter.errorHandler(500, ctx -> GMCServer.LOG.error("Unexpected HTTP error", ctx.failure()));
		this.srv.requestHandler(this.baseRouter);

		this.apiRouter = Router.router(this.vertx);
		this.baseRouter.route("/api/v1/*").subRouter(this.apiRouter);
		this.apiRouter.errorHandler(500, ctx -> GMCServer.LOG.error("Unexpected error in API", ctx.failure()));

		this.bodyHandler = BodyHandler.create();
		this.apiHandler = new APIHandler();
		this.authHandler = new AuthHandler(this);
		this.strictAuthHandler = new StrictAuthHandler();
		this.corsHandler = new CorsHandler(this.getConfig().getProperty("cors.web-host"));

		this.apiRouter.route().handler(this.corsHandler);

		HttpClientOptions httpOpts = new HttpClientOptions();
		httpOpts.setSsl(true);
		this.httpClient = this.vertx.createHttpClient(httpOpts);

		final WebClientOptions webOpts = new WebClientOptions();
		webOpts.setUserAgent("GMCServer/" + GMCBuild.VERSION + " (Vert.x Web Client) - https://home.gmc.vinceh121.me");
		this.webClient = WebClient.create(this.vertx, webOpts);

		this.registerModules();

		if (Boolean.parseBoolean(this.config.getProperty("web.enabled"))) {
			this.setupWebRouter(this.vertx);
		}
	}

	private void setupWebRouter(final Vertx vertx) {
		GMCServer.LOG.info("Starting web server");
		final Router webRouter = Router.router(vertx);
		final StaticHandler webHandler = StaticHandler.create(FileSystemAccess.ROOT,
				this.config.getProperty("web.root"));
		webHandler.setIncludeHidden(false);
		webRouter.route().handler(webHandler).handler(ctx -> ctx.reroute("/"));
		this.baseRouter.route("/*").subRouter(webRouter);
	}

	private void setupEventBusCodecs() {
		this.setupEventBusCodecClass(Record.class);
		this.setupEventBusCodecClass(User.class);
		this.setupEventBusCodecClass(Device.class);
	}

	private <T extends AbstractEntity> void setupEventBusCodecClass(final Class<T> clazz) {
		this.getEventBus().registerDefaultCodec(clazz, new EntityCodec<>(clazz));
	}

	private void registerManagers() {
		this.databaseManager = new DatabaseManager(this);
		this.mfaManager = new MFAManager(this);
		this.userManager = new UserManager(this);
		this.deviceManager = new DeviceManager(this);
		this.emailManager = new EmailManager(this);
		this.alertManager = new AlertManager(this);
		this.deviceCalendarManager = new DeviceCalendarManager(this);
		this.loggingManager = new LoggingManager(this);
		this.proxyManager = new ProxyManager(this);
		this.importManager = new ImportManager(this);
	}

	private void registerModules() {
		this.modules.addAll(Arrays.asList(new LoggingModule(this),
				new DeviceModule(this),
				new AuthModule(this),
				new GeoModule(this),
				new UserModule(this),
				new ImportExportModule(this),
				new InstanceModule(this),
				new AdminModule(this),
				new CaptchaModule(this)));
	}

	public void start() {
		final String host = this.config.getProperty("server.host", "127.0.0.1");
		this.srv.listen(Integer.parseInt(this.config.getProperty("server.port")), host).onSuccess(s -> {
			GMCServer.LOG.info("Listening on {}:{}", host, this.srv.actualPort());
		}).onFailure(t -> {
			GMCServer.LOG.error(
					new FormattedMessage("Failed to listen on {}:{}", host, this.config.getProperty("server.port")),
					t);
		});
	}

	public Router getBaseRouter() {
		return this.baseRouter;
	}

	public Router getApiRouter() {
		return this.apiRouter;
	}

	public BodyHandler getBodyHandler() {
		return this.bodyHandler;
	}

	public APIHandler getApiHandler() {
		return this.apiHandler;
	}

	public AuthHandler getAuthHandler() {
		return this.authHandler;
	}

	public StrictAuthHandler getStrictAuthHandler() {
		return this.strictAuthHandler;
	}

	public Tokenize getTokenize() {
		return this.tokenize;
	}

	public Argon2 getArgon() {
		return this.argon;
	}

	public Properties getConfig() {
		return this.config;
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

	public WebClient getWebClient() {
		return this.webClient;
	}

	public Vertx getVertx() {
		return this.vertx;
	}

	public InstanceInfo getInstanceInfo() {
		return this.instanceInfo;
	}

	public EventBus getEventBus() {
		return this.getVertx().eventBus();
	}

	public AbstractAuthenticator getAuthenticator() {
		return this.authenticator;
	}

	public Collection<AbstractModule> getModules() {
		return this.modules;
	}

	public DatabaseManager getDatabaseManager() {
		return this.databaseManager;
	}

	public MFAManager getMfaManager() {
		return this.mfaManager;
	}

	public UserManager getUserManager() {
		return this.userManager;
	}

	public DeviceManager getDeviceManager() {
		return this.deviceManager;
	}

	public EmailManager getEmailManager() {
		return this.emailManager;
	}

	public AlertManager getAlertManager() {
		return this.alertManager;
	}

	public DeviceCalendarManager getDeviceCalendarManager() {
		return this.deviceCalendarManager;
	}

	public LoggingManager getLoggingManager() {
		return this.loggingManager;
	}

	public ProxyManager getProxyManager() {
		return this.proxyManager;
	}

	public ImportManager getImportManager() {
		return importManager;
	}
}

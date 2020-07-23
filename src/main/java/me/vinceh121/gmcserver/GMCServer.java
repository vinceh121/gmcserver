package me.vinceh121.gmcserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Properties;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import me.vinceh121.gmcserver.event.WebsocketManager;
import me.vinceh121.gmcserver.handlers.APIHandler;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.handlers.CorsHandler;
import me.vinceh121.gmcserver.handlers.StrictAuthHandler;
import me.vinceh121.gmcserver.handlers.WebHandler;
import me.vinceh121.gmcserver.mfa.MFAManager;
import me.vinceh121.gmcserver.modules.AuthModule;
import me.vinceh121.gmcserver.modules.DeviceModule;
import me.vinceh121.gmcserver.modules.GeoModule;
import me.vinceh121.gmcserver.modules.LoggingModule;
import me.vinceh121.gmcserver.modules.UserModule;
import xyz.bowser65.tokenize.Tokenize;

public class GMCServer {
	private static final Logger LOG = LoggerFactory.getLogger(GMCServer.class);
	private final Properties config = new Properties();

	private final HttpServer srv;
	private final Router baseRouter, apiRouter;

	private final WebsocketManager wsManager;
	private final MFAManager mfaManager;
	private final DatabaseManager databaseManager;

	private final Tokenize tokenize;
	private final Argon2 argon;

	private final BodyHandler bodyHandler;
	private final CorsHandler corsHandler;
	private final APIHandler apiHandler;
	private final AuthHandler authHandler;
	private final StrictAuthHandler strictAuthHandler;

	public static void main(final String[] args) {
		LOG.debug("Build options:\n" + GMCBuild.buildOptions());
		final GMCServer srv = new GMCServer();
		srv.start();
	}

	public GMCServer() {
		try {
			final FileInputStream configInput = new FileInputStream(GMCBuild.CONFIG_PATH);
			this.config.load(configInput);
			configInput.close();
		} catch (final IOException e) {
			GMCServer.LOG.error("Failed to load config", e);
			System.exit(-1);
		}

		this.databaseManager = new DatabaseManager(this);

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

		final VertxOptions options;
		try {
			options = new VertxOptions(
					new JsonObject(new String(Files.readAllBytes(Paths.get(GMCBuild.VERTX_CONFIG_PATH)))));
		} catch (final IOException e) {
			LOG.error("Failed to read vertx config", e);
			System.exit(-2);
			throw new IllegalStateException(e);
		}

		final Vertx vertx = Vertx.factory.vertx(options);
		this.srv = vertx.createHttpServer();
		this.srv.exceptionHandler(t -> GMCServer.LOG.error("Unexpected error: {}", t));

		this.baseRouter = Router.router(vertx);
		this.srv.requestHandler(this.baseRouter);

		this.apiRouter = Router.router(vertx);
		this.baseRouter.mountSubRouter("/api/v1/", this.apiRouter);

		this.bodyHandler = BodyHandler.create();
		this.apiHandler = new APIHandler();
		this.authHandler = new AuthHandler(this);
		this.strictAuthHandler = new StrictAuthHandler();
		this.corsHandler = new CorsHandler(this.getConfig().getProperty("cors.web-host"));

		this.apiRouter.route().handler(this.corsHandler);

		this.registerModules();

		this.wsManager = new WebsocketManager(this);
		this.srv.webSocketHandler(this.wsManager);

		this.mfaManager = new MFAManager(this);

		if (Boolean.parseBoolean(this.config.getProperty("web.enabled"))) {
			this.setupWebRouter(vertx);
		}
	}

	private void setupWebRouter(final Vertx vertx) {
		GMCServer.LOG.info("Starting web server");
		final Router webRouter = Router.router(vertx);
		final WebHandler webHandler = new WebHandler(Paths.get(this.config.getProperty("web.root")));
		webRouter.route().handler(webHandler);
		this.baseRouter.mountSubRouter("/", webRouter);
	}

	private void registerModules() {
		new LoggingModule(this);
		new DeviceModule(this);
		new AuthModule(this);
		new GeoModule(this);
		new UserModule(this);
	}

	public void start() {
		this.srv.listen(Integer.parseInt(this.config.getProperty("server.port")));
		GMCServer.LOG.info("Listening on port {}", this.srv.actualPort());
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

	public WebsocketManager getWebsocketManager() {
		return this.wsManager;
	}

	public MFAManager getMfaManager() {
		return this.mfaManager;
	}

	public DatabaseManager getDatabaseManager() {
		return this.databaseManager;
	}

	public Properties getConfig() {
		return this.config;
	}

}

package me.vinceh121.gmcserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Random;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.event.WebsocketManager;
import me.vinceh121.gmcserver.handlers.APIHandler;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.handlers.StrictAuthHandler;
import me.vinceh121.gmcserver.modules.AuthModule;
import me.vinceh121.gmcserver.modules.DeviceModule;
import me.vinceh121.gmcserver.modules.GeoModule;
import me.vinceh121.gmcserver.modules.LoggingModule;
import xyz.bowser65.tokenize.Tokenize;

public class GMCServer {
	private static final Logger LOG = LoggerFactory.getLogger(GMCServer.class);
	public static final String ERROR_USER_ID = "Invalid user ID (AID)";
	public static final String ERROR_DEVICE_ID = "Invalid device ID (GID)";
	public static final String ERROR_DEVICE_NOT_OWNED = "User does not own device";
	public static final String CONFIG_PATH = "./config.properties";
	private final Properties config = new Properties();
	private final PojoCodecProvider pojoCodecProvider;
	private final CodecRegistry codecRegistry;
	private final MongoClient mongoClient;
	private final MongoDatabase mongoDb;
	private final MongoCollection<Record> colRecords;
	private final MongoCollection<User> colUsers;
	private final MongoCollection<Device> colDevices;
	private final HttpServer srv;
	private final Router router;

	private final WebsocketManager wsManager;

	private final Tokenize tokenize;
	private final Argon2 argon;

	private final BodyHandler bodyHandler;
	private final APIHandler apiHandler;
	private final AuthHandler authHandler;
	private final StrictAuthHandler strictAuthHandler;

	public static void main(final String[] args) {
		final GMCServer srv = new GMCServer();
		srv.start();
	}

	public GMCServer() {
		try {
			this.config.load(new FileInputStream(GMCServer.CONFIG_PATH));
		} catch (final IOException e) {
			GMCServer.LOG.error("Failed to load config", e);
			System.exit(-1);
		}

		this.pojoCodecProvider = PojoCodecProvider.builder()
				.automatic(true)
				.conventions(Arrays.asList(classModelBuilder -> classModelBuilder.enableDiscriminator(true),
						Conventions.ANNOTATION_CONVENTION,
						Conventions.CLASS_AND_PROPERTY_CONVENTION))
				.build();
		this.codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
				CodecRegistries.fromProviders(this.pojoCodecProvider, new GeoJsonCodecProvider()));
		final MongoClientSettings set = MongoClientSettings.builder()
				.applicationName("GMCServer")
				.applyConnectionString(new ConnectionString(this.config.getProperty("mongo.constring")))
				.codecRegistry(this.codecRegistry)
				.build();

		this.mongoClient = MongoClients.create(set);
		this.mongoDb = this.mongoClient.getDatabase("gmcserver");

		this.colRecords = this.mongoDb.getCollection("records", Record.class);
		this.colUsers = this.mongoDb.getCollection("users", User.class);
		this.colDevices = this.mongoDb.getCollection("devices", Device.class);

		this.checkIndexes();

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
			new Random().nextBytes(secret);
			this.config.setProperty("auth.secret", Hex.encodeHexString(secret));
			GMCServer.LOG.warn("New temporary secret is {}", this.config.getProperty("auth.secret"));
		}

		this.tokenize = new Tokenize(secret);
		this.argon = Argon2Factory.create();

		final VertxOptions options = new VertxOptions();
		options.setBlockedThreadCheckInterval(1000 * 60 * 60); // XXX debug

		final Vertx vertx = Vertx.factory.vertx(options);
		this.srv = vertx.createHttpServer();
		this.srv.exceptionHandler(t -> GMCServer.LOG.error("Unexpected error: {}", t));

		this.router = Router.router(vertx);
		this.srv.requestHandler(this.router);

		this.router.route().handler(ctx -> { // XXX debug
			// and i swear if you forget to remove this one i'll slap you across the ocean
			ctx.response().putHeader("Access-Control-Allow-Origin", "*");
			if (ctx.request().method().equals(HttpMethod.OPTIONS)) {
				ctx.response().putHeader("Access-Control-Request-Method", "POST, GET, OPTIONS");
				ctx.response().putHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
				ctx.response().setStatusCode(204).end();
			} else {
				ctx.next();
			}
		});

		this.bodyHandler = BodyHandler.create();
		this.apiHandler = new APIHandler();
		this.authHandler = new AuthHandler(this);
		this.strictAuthHandler = new StrictAuthHandler();

		this.registerModules();

		this.wsManager = new WebsocketManager(this);
		this.srv.webSocketHandler(this.wsManager);
	}

	private void checkIndexes() {
		int deviceIndexCount = 0;
		final MongoCursor<Document> cur = this.colDevices.listIndexes().iterator();

		while (cur.hasNext()) {
			cur.next();
			deviceIndexCount++;
		}

		if (deviceIndexCount <= 1) {
			LOG.warn("Device collection does not have index, generating");
			this.colDevices.createIndex(Indexes.geo2dsphere("location"));
		}
	}

	private void registerModules() {
		new LoggingModule(this);
		new DeviceModule(this);
		new AuthModule(this);
		new GeoModule(this);
	}

	public void start() {
		this.srv.listen(Integer.parseInt(this.config.getProperty("server.port")));
		GMCServer.LOG.info("Listening on port {}", this.srv.actualPort());
	}

	public Router getRouter() {
		return this.router;
	}

	public MongoCollection<Device> getColDevices() {
		return this.colDevices;
	}

	public MongoCollection<Record> getColRecords() {
		return this.colRecords;
	}

	public MongoCollection<User> getColUsers() {
		return this.colUsers;
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

	public Properties getConfig() {
		return this.config;
	}

}

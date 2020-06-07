package me.vinceh121.gmcserver;

import java.util.Arrays;
import java.util.Random;

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
import com.mongodb.client.MongoDatabase;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.handlers.APIHandler;
import me.vinceh121.gmcserver.handlers.AuthHandler;
import me.vinceh121.gmcserver.handlers.StrictAuthHandler;
import me.vinceh121.gmcserver.modules.AuthModule;
import me.vinceh121.gmcserver.modules.DeviceModule;
import me.vinceh121.gmcserver.modules.LoggingModule;
import xyz.bowser65.tokenize.Tokenize;

public class GMCServer {
	private static final Logger LOG = LoggerFactory.getLogger(GMCServer.class);
	public static final String ERROR_USER_ID = "Invalid user ID (AID)";
	public static final String ERROR_DEVICE_ID = "Invalid device ID (GID)";
	public static final String ERROR_DEVICE_NOT_OWNED = "User does not own device";
	private final PojoCodecProvider pojoCodecProvider;
	private final CodecRegistry codecRegistry;
	private final MongoClient mongoClient;
	private final MongoDatabase mongoDb;
	private final MongoCollection<Record> colRecords;
	private final MongoCollection<User> colUsers;
	private final MongoCollection<Device> colDevices;
	private final HttpServer srv;
	private final Router router;

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
		this.pojoCodecProvider = PojoCodecProvider.builder()
				.automatic(true)
				.conventions(Arrays.asList(classModelBuilder -> classModelBuilder.enableDiscriminator(true),
						Conventions.ANNOTATION_CONVENTION,
						Conventions.CLASS_AND_PROPERTY_CONVENTION))
				.build();
		this.codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
				CodecRegistries.fromProviders(this.pojoCodecProvider));
		final MongoClientSettings set = MongoClientSettings.builder()
				.applicationName("GMCServer")
				.applyConnectionString(new ConnectionString("mongodb://localhost"))
				.codecRegistry(this.codecRegistry)
				.build();

		this.mongoClient = MongoClients.create(set);
		this.mongoDb = this.mongoClient.getDatabase("gmcserver");
		this.colRecords = this.mongoDb.getCollection("records", Record.class);
		this.colUsers = this.mongoDb.getCollection("users", User.class);
		this.colDevices = this.mongoDb.getCollection("devices", Device.class);

		final byte[] secret = new byte[1024];

		new Random().nextBytes(secret);

		this.tokenize = new Tokenize(secret);
		this.argon = Argon2Factory.create();

		final VertxOptions options = new VertxOptions();
		options.setBlockedThreadCheckInterval(1000 * 60 * 60); // XXX debug

		final Vertx vertx = Vertx.factory.vertx(options);
		this.srv = vertx.createHttpServer();
		this.srv.exceptionHandler(t -> {
			GMCServer.LOG.error("Unexpected error: {}", t);
		});

		this.router = Router.router(vertx);
		this.srv.requestHandler(this.router);

		this.bodyHandler = BodyHandler.create();
		this.apiHandler = new APIHandler();
		this.authHandler = new AuthHandler(this);
		this.strictAuthHandler = new StrictAuthHandler();

		this.registerModules();
	}

	private void registerModules() {
		new LoggingModule(this);
		new DeviceModule(this);
		new AuthModule(this);
	}

	public void start() {
		this.srv.listen(80);
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
		return bodyHandler;
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

}

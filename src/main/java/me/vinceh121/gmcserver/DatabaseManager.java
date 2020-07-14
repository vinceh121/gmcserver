package me.vinceh121.gmcserver;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Objects;

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

import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;

public class DatabaseManager {
	private static Logger LOG = LoggerFactory.getLogger(DatabaseManager.class);
	private final GMCServer srv;
	private final MongoClient client;
	private final MongoDatabase database;
	private final PojoCodecProvider pojoCodecProvider;
	private final CodecRegistry codecRegistry;
	private final Hashtable<Class<?>, MongoCollection<?>> collections;

	public DatabaseManager(final GMCServer srv) {
		this.srv = srv;
		this.pojoCodecProvider = PojoCodecProvider.builder()
				.automatic(true)
				.conventions(Arrays.asList(classModelBuilder -> classModelBuilder.enableDiscriminator(true),
						Conventions.ANNOTATION_CONVENTION,
						Conventions.CLASS_AND_PROPERTY_CONVENTION,
						Conventions.OBJECT_ID_GENERATORS))
				.build();
		this.codecRegistry = CodecRegistries.fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
				CodecRegistries.fromProviders(this.pojoCodecProvider, new GeoJsonCodecProvider()));
		final MongoClientSettings set = MongoClientSettings.builder()
				.applicationName("GMCServer")
				.applyConnectionString(new ConnectionString(this.srv.getConfig().getProperty("mongo.constring")))
				.codecRegistry(this.codecRegistry)
				.build();

		this.client = MongoClients.create(set);
		this.database = this.client.getDatabase(this.srv.getConfig().getProperty("mongo.database"));

		collections = new Hashtable<>();

		for (final GMCCol col : GMCCol.values()) {
			collections.put(col.getClazz(), database.getCollection(col.getName(), col.getClazz()));
		}
		this.checkIndexes();
	}

	private void checkIndexes() {
		int deviceIndexCount = 0;
		final MongoCursor<Document> cur = this.getCollection(Device.class).listIndexes().iterator();

		while (cur.hasNext()) {
			cur.next();
			deviceIndexCount++;
		}

		if (deviceIndexCount <= 1) {
			LOG.warn("Device collection does not have index, generating");
			this.getCollection(Device.class).createIndex(Indexes.geo2dsphere("location"));
		}
	}

	public MongoDatabase getDatabase() {
		return database;
	}

	public <T> MongoCollection<T> getCollection(final Class<T> clazz) {
		@SuppressWarnings("unchecked")
		final MongoCollection<T> col = (MongoCollection<T>) this.collections.get(clazz);
		Objects.nonNull(col);
		return col;
	}

	private enum GMCCol {
		USERS("users", User.class), DEVICES("devices", Device.class), RECORDS("records", Record.class);

		private final String name;
		private final Class<?> clazz;

		private GMCCol(final String name, final Class<?> clazz) {
			this.name = name;
			this.clazz = clazz;
		}

		public String getName() {
			return name;
		}

		public Class<?> getClazz() {
			return clazz;
		}
	}
}

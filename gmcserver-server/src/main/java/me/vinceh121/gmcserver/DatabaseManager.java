package me.vinceh121.gmcserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.keyvalue.UnmodifiableMapEntry;
import org.bson.Document;
import org.bson.codecs.DoubleCodec;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.codecs.pojo.PropertyModelBuilder;
import org.bson.conversions.Bson;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.geojson.codecs.GeoJsonCodecProvider;

import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.DeviceCalendar;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.managers.AbstractManager;

public class DatabaseManager extends AbstractManager {
	private final MongoClient client;
	private final MongoDatabase database;
	private final PojoCodecProvider pojoCodecProvider;
	private final CodecRegistry codecRegistry;
	private final Hashtable<Class<?>, MongoCollection<?>> collections;

	public DatabaseManager(final GMCServer srv) {
		super(srv);

		final ClassModelBuilder<Record> recordClassModel = ClassModel.builder(Record.class); // spaghet but really
																								// needed and i'm too
																								// lazy to make it
																								// dynamic rn
		for (final String p : Record.STAT_FIELDS) {
			@SuppressWarnings("unchecked")
			final PropertyModelBuilder<Double> propertyModelBuilder = (PropertyModelBuilder<Double>) recordClassModel
				.getProperty(p);
			propertyModelBuilder.codec(new DoubleCodec()).propertySerialization(v -> !Double.isNaN(v));
		}

		this.pojoCodecProvider = PojoCodecProvider.builder()
			.automatic(true)
			.conventions(Arrays.asList(classModelBuilder -> classModelBuilder.enableDiscriminator(true),
					Conventions.ANNOTATION_CONVENTION,
					Conventions.CLASS_AND_PROPERTY_CONVENTION,
					Conventions.OBJECT_ID_GENERATORS))
			.register(recordClassModel.build())
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

		this.collections = new Hashtable<>();

		for (final GMCCol col : GMCCol.values()) {
			this.collections.put(col.getClazz(), this.database.getCollection(col.getName(), col.getClazz()));
		}
		this.checkIndexes();
	}

	private void checkIndexes() {
		// key: collection class
		// value: list of pairs of index document and index options
		final Map<Class<?>, List<Entry<Bson, IndexOptions>>> defaultIndexes = new Hashtable<>();
		defaultIndexes.put(Record.class,
				Arrays.asList(new UnmodifiableMapEntry<>(Indexes.ascending("deviceId", "date"),
						new IndexOptions().name("timeline"))));
		defaultIndexes.put(Device.class,
				Arrays.asList(
						new UnmodifiableMapEntry<>(Indexes.geo2dsphere("location"),
								new IndexOptions().name("map")),
						new UnmodifiableMapEntry<>(Indexes.ascending("ownerId"), new IndexOptions().name("owners"))));
		defaultIndexes.put(DeviceCalendar.class,
				Arrays.asList(new UnmodifiableMapEntry<>(Indexes.ascending("createdAt"),
						new IndexOptions().name("expiery").expireAfter(1L, TimeUnit.DAYS))));

		for (final Entry<Class<?>, List<Entry<Bson, IndexOptions>>> e : defaultIndexes.entrySet()) {
			final MongoCollection<?> coll = this.getCollection(e.getKey());
			final List<String> indexNames = new ArrayList<>();
			for (final Document d : coll.listIndexes()) {
				indexNames.add(d.getString("name"));
			}
			for (final Entry<Bson, IndexOptions> indexOpts : e.getValue()) {
				final String name = indexOpts.getValue().getName();
				if (!indexNames.contains(name)) {
					this.log.warn("Creating index {} on collection {}", name, e.getKey().getSimpleName());
					coll.createIndex(indexOpts.getKey(), indexOpts.getValue());
				}
			}
		}
	}

	public MongoDatabase getDatabase() {
		return this.database;
	}

	public <T> MongoCollection<T> getCollection(final Class<T> clazz) {
		@SuppressWarnings("unchecked")
		final MongoCollection<T> col = (MongoCollection<T>) this.collections.get(clazz);
		Objects.nonNull(col);
		return col;
	}

	private enum GMCCol {
		USERS("users", User.class), DEVICES("devices", Device.class), RECORDS("records", Record.class),
		CALENDARS("calendars", DeviceCalendar.class);

		private final String name;
		private final Class<?> clazz;

		GMCCol(final String name, final Class<?> clazz) {
			this.name = name;
			this.clazz = clazz;
		}

		public String getName() {
			return this.name;
		}

		public Class<?> getClazz() {
			return this.clazz;
		}
	}
}

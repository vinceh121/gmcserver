package gmcserver;

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import io.vertx.core.MultiMap;
import me.vinceh121.gmcserver.entities.Record;

class TestRecord {

	@Test
	void testBuild() {
		final MultiMap map = MultiMap.caseInsensitiveMultiMap();
		map.add("cpm", "12345.12345");
		map.add("acpm", "54321.54321");
		map.add("usv", "123.123");
		map.add("accy", "0.123");

		map.add("lat", "1.123");
		map.add("lon", "3.21");
		map.add("alt", "350.5");

		final Record rec = new Record.Builder(map).buildPosition().buildParameters().build();

		Assertions.assertEquals(12345.12345D, rec.getCpm(), "cpm");
		Assertions.assertEquals(54321.54321D, rec.getAcpm(), "acpm");
		Assertions.assertEquals(123.123D, rec.getUsv(), "usv");
		Assertions.assertEquals(0.123D, rec.getAccy(), "accy");

		final Position expectedPos = new Position(3.21, 1.123, 350.5);
		final Point expectedPoint = new Point(expectedPos);

		Assertions.assertEquals(expectedPoint, rec.getLocation(), "location");
	}
	
	@Test
	void toURadMonitor() {
		final Record r = new Record();
		r.setDate(new Date(123456789L));
		r.setCpm(25);
		r.setHcho(0.1D);
		r.setCo2(0.5D);
		
		final String actual = r.toURadMonitorUrl();
		final String expected = "/01/123456/07/0.5/08/0.1/0B/25.0";
		
		Assertions.assertEquals(expected, actual);
	}

}

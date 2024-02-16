package gmcserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import me.vinceh121.gmcserver.entities.Record;

class TestMapping {
	@Test
	void testRecord() {
		assertEquals(
				"#{id},#{deviceId},#{cpm},#{acpm},#{usv},#{co2},#{hcho},#{tmp},#{ap},#{hmdt},#{accy},#{date},#{ip},#{type},#{location}",
				Record.sqlFields());
	}
}

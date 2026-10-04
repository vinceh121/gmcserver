package me.vinceh121.gmcserver.migration;

import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.questdb.client.Sender;

public class QuestDbMigrator {
	public static void main(String[] args) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		try (Sender sender = Sender.fromConfig("http::addr=localhost:9001;");
				FileReader fr = new FileReader("/home/vincent/dump/gmcserver/records.json");
				MappingIterator<ObjectNode> iter = mapper.readerFor(ObjectNode.class).readValues(fr)) {

			for (ObjectNode node : iter.readAll()) {
				sender.table("records");
				
				sender.symbol("deviceId", node.get("deviceId").get("$oid").asText());
				
				if (node.has("ip")) sender.symbol("ip", node.get("ip").asText());

				node.fieldNames().forEachRemaining(s -> {
					JsonNode val = node.get(s);

					if (val.isNumber()) {
						sender.doubleColumn(s, val.asDouble());
					}
				});

				sender.at(Instant.parse(node.get("date").get("$date").asText()));
			}
		}
	}
}

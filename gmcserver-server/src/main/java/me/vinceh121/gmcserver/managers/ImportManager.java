/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package me.vinceh121.gmcserver.managers;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.logging.log4j.message.FormattedMessage;
import org.bson.types.ObjectId;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.modules.ImportExportModule;

public class ImportManager extends AbstractManager {

	public ImportManager(final GMCServer srv) {
		super(srv);
	}

	public ImportGmcmap importGmcmap() {
		return new ImportGmcmap(this.srv);
	}

	public class ImportGmcmap extends AbstractAction<Void> {
		private ObjectId deviceId;
		private String gmcmapId;

		public ImportGmcmap(GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(Promise<Void> promise) {
			this.getRecords(0).onSuccess(recs -> {
				if (recs.size() == 0) {
					promise.fail(new IllegalStateException("Returned data table is empty"));
					return;
				}

				this.srv.getDatabaseManager().getCollection(Record.class).insertMany(recs);

				promise.complete();

				this.importPageRecurse(1);
			}).onFailure(promise::fail);
		}

		private void importPageRecurse(final int page) {
			this.getRecords(page).onSuccess(recs -> {
				log.info("Got {} records from device import {}, page {}", recs.size(), deviceId, page);

				if (recs.size() != 0) {
					this.srv.getDatabaseManager().getCollection(Record.class).insertMany(recs);
					this.importPageRecurse(page + 1);
				} else {
					log.info("Finished import for {}", gmcmapId);
				}
			}).onFailure(t -> {
				log.error(new FormattedMessage("Error while importing device {} at page {}", gmcmapId, page), t);
			});
		}

		private Future<List<Record>> getRecords(final int page) {
			return Future.future(p -> {
				this.srv.getWebClient()
					.get(ImportExportModule.GMCMAP_HOST, ImportExportModule.GMCMAP_HISTORY_URI)
					.setQueryParam("param_ID", gmcmapId)
					.setQueryParam("curpage", String.valueOf(page))
					.setQueryParam("systemTimeZone", "0")
					.send(a -> {
						if (a.failed()) {
							p.fail("Http request failed: " + a.cause().getMessage());
							return;
						}
						try {

							final HttpResponse<Buffer> res = a.result();
							final Document doc = Jsoup.parse(res.bodyAsString());
							final Element table = doc.getElementsByTag("table").first();

							if (table == null) {
								p.fail("No data table found");
								return;
							}

							final Map<String, Integer> colIndexes = new Hashtable<>();
							final Element thead = table.children().select("thead").first().selectFirst("tr");
							for (int i = 0; i < thead.childrenSize(); i++) {
								final Element c = thead.child(i);
								if (!c.tagName().equals("th")) {
									continue;
								}
								if (c.text().startsWith("Date (")) {
									c.text("Date");
								}

								colIndexes.put(c.text(), i);
							}

							final List<Record> records = new Vector<>();

							for (final Element c : table.children()) {
								if (!c.tagName().equals("tbody")) {
									continue;
								}

								final Element tr = c.getElementsByTag("tr").first();

								// Date, CPM, ACPM and uSv are guaranteed to be present
								final Element elmDate = tr.child(colIndexes.get("Date"));
								final Element elmCpm = tr.child(colIndexes.get("CPM"));
								final Element elmAcpm = tr.child(colIndexes.get("ACPM"));
								final Element elmUsv = tr.child(colIndexes.get("uSv/h"));

								Element elmLon = null;
								if (colIndexes.containsKey("Longitude")) {
									elmLon = tr.child(colIndexes.get("Longitude"));
								}

								Element elmLat = null;
								if (colIndexes.containsKey("Latitude")) {
									elmLat = tr.child(colIndexes.get("Latitude"));
								}

								Element elmAlt = null;
								if (colIndexes.containsKey("Altitude")) {
									elmAlt = tr.child(colIndexes.get("Altitude"));
								}

								Element elmCO2 = null;
								if (colIndexes.containsKey("CO2")) {
									elmCO2 = tr.child(colIndexes.get("CO2"));
								}

								Element elmHCHO = null;
								if (colIndexes.containsKey("HCHO")) {
									elmHCHO = tr.child(colIndexes.get("HCHO"));
								}

								Element elmTemperature = null;
								if (colIndexes.containsKey("Temperature")) {
									elmTemperature = tr.child(colIndexes.get("Temperature"));
								}

								Element elmHumidity = null;
								if (colIndexes.containsKey("Humidity")) {
									elmHumidity = tr.child(colIndexes.get("Humidity"));
								}

								final Record r = new Record();
								r.setDeviceId(deviceId);

								try {
									r.setDate(ImportExportModule.GMCMAP_DATE_FMT.parse(elmDate.text()));
								} catch (final ParseException e) {
									log.error("Error while parsing date for record during import", e);
									return;
								}

								if (!elmCpm.text().isEmpty()) {
									r.setCpm(Double.parseDouble(elmCpm.text()));
								}
								if (!elmAcpm.text().isEmpty()) { // for some reason this wasn't a problem before
									r.setAcpm(Double.parseDouble(elmAcpm.text()));
								}
								if (!elmUsv.text().isEmpty()) {
									r.setUsv(Double.parseDouble(elmUsv.text()));
								}

								final List<Double> pos = new ArrayList<>(3);
								for (int i = 0; i < 3; i++) {
									pos.add(Double.NaN);
								}

								if (elmLon != null) {
									pos.set(0, Double.parseDouble(elmLon.text()));
								}

								if (elmLat != null) {
									pos.set(1, Double.parseDouble(elmLat.text()));
								}

								if (elmAlt != null) {
									pos.set(2, Double.parseDouble(elmAlt.text()));
								}

								if (pos.size() > 0) {
									r.setLocation(new Point(new Position(pos)));
								}

								if (elmCO2 != null) {
									r.setCo2(Double.parseDouble(elmCO2.text()));
								}

								if (elmHCHO != null) {
									r.setHcho(Double.parseDouble(elmHCHO.text()));
								}

								if (elmTemperature != null) {
									r.setTmp(Double.parseDouble(elmTemperature.text()));
								}

								if (elmHumidity != null) {
									r.setHmdt(Double.parseDouble(elmHumidity.text()));
								}

								records.add(r);
							}
							p.complete(records);
						} catch (final NumberFormatException e) {
							p.fail(new RuntimeException("Failed to import device", e));
						} catch (final Exception e) {
							log.error(
									new FormattedMessage("ohno oopsie fucky wucky with import {} at page {}",
											gmcmapId,
											page),
									e);
						}
					});
			});
		}

		public ObjectId getDeviceId() {
			return deviceId;
		}

		public ImportGmcmap setDeviceId(ObjectId deviceId) {
			this.deviceId = deviceId;
			return this;
		}

		public String getGmcmapId() {
			return gmcmapId;
		}

		public ImportGmcmap setGmcmapId(String gmcmapId) {
			this.gmcmapId = gmcmapId;
			return this;
		}
	}
}

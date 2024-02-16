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

import java.util.Map;

import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlResult;
import io.vertx.sqlclient.templates.SqlTemplate;
import me.vinceh121.gmcserver.managers.AbstractManager;

public class DatabaseManager extends AbstractManager {
	private final PgPool pool;

	public DatabaseManager(final GMCServer srv) {
		super(srv);
		final PgConnectOptions optsConfig;
		if (srv.getConfig().contains("db.uri")) {
			optsConfig = PgConnectOptions.fromUri(srv.getConfig().getProperty("db.uri"));
		} else {
			optsConfig = new PgConnectOptions();
		}

		final PgConnectOptions optsEnv = PgConnectOptions.fromEnv();
		final PgConnectOptions optsEffective = optsConfig.merge(optsEnv.toJson());

		final PoolOptions poolOpts = new PoolOptions();
		poolOpts.setMaxSize(5);

		this.pool = PgPool.pool(srv.getVertx(), optsEffective, poolOpts);

		this.checkIndexes();
	}

	// TODO
	private void checkIndexes() {
	}

	public PgPool getPool() {
		return this.pool;
	}

	public SqlTemplate<Map<String, Object>, RowSet<Row>> query(String query) {
		return SqlTemplate.forQuery(this.getPool(), query);
	}

	public SqlTemplate<Map<String, Object>, SqlResult<Void>> update(String query) {
		return SqlTemplate.forUpdate(this.getPool(), query);
	}
}

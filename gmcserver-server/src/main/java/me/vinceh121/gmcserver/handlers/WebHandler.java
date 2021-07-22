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
package me.vinceh121.gmcserver.handlers;

import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.FormattedMessage;

import io.vertx.core.Handler;
import io.vertx.core.file.FileSystem;
import io.vertx.ext.web.RoutingContext;

public class WebHandler implements Handler<RoutingContext> {
	private static final Logger LOG = LogManager.getLogger(WebHandler.class);
	private final FileSystem fs;
	private final Path webRoot;
	private final String indexFile;

	public WebHandler(final Path webRoot, final FileSystem fs) {
		this.fs = fs;
		this.webRoot = webRoot;
		this.indexFile = webRoot.resolve("index.html").toAbsolutePath().toString();
	}

	@Override
	public void handle(final RoutingContext ctx) {
		final String path = ctx.normalizedPath();
		final Path filePath = this.webRoot.resolve(path.substring(1));
		final String fullPath = filePath.toAbsolutePath().toString();
		this.fs.exists(fullPath, exRes -> {
			if (exRes.result()) {
				this.supplyFile(ctx, fullPath);
			} else {
				this.supplyFile(ctx, this.indexFile);
			}
		});
	}

	private void supplyFile(final RoutingContext ctx, final String fullPath) {
		this.fs.readFile(fullPath, res -> {
			if (res.failed()) {
				WebHandler.LOG.error(new FormattedMessage("Failed to read file {}", fullPath), res.cause());
				ctx.response().setStatusCode(500).end();
				return;
			}

			ctx.response().putHeader("Content-Type", WebHandler.getContentType(fullPath)).end(res.result());
		});
	}

	private static String getContentType(final String file) {
		final String extension = file.substring(file.lastIndexOf('.') + 1);
		switch (extension) {
		case "html":
			return "text/html";
		case "js":
			return "application/javascript";
		case "png":
			return "image/png";
		case "svg":
			return "image/svg+xml";
		case "map":
			return "application/json";
		case "css":
			return "text/css";
		default:
			return "text/plain";
		}
	}
}

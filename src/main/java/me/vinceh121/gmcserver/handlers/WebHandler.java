package me.vinceh121.gmcserver.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;

public class WebHandler implements Handler<RoutingContext> {
	private static final Logger LOG = LoggerFactory.getLogger(WebHandler.class);
	private final static int BUFFER_SIZE = 1024;
	private final Path webRoot;
	private final File indexFile;

	public WebHandler(final Path webRoot) {
		this.webRoot = webRoot;
		this.indexFile = webRoot.resolve("index.html").toFile();
	}

	@Override
	public void handle(final RoutingContext ctx) {
		final String path = ctx.normalisedPath();
		final Path filePath = webRoot.resolve(path.substring(1));
		final File file = filePath.toFile();

		if (!file.exists() || file.isDirectory()) {
			this.supplyFile(ctx, indexFile);
			return;
		}

		supplyFile(ctx, file);
	}

	private void supplyFile(final RoutingContext ctx, final File file) {
		final Buffer buf = Buffer.buffer((int) file.length());

		final FileInputStream fin;
		try {
			fin = new FileInputStream(file);
		} catch (final FileNotFoundException e) { // shouldn't happen because checked previously
			throw new IllegalStateException(e);
		}

		try {
			int available;
			while ((available = fin.available()) != 0) {
				final byte[] inBuf = new byte[available < BUFFER_SIZE ? BUFFER_SIZE : available];
				fin.read(inBuf);
				buf.appendBytes(inBuf);
			}
			fin.close();
		} catch (final IOException e1) {
			LOG.error("Error while reading file ", e1);
		}

		ctx.response().putHeader("Content-Type", getContentType(file)).end(buf);
	}

	private String getContentType(final File file) {
		final String extension = file.getName().substring(file.getName().lastIndexOf('.') + 1);
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

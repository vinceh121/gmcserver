package me.vinceh121.gmcserver.managers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.logging.log4j.message.FormattedMessage;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.proxy.AbstractProxy;
import me.vinceh121.gmcserver.proxy.GmcmapProxy;
import me.vinceh121.gmcserver.proxy.RadmonProxy;

public class ProxyManager extends AbstractManager {

	private final Map<String, AbstractProxy> proxies = new HashMap<>();

	public ProxyManager(final GMCServer srv) {
		super(srv);
		this.registerProxies();
	}

	private void registerProxies() {
		this.registerProxy(new GmcmapProxy(this.srv));
		this.registerProxy(new RadmonProxy(this.srv));
	}

	private void registerProxy(final AbstractProxy p) {
		this.proxies.put(p.getClass().getSimpleName(), p);
	}

	public Map<String, AbstractProxy> getProxies() {
		return proxies;
	}

	public ValidateProxiesSettingsAction validateProxiesSettings() {
		return new ValidateProxiesSettingsAction(srv);
	}

	public ProcessDeviceProxiesAction processDeviceProxies() {
		return new ProcessDeviceProxiesAction(srv);
	}

	public class ValidateProxiesSettingsAction extends AbstractAction<Void> {
		private JsonObject proxiesSettings;

		private ValidateProxiesSettingsAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			if (this.proxiesSettings == null) {
				promise.complete();
				return;
			}
			@SuppressWarnings("rawtypes") // to vertx: why not type this with `<?>` ?
			final List<Future> futures = new Vector<>();
			for (final String key : this.proxiesSettings.getMap().keySet()) {
				final AbstractProxy p = proxies.get(key);
				if (p == null) {
					promise.fail("Unknown proxy '" + key + "'");
					return;
				}
			}
			CompositeFuture.all(futures).onSuccess(f -> promise.complete()).onFailure(promise::fail);
		}

		public JsonObject getProxiesSettings() {
			return proxiesSettings;
		}

		public ValidateProxiesSettingsAction setProxiesSettings(JsonObject proxiesSettings) {
			this.proxiesSettings = proxiesSettings;
			return this;
		}
	}

	public class ProcessDeviceProxiesAction extends AbstractAction<Void> {
		private Device device;
		private Record record;

		private ProcessDeviceProxiesAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			for (final String key : device.getProxiesSettings().keySet()) {
				processProxy(proxies.get(key));
			}
			promise.complete();
		}

		private void processProxy(final AbstractProxy p) {
			p.proxyRecord(record, this.device.getProxiesSettings().get(p.getClass().getSimpleName())).onFailure(t -> {
				log.error(new FormattedMessage("Failed to proxy record {} for {}", record.getId(),
						p.getClass().getSimpleName()), t);
				// TODO save error to report to user
			});
		}

		public Device getDevice() {
			return device;
		}

		public ProcessDeviceProxiesAction setDevice(Device device) {
			this.device = device;
			return this;
		}

		public Record getRecord() {
			return record;
		}

		public ProcessDeviceProxiesAction setRecord(Record record) {
			this.record = record;
			return this;
		}
	}

}

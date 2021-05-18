package me.vinceh121.gmcserver.managers;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.message.FormattedMessage;

import io.vertx.core.Promise;
import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.actions.AbstractAction;
import me.vinceh121.gmcserver.entities.Device;
import me.vinceh121.gmcserver.entities.Record;
import me.vinceh121.gmcserver.proxy.AbstractProxy;
import me.vinceh121.gmcserver.proxy.GmcmapProxy;

public class ProxyManager extends AbstractManager {

	private final Map<String, AbstractProxy> proxies = new HashMap<>();

	public ProxyManager(final GMCServer srv) {
		super(srv);
		this.registerProxies();
	}

	private void registerProxies() {
		this.registerProxy(new GmcmapProxy(this.srv));
	}

	private void registerProxy(final AbstractProxy p) {
		this.proxies.put(p.getClass().getSimpleName(), p);
	}

	public Map<String, AbstractProxy> getProxies() {
		return proxies;
	}

	public ProcessDeviceProxiesAction processDeviceProxies() {
		return new ProcessDeviceProxiesAction(srv);
	}

	public class ProcessDeviceProxiesAction extends AbstractAction<Void> {
		private Device device;
		private Record record;

		public ProcessDeviceProxiesAction(final GMCServer srv) {
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

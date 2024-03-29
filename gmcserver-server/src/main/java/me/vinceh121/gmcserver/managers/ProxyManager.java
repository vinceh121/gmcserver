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
import me.vinceh121.gmcserver.proxy.SafecastProxy;
import me.vinceh121.gmcserver.proxy.URadMonitorProxy;

public class ProxyManager extends AbstractManager {

	private final Map<String, AbstractProxy> proxies = new HashMap<>();

	public ProxyManager(final GMCServer srv) {
		super(srv);
		this.registerProxies();
	}

	private void registerProxies() {
		this.registerProxy(new GmcmapProxy(this.srv));
		this.registerProxy(new RadmonProxy(this.srv));
		this.registerProxy(new SafecastProxy(this.srv));
		this.registerProxy(new URadMonitorProxy(this.srv));
	}

	private void registerProxy(final AbstractProxy p) {
		this.proxies.put(p.getClass().getSimpleName(), p);
	}

	public Map<String, AbstractProxy> getProxies() {
		return this.proxies;
	}

	public ValidateProxiesSettingsAction validateProxiesSettings() {
		return new ValidateProxiesSettingsAction(this.srv);
	}

	public ProcessDeviceProxiesAction processDeviceProxies() {
		return new ProcessDeviceProxiesAction(this.srv);
	}

	/**
	 * Validates proxy settings.
	 * 
	 * Throws {@code EntityNotFoundException} if a specified proxy is unknown.
	 * Throws {@code IllegalArgumentException} if a proxy invalidated its settings.
	 */
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
				final AbstractProxy p = ProxyManager.this.proxies.get(key);
				if (p == null) {
					promise.fail("Unknown proxy '" + key + "'");
					return;
				}
				// FIXME we're missing some logic here
			}
			CompositeFuture.all(futures).onSuccess(f -> promise.complete()).onFailure(promise::fail);
		}

		public JsonObject getProxiesSettings() {
			return this.proxiesSettings;
		}

		public ValidateProxiesSettingsAction setProxiesSettings(final JsonObject proxiesSettings) {
			this.proxiesSettings = proxiesSettings;
			return this;
		}
	}

	/**
	 * Processes proxying.
	 *
	 * Proxying is differed and no exceptions should be thrown.
	 */
	public class ProcessDeviceProxiesAction extends AbstractAction<Void> {
		private Device device;
		private Record record;

		private ProcessDeviceProxiesAction(final GMCServer srv) {
			super(srv);
		}

		@Override
		protected void executeSync(final Promise<Void> promise) {
			for (final String key : this.device.getProxiesSettings().keySet()) {
				this.processProxy(ProxyManager.this.proxies.get(key));
			}
			promise.complete();
		}

		private void processProxy(final AbstractProxy p) {
			p.proxyRecord(this.record, this.device, this.device.getProxiesSettings().get(p.getClass().getSimpleName()))
				.onFailure(t -> {
					ProxyManager.this.log.error(
							new FormattedMessage("Failed to proxy record {} for {}",
									this.record.getId(),
									p.getClass().getSimpleName()),
							t);
					// TODO save error to report to user
				});
		}

		public Device getDevice() {
			return this.device;
		}

		public ProcessDeviceProxiesAction setDevice(final Device device) {
			this.device = device;
			return this;
		}

		public Record getRecord() {
			return this.record;
		}

		public ProcessDeviceProxiesAction setRecord(final Record record) {
			this.record = record;
			return this;
		}
	}

}

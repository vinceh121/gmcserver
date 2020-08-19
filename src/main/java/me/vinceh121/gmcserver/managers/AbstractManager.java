package me.vinceh121.gmcserver.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.vinceh121.gmcserver.GMCServer;

public abstract class AbstractManager {
	protected final GMCServer srv;
	protected final Logger log;

	public AbstractManager(final GMCServer srv) {
		this.srv = srv;
		this.log = LoggerFactory.getLogger(this.getClass());
	}
}

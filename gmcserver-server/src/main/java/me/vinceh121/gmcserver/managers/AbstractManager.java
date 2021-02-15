package me.vinceh121.gmcserver.managers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import me.vinceh121.gmcserver.GMCServer;

public abstract class AbstractManager {
	protected final GMCServer srv;
	protected final Logger log;

	public AbstractManager(final GMCServer srv) {
		this.srv = srv;
		this.log = LogManager.getLogger(this.getClass());
	}
}

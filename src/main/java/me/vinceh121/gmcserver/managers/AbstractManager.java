package me.vinceh121.gmcserver.managers;

import me.vinceh121.gmcserver.GMCServer;

public abstract class AbstractManager {
	protected final GMCServer srv;

	public AbstractManager(final GMCServer srv) {
		this.srv = srv;
	}
}

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

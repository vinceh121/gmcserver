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

module.exports = {
	URL: process.env.URL || "http://localhost:8081/api/v1",
	LOG_URL: process.env.LOG_URL || "http://localhost:8081",
	USERNAME: process.env.USERNAME || "testuser",
	PASSWORD: process.env.PASSWORD || "testpassword",
	EMAIL: process.env.EMAIL || "test@test.com",
	TOKEN: process.env.TOKEN,
	OBJECTID_REGEX: /^[a-f\d]{24}$/i
};

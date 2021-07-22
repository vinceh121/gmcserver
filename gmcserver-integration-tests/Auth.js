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

const fetch = require("node-fetch");
const globals = require("./globals");
const { URL, USERNAME, PASSWORD, EMAIL } = globals;

const register = async () => {
	const res = await fetch(URL + "/auth/register",
		{
			method: "POST",
			body: JSON.stringify({ username: USERNAME, password: PASSWORD, email: EMAIL })
		}
	);
	expect(res.status).toBe(200);
	const data = await res.json();
	expect(data.token).toBeTruthy();
	expect(data.id).toBeTruthy();
};

const login = async () => {
	const res = await fetch(URL + "/auth/login",
		{
			method: "POST",
			body: JSON.stringify({ username: USERNAME, password: PASSWORD })
		}
	);
	expect(res.status).toBe(200);
	const data = await res.json();
	expect(data.token).toBeTruthy();
	expect(data.id).toBeTruthy();
	expect(data.mfa).toBe(false);
};

module.exports = [register, login];

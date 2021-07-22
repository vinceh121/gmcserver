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
const { URL, TOKEN, USERNAME, PASSWORD } = require("./globals");

/**
 * @returns the auth token
 */
const login = async () => {
	if (TOKEN !== undefined)
		return TOKEN;
	const res = await fetch(URL + "/auth/login", {
		method: "POST",
		body: JSON.stringify({ username: USERNAME, password: PASSWORD })
	});
	const data = await res.json();
	return data.token;
};

const loginDevice = async (deviceId) => {
	const token = await login();

	const deviceRes = await fetch(URL + "/device/" + deviceId, { headers: { Authorization: token } });
	const deviceData = await deviceRes.json();
	const deviceGmcId = deviceData.gmcId;

	const userRes = await fetch(URL + "/user/me", { headers: { Authorization: token } });
	const userData = await userRes.json();
	const userGmcId = userData.gmcId;

	return { userGmcId, deviceGmcId };
};

module.exports = { login, loginDevice };

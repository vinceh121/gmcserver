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
const { URL, LOG_URL, OBJECTID_REGEX } = require("./globals");
const { login, loginDevice } = require("./gmcapi");

const DEVICE_NAME = "My Test Device";
const DEVICE_POS = [51.1594650, -1.4439722];
const DEVICE_MODEL = "Node Fetch";

let device = undefined;

const createDevice = async () => {
	const expectedDevice = {
		name: DEVICE_NAME,
		model: DEVICE_MODEL,
		location: DEVICE_POS,
		owner: expect.stringMatching(OBJECTID_REGEX)
	};

	const token = await login();
	const res = await fetch(URL + "/device",
		{
			method: "POST",
			body: JSON.stringify({ name: DEVICE_NAME, position: DEVICE_POS, model: DEVICE_MODEL }),
			headers: {
				Authorization: token,
				"Content-Type": "application/json"
			}
		}
	);
	expect(res.status).toBe(200);

	const data = await res.json();
	device = data;
	expect(data).toMatchObject(expectedDevice);
};

const emptyTimeline = async () => {
	const token = await login();
	const res = await fetch(URL + "/device/" + device.id + "/timeline")
	expect(res.status).toBe(200);

	const data = await res.json();
	expect(data).toStrictEqual([]);
};

const logGmc = async () => {
	const DATE = new Date().getTime();
	const CPM = 42.0;
	const USV = 0.69;
	const ACPM = 42.69;

	const { deviceGmcId, userGmcId } = await loginDevice(device.id);

	const logRes = await fetch(LOG_URL + `/log2.asp?AID=${userGmcId}&GID=${deviceGmcId}&CPM=${CPM}&UsV=${USV}&aCpM=${ACPM}`);
	const logData = await logRes.text();
	expect(logData).toBe("OK.ERR0");

	const tlRes = await fetch(URL + "/device/" + device.id + "/timeline");
	const tlData = await tlRes.json();
	const rec = tlData[0];

	expect(rec).toMatchObject({ cpm: CPM, usv: USV, acpm: ACPM });
	expect(rec.date >= DATE).toBe(true);
};

const logSafecast = async () => {
	const DATE = new Date();
	const CPM = 42.0;
	const LON = 1.444;
	const LAT = 43.6045;

	const { deviceGmcId, userGmcId } = await loginDevice(device.id);

	const logRes = await fetch(LOG_URL + "/measurements.json?api_key=" + userGmcId, {
		method: "POST",
		body: JSON.stringify({
			device_id: deviceGmcId,
			unit: "cpm",
			value: CPM,
			captured_at: DATE,
			longitude: LON,
			latitude: LAT
		}),
		headers: { "Content-Type": "application/json" }
	});
	const logData = await logRes.text();
	expect(logRes.status).toBe(200);

	const tlRes = await fetch(URL + "/device/" + device.id + "/timeline");
	const tlData = await tlRes.json();
	const rec = tlData[0];

	expect(rec).toMatchObject({ cpm: CPM, location: [LON, LAT] });
	expect(rec.date >= DATE).toBe(true);
};

module.exports = [createDevice, emptyTimeline, logGmc, logSafecast];

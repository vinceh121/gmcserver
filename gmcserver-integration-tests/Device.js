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
		owner: { id: expect.stringMatching(OBJECTID_REGEX) }
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

module.exports = [createDevice, emptyTimeline, logGmc];

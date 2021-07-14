const fetch = require("node-fetch");
const { URL, OBJECTID_REGEX } = require("./globals");
const { login } = require("./gmcapi");

const DEVICE_NAME = "My Test Device";
const DEVICE_POS = [51.1594650, -1.4439722];
const DEVICE_MODEL = "Node Fetch";

let device = undefined;

test("Create device", async () => {
	const expectedDevice = {
		name: DEVICE_NAME,
		model: DEVICE_MODEL,
		position: DEVICE_POS,
		user: { id: expect.stringMatching(OBJECTID_REGEX) }
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
	expect(data).toMatchObject(expectedDevice);
	device = data;
});

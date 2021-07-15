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

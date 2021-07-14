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

module.exports = { login };

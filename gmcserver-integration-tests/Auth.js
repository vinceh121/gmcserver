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

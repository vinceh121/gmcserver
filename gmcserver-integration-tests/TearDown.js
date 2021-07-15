const fetch = require("node-fetch");
const { login } = require("./gmcapi");
const { URL, PASSWORD } = require("./globals");

const deleteAccount = async () => {
	const token = await login();
	const res = await fetch(URL + "/user/me", {
		method: "DELETE",
		headers: { Authorization: token },
		body: JSON.stringify({ password: PASSWORD })
	});
	
	expect(res.status).toBe(200);
	
	const data = await res.json();

	expect(data.status).toBe(200);
};

module.exports = [deleteAccount];

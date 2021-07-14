const fetch = require("node-fetch");
const globals = require("./globals");
const { URL, USERNAME, PASSWORD, EMAIL } = globals;

test("register", (done) => {
	fetch(URL + "/auth/register",
		{
			method: "POST",
			body: JSON.stringify({ username: USERNAME, password: PASSWORD, email: EMAIL })
		}
	).then((res) => {
		expect(res.status).toBe(200);
		res.json().then((data) => {
			expect(data.token).toBeTruthy();
			expect(data.id).toBeTruthy();
			done();
		});
	}, (err) => {
		done(err);
	});
});


test("login", (done) => {
	fetch(URL + "/auth/login",
		{
			method: "POST",
			body: JSON.stringify({ username: USERNAME, password: PASSWORD })
		}
	).then((res) => {
		expect(res.status).toBe(200);
		res.json().then((data) => {
			expect(data.token).toBeTruthy();
			expect(data.id).toBeTruthy();
			expect(data.mfa).toBe(false);
			done();
		});
	}, (err) => {
		done(err);
	});
});

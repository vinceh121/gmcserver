module.exports = {
	URL: process.env.URL || "http://localhost:8081/api/v1",
	LOG_URL: process.env.LOG_URL || "http://localhost:8081/",
	USERNAME: process.env.USERNAME || "testuser",
	PASSWORD: process.env.PASSWORD || "testpassword",
	EMAIL: process.env.EMAIL || "test@test.com",
	TOKEN: process.env.TOKEN,
	OBJECTID_REGEX: /^[a-f\d]{24}$/i
};

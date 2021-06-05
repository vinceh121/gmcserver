// You need to run the following command to download the entire device index first:
// 	curl -o complete-gmcmap-mapdevices.json https://www.gmcmap.com/AJAX_load_time.asp\?OffSet\=0\&Limit\=2147483647\&dataRange\=100000\&timeZone\=2
// You also need to replace YOUR_TOKEN_HERE with your token

const fs = require("fs");
const http = require("http");

const reg = /Param_ID=([0-9]+)/g;
const data = String(fs.readFileSync("./complete-gmcmap-mapdevices.json"));

let ids = new Set();

for (let m of data.matchAll(reg)) {
	ids.add(m[1]);
}

console.log(ids);

for (let id of ids) {
	let req = http.request({
		hostname: "localhost",
		port: 8080,
		path: "/api/v1/import/gmcmap",
		method: "POST",
		headers: {
			Authorization: "YOUR_TOKEN_HERE"
		}
	}, (res) => {
		res.on("data", e => console.log("Data " + id + ": " + e));
		res.on("end", () => console.log("End " + res.statusCode + " for " + id));
	});
	req.on("error", e => console.log("Error with " + id + ": " + e));
	req.end(JSON.stringify({gmcmapId: id}));
}


const Auth = require("./Auth");
const Device = require("./Device");
const TearDown = require("./TearDown");

const testMatrix = [Auth, Device, TearDown];

for (let i of testMatrix) {
	for (let j of i) {
		it(j.name, j);
	}
}

let cur = db.devices.find({});

while (cur.hasNext()) {
	let dev = cur.next()
	print("doing device " + dev._id);
	let r = db.records.findOne({location: {$exists: true}, deviceId: dev._id});
	if (r) {
		let loc = JSON.parse(JSON.stringify(r.location));
		//loc.coordinates.pop();
		let coords = loc.coordinates;
		if (coords[0] < -180 || coords[0] > 180 || coords[1] < -90 || coords[1] > 90) {
			loc.coordinates = coords.reverse();
			print("should reverse!");
		}
		db.devices.updateOne({_id: dev._id}, {$set: {location: loc}});
	}
}

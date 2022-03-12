db.records.find({}).forEach(r => {
	if (!r.location) return;

	let loc = JSON.parse(JSON.stringify(r.location));
	let coords = loc.coordinates;

	if (coords[0] < -180 || coords[0] > 180 || coords[1] < -90 || coords[1] > 90) {
		loc.coordinates = coords.reverse();
//		print("reversing!");
	}
	db.devices.updateOne({_id: r._id}, {$set: {location: loc}});
});


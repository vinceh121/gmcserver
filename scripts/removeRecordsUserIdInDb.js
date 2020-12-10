let db = connect("localhost:27017/gmcserver")
let cursor = db.records.find()
while (cursor.hasNext()) {
	let rec = cursor.next()
	if (!Object.keys(rec).includes("userId")) continue
	print(rec._id + ":")
	db.records.updateOne({_id: rec._id}, {$unset: {userId: ""}})
}


let db = connect("localhost:27017/gmcserver")
let cursor = db.records.find()
while (cursor.hasNext()) {
	let rec = cursor.next()
	let fields = {}
	for (let f in rec) {
		if (Number.isNaN(rec[f])) {
			fields[f] = ""
		}
	}
	if (Object.keys(fields).length == 0) continue
	print(rec._id + ":")
	printjson(fields)
	db.records.updateOne({_id: rec._id}, {$unset: fields})
}


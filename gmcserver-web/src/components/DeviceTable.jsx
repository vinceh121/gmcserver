import React, { useEffect, useState } from "react";
import { Table } from "antd";
import { numericRecordFields } from "../GmcTypes";

const stdCols = []
stdCols.push({
	title: "Date",
	dataIndex: "date",
	key: "date",
	fixed: "left",
	render: a => new Date(a).toLocaleString(),
	sorter: (a, b) => a.date - b.date
});

numericRecordFields.map(f => ({
	title: f.toUpperCase(),
	dataIndex: f,
	key: f,
	sorter: (a, b) => a[f] - b[f]
})).forEach(e => stdCols.push(e));

[
	{
		title: "IP", dataIndex: "ip", key: "ip",
		sorter: (a, b) => a.ip.localeCompare(b.ip)
	},
	{
		title: "Type", dataIndex: "type", key: "type",
		sorter: (a, b) => a.type.localeCompare(b.type)
	},
	{
		title: "Location", dataIndex: "location", key: "location"
	}
].forEach(e => stdCols.push(e))

function DeviceTable(props) {
	const [cols, setCols] = useState([]);

	useEffect(() => {
		const fields = new Set();
		for (let r of props.timeline) {
			for (let f of Object.keys(r)) {
				if (r[f] && r[f] !== "NaN") {
					fields.add(f);
				}
			}
		}

		setCols(stdCols.filter(e => fields.has(e.key)));
	}, [props.timeline])

	return (
		<Table dataSource={props.timeline} columns={cols} />
	);
}

export default DeviceTable;

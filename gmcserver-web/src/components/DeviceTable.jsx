/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
		<Table onRow={(rec) => (props.onClick ? { onClick: () => props.onClick(rec) } : {})} dataSource={props.timeline} columns={cols} />
	);
}

export default DeviceTable;

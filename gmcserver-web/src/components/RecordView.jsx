/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import { Descriptions } from "antd";
import React from "react";
import { numericRecordFields } from "../GmcTypes";
import Loader from "./Loader";

function RecordView(props) {
	const record = props.record;
	if (record) {
		return (
			<Descriptions
				bordered
			>
				{record.id && <Descriptions.Item label="ID">{record.id}</Descriptions.Item>}
				<Descriptions.Item label="Date">{new Date(record.date).toLocaleString()}</Descriptions.Item>
				{numericRecordFields.map(f =>
					record[f] && record[f] !== "NaN" ? <Descriptions.Item key={f} label={f.toUpperCase()}>{record[f]}</Descriptions.Item> : undefined
				)}
				{record.ip && <Descriptions.Item label="IP">{record.ip}</Descriptions.Item>}
				{record.type && <Descriptions.Item label="Type">{record.type}</Descriptions.Item>}
				{record.location && <Descriptions.Item label="Location">{record.location}</Descriptions.Item>}
			</Descriptions>
		);
	} else {
		return <Loader />;
	}
}

export default RecordView;

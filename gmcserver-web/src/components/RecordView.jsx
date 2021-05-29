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

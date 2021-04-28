import React from "react";
import { Tooltip } from "antd";
import { ImportOutlined, LineOutlined, StopTwoTone } from "@ant-design/icons";

function DeviceBadge(props) {
	const device = props.device;
	if (device.importedFrom) {
		return (
			<Tooltip title={"Imported from " + device.importedFrom + (device.disabled ? "\nDisabled" : "")}>
				{device.disabled ? <ImportOutlined style={{ color: "#F44336" }} /> : <ImportOutlined />}
			</Tooltip>
		);
	} else if (device.disabled) {
		return (
			<Tooltip title="Disabled">
				<StopTwoTone twoToneColor="#F44336" />
			</Tooltip>
		);
	} else {
		return <LineOutlined />;
	}
}

export default DeviceBadge;

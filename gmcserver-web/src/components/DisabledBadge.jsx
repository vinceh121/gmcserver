import React from "react";
import { Tooltip } from "antd";
import { StopOutlined } from "@ant-design/icons";

function DisabledBadge() {
	return (
		<Tooltip title="Disabled">
			<StopOutlined />
		</Tooltip>
	);
}

export default DisabledBadge;

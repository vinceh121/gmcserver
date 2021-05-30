import React from "react";
import { SafetyOutlined } from "@ant-design/icons";
import { Tooltip } from "antd";

function AdminBadge() {
	return (
		<Tooltip placement="topRight" title="Administrator">
			<SafetyOutlined style={{ fontSize: "24px", color: "#F44336" }} />
		</Tooltip>
	);
}

export default AdminBadge;

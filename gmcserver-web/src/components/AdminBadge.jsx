import React from "react";
import { IdcardOutlined } from "@ant-design/icons";
import { Tooltip } from "antd";

function AdminBadge() {
	return (
		<Tooltip placement="topRight" title="Administrator">
			<IdcardOutlined style={{ fontSize: "24px" }} />
		</Tooltip>
	);
}

export default AdminBadge;

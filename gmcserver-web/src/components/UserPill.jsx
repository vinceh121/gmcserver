import React from "react";
import { Avatar, Button } from "antd";
import { SafetyOutlined, UserOutlined } from "@ant-design/icons";
import { Link } from "react-router-dom";

function UserPill(props) {
	const user = props.user;
	return (
		<Link to={"/user/" + user.id}>
			<Button
				shape="round"
				icon={
					<Avatar size="small" style={{ marginRight: "4px" }}
						icon={
							user.admin ? <SafetyOutlined style={{ color: "#F44336" }} /> : <UserOutlined />
						} />
				}
			>
				{user.username}
			</Button>
		</Link>
	);
}

export default UserPill;

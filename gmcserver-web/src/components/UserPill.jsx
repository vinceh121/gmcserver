import React from "react";
import { Avatar, Button } from "antd";
import { UserOutlined } from "@ant-design/icons";
import AdminBadge from "./AdminBadge";
import { Link } from "react-router-dom";

function UserPill(props) {
	const user = props.user;
	return (
		<Link to={"/user/" + user.id}>
			<Button
				shape="round"
				icon={<Avatar size="small" icon={<UserOutlined />} />}
			>
				{user.username}
			</Button>
		</Link>
	);
}

export default UserPill;

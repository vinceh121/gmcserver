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

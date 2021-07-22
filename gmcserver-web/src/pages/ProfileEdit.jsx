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

import React, { useEffect, useState } from "react";
import { useHistory } from "react-router-dom";
import { PageHeader, Form, Input, Divider, Button, message, Switch } from "antd";
import { fetchUser, getStorage, updateMe } from "../GmcApi";
import Loader from "../components/Loader";

function ProfileEdit() {
	const history = useHistory();
	const [user, setUser] = useState();

	useEffect(() => {
		fetchUser(getStorage().getItem("userId")).then(
			(res) => setUser(res),
			(err) => message.error(String(err))
		)
	}, []);

	const submit = (data) => {
		for (let f in data) {
			if (data[f] === "" || data[f] === user[f]) {
				delete data[f];
			}
		}

		updateMe(data).then(
			(res) => message.success(res.description),
			(err) => message.error(err.description)
		);
	};

	if (user) {
		return (
			<PageHeader
				onBack={history.goBack}
				title="Editing your account"
			>
				<Form
					name="profileEdit"
					initialValues={user}
					onFinish={submit}
				>
					<Form.Item
						label="Alert emails"
						name="alertEmails"
					>
						<Switch />
					</Form.Item>

					<Form.Item label="2FA">
						<Switch
							checked={user.mfa}
							onChange={() => {
								history.push("/mfa");
							}}
						/>
					</Form.Item>

					<Divider />

					<Form.Item
						label="Username"
						name="username"
					>
						<Input />
					</Form.Item>
					<Form.Item
						label="Email"
						name="email"
					>
						<Input />
					</Form.Item>

					<Divider />

					<Form.Item
						label="Current password"
						name="currentPassword"
					>
						<Input type="password" />
					</Form.Item>
					<Form.Item
						label="New password"
						name="newPassword"
					>
						<Input type="password" />
					</Form.Item>
					<Form.Item>
						<Button type="primary" htmlType="submit">
							Edit
					</Button>
					</Form.Item>
				</Form>
			</PageHeader>
		);
	} else {
		return (
			<Loader />
		);
	}
}

export default ProfileEdit;

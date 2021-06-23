import React, { useEffect, useState } from "react";
import { useHistory } from "react-router-dom";
import { PageHeader, Form, Input, Divider, Button, message } from "antd";
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

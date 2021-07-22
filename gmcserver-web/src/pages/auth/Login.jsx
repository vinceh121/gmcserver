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

import React, { useState } from "react";
import { Alert, Button, Card, Form, Input, InputNumber, Space } from "antd";
import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { Link, useHistory } from "react-router-dom";
import { login, mfaSubmit } from "../../GmcApi";
import Modal from "antd/lib/modal/Modal";

function Login() {
	const [state, setState] = useState({});
	const history = useHistory();

	const doLogin = (values) => {
		setState({ loading: true });
		login(values.username, values.password).then(
			(res) => {
				setState(res);
				if (!res.mfa) {
					history.push("/");
				}
			},
			(err) => setState({ error: err })
		);
	};

	const doMfa = (values) => {
		mfaSubmit(values.pass).then(
			res => {
				setState(res);
				history.push("/");
			},
			err => setState({ error: err })
		)
	}

	return (
		<>
			<Card title="Login" loading={state.loading} style={{ width: "max-content", margin: "16px" }}>
				<Space direction="vertical">
					{state.error ? (
						<Alert
							message="Error while logging in"
							type="error"
							description={
								state.error.description
									? state.error.description
									: String(state.error)
							}
							showIcon
						/>
					) : undefined}
					<Form onFinish={doLogin}>
						<Form.Item
							name="username"
							rules={[{ required: true, message: "Required" }]}
						>
							<Input prefix={<UserOutlined />} placeholder="Username or Email" />
						</Form.Item>
						<Form.Item
							name="password"
							rules={[{ required: true, message: "Required" }]}
						>
							<Input
								prefix={<LockOutlined />}
								type="password"
								placeholder="Password"
							/>
						</Form.Item>
						<Form.Item>
							<Button type="primary" htmlType="submit">
								Login
							</Button>
							<p style={{ marginTop: "8px" }}>Don't have an account? <Link to="/register">Register</Link></p>
						</Form.Item>
					</Form>
				</Space>
			</Card>
			<Modal title="2FA" visible={state.mfa}>
				<Form onFinish={doMfa}>
					<Form.Item name="pass">
						<InputNumber placeholder="MFA code" />
					</Form.Item>
					<Form.Item>
						<Button type="primary" htmlType="submit">Submit</Button>
					</Form.Item>
				</Form>
			</Modal>
		</>
	);
}

export default Login;

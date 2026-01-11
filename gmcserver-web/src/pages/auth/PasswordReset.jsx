/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import React, { useState } from "react";
import { Alert, Button, Card, Form, Input, Space, Result } from "antd";
import { UserOutlined } from "@ant-design/icons";
import { Link } from "react-router-dom";
import { passwordReset } from "../../GmcApi";

function PasswordReset() {
	const [state, setState] = useState({});

	const doPasswordReset = (values) => {
		setState({ loading: true });
		passwordReset(values.email)
			.then(() => setState({loading: false, success: true}))
			.catch(
			(err) => setState({ error: err })
		);
	};

	return (
		<>
			{!state.success ? <Card title="Password Reset" loading={state.loading} style={{ width: "max-content", margin: "16px" }}>
				<Space direction="vertical">
					{state.error ? (
						<Alert
							message="Error while password reset"
							type="error"
							description={
								state.error.description
									? state.error.description
									: String(state.error)
							}
							showIcon
						/>
					) : undefined}
					<Form onFinish={doPasswordReset}>
						<Form.Item
							name="email"
							rules={[{ required: true, message: "Required" }]}
						>
							<Input prefix={<UserOutlined />} placeholder="Email" />
						</Form.Item>
						<Form.Item>
							<Button type="primary" htmlType="submit">
								Reset my password
							</Button>
							<p style={{ marginTop: "8px" }}>Don't have an account? <Link to="/register">Register</Link></p>
						</Form.Item>
					</Form>
				</Space>
			</Card> :
				<Result
					status="success"
					title="An email has been sent to reset your password!"
				/>
			}
		</>
	);
}

export default PasswordReset;

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
import { Alert, Button, Card, Form, Input, Space } from "antd";
import { Link, useLocation, useHistory } from "react-router-dom";
import {passwordResetConfirmation} from "../../GmcApi";
import {LockOutlined} from '@ant-design/icons'

function PasswordResetConfirmation() {
	const [state, setState] = useState({});
	const location = useLocation();
	const history = useHistory();
	const searchParams = new URLSearchParams(location.search);

	const doPasswordReset = (values) => {
		setState({ loading: true });
		passwordResetConfirmation(searchParams.get('token'), values.password)
			.then(() => history.push('/login'))
			.catch(
			(err) => setState({ error: err })
		);
	};

	return (
		<>
			<Card title="Password Reset Confirmation" loading={state.loading} style={{ width: "max-content", margin: "16px" }}>
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
								Reset my password
							</Button>
							<p style={{ marginTop: "8px" }}>Don't have an account? <Link to="/register">Register</Link></p>
						</Form.Item>
					</Form>
				</Space>
			</Card>
		</>
	);
}

export default PasswordResetConfirmation;

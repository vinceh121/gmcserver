import React, { useState } from "react";
import { Alert, Button, Card, Form, Input, Space } from "antd";
import { LockOutlined, UserOutlined } from "@ant-design/icons";
import { Link, useHistory } from "react-router-dom";
import { login } from "../../GmcApi";
import Modal from "antd/lib/modal/Modal";

function Login() {
	const [state, setState] = useState({});
	const history = useHistory();

	const doLogin = (values) => {
		setState({ loading: true });
		login(values.username, values.password).then(
			(res) => {
				setState(res);
				if (!state.mfa) {
					history.push("/");
				}
			},
			(err) => setState({ error: err })
		);
	};

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
			<Modal title="2FA" visible={state.mfa}></Modal> {/* TODO */}
		</>
	);
}

export default Login;

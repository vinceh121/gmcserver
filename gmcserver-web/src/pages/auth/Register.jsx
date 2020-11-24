import React, { useState } from "react";
import { Alert, Button, Card, Form, Input } from "antd";
import { register } from "../../GmcApi";
import { useHistory } from "react-router-dom";

function Register() {
	const [state, setState] = useState({});
	const history = useHistory();

	const onFinish = (data) => {
		setState({ loading: true });
		register(data.username, data.email, data.password).then(
			(result) => {
				setState({ result });
				history.push("/welcome");
			},
			(error) => setState({ error })
		);
	};

	return (
		<div
			style={{
				backgroundImage:
					"url(https://images.unsplash.com/photo-1578995511335-b54ca0772e83?auto=format&fit=crop&w=1934&q=80)",
				backgroundBlendMode: "overlay",
				backgroundRepeat: "no-repeat",
				backgroundSize: "cover",
				backgroundAttachment: "fixed",
				backgroundPosition: "50%",
			}}
		>
			<Card
				title="Create account"
				style={{ position: "fixed", margin: "16px", right: 0 }}
			>
				{state.error ? (
					<Alert
						type="error"
						message={state.error.status + ": " + state.error.description}
						style={{ marginBottom: "16px" }}
						closable
					/>
				) : undefined}
				<Form onFinish={onFinish}>
					<Form.Item name="username" label="Username" required="true">
						<Input />
					</Form.Item>

					<Form.Item
						name="email"
						label="Email"
						required="true"
						rules={[{ type: "email", message: "Invalid email" }]}
					>
						<Input />
					</Form.Item>

					<Form.Item name="password" label="Password" required="true">
						<Input.Password />
					</Form.Item>

					<Form.Item>
						<Button type="primary" htmlType="submit" loading={state.loading}>
							Register
						</Button>
					</Form.Item>
				</Form>
			</Card>
		</div>
	);
}

export default Register;

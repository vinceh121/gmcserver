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
import { Alert, Button, Card, Form, Input } from "antd";
import { fetchCaptcha, fetchInstanceInfo, register } from "../../GmcApi";
import { useHistory } from "react-router-dom";

function Register() {
	const [state, setState] = useState({});
	const [captchaRequired, setCaptchaRequired] = useState(undefined);
	const [captchaId, setCaptchaId] = useState(undefined);
	const history = useHistory();

	useEffect(() => {
		if (captchaRequired && captchaId === undefined) {
			fetchCaptcha().then((id) => setCaptchaId(id));
		}
	}, [captchaId, captchaRequired]);

	useEffect(() => {
		if (captchaRequired === undefined) {
			fetchInstanceInfo().then((info) => setCaptchaRequired(info.captcha));
		}
	}, [captchaRequired]);

	const onFinish = (data) => {
		setState({ loading: true });
		register(data.username, data.email, data.password, data.captchaAnswer, captchaId).then(
			(result) => {
				setState({ result });
				history.push("/welcome");
			},
			(error) => {
				setState({ error });
				if (error.extras && (error.extras.captchaResponse === "False" || error.extras.captchaResponse === "Expired")) {
					setCaptchaId(undefined);
				}
			}
		);
	};

	return (
		<div
			style={{
				backgroundImage:
					"url(https://images.unsplash.com/photo-1578995511335-b54ca0772e83?auto=format&fit=crop&w=1934&q=80&blur=100)",
				backgroundBlendMode: "overlay",
				backgroundRepeat: "no-repeat",
				backgroundSize: "cover",
				backgroundAttachment: "fixed",
				backgroundPosition: "50%",
				height: "80vh"
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

					{captchaRequired ?
						<Card size="small" style={{ marginBottom: "24px" }}>
							<img alt="Captcha" src={"/api/v1/captcha?id=" + captchaId} />
							<Form.Item noStyle="true" name="captchaAnswer" required="true">
								<Input />
							</Form.Item>
						</Card>
						: null}

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

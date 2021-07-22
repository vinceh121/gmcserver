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

import React, { useState, useEffect } from "react";
import {
	Button,
	Card,
	message,
	Result,
	Space,
	Steps,
	Typography,
	Form,
	InputNumber,
} from "antd";
import Loader from "../components/Loader";
import { fetchMe, mfaDisable, mfaFinishSetup, mfaStartSetup } from "../GmcApi";
import QRCode from "react-qr-code";
import { useHistory } from "react-router-dom";

const { Step } = Steps;
const { Text } = Typography;

function MfaSetup(props) {
	const [mfaUri, setMfaUri] = useState();

	useEffect(() => {
		if (props.step === 0) {
			mfaStartSetup().then(
				(res) => setMfaUri(res.mfaUri),
				(err) => message.error("Failed to get MFA URI")
			);
		}
	}, [props.step]);

	if (props.step === 0 && mfaUri) {
		return (
			<div>
				<Space direction="vertical">
					<QRCode value={mfaUri} />
					<Text code>{mfaUri}</Text>
					<Button type="primary" onClick={props.onNextStep}>
						Next
					</Button>
				</Space>
			</div>
		);
	} else if (props.step === 1) {
		return (
			<div>
				<Form
					onFinish={(v) => {
						mfaFinishSetup(v.pass).then((r) => {
							props.onNextStep();
						});
					}}
				>
					<Form.Item
						name="pass"
						rules={[
							{
								type: "number",
								min: 0,
								max: 999_999,
								message: "Invalid MFA pass",
							},
						]}
					>
						<InputNumber placeholder="MFA code" />
					</Form.Item>
					<Form.Item>
						<Button type="primary" htmlType="submit">
							Submit
						</Button>
					</Form.Item>
				</Form>
			</div>
		);
	} else if (props.step === 2) {
		return (
			<Result
				status="success"
				title="MFA is now setup on your account!"
			/>
		);
	} else {
		return <Loader />;
	}
}

function Mfa(props) {
	const history = useHistory();
	const [state, setState] = useState({ step: 0 });

	useEffect(() => {
		fetchMe().then(
			(user) =>
				setState((s) => {
					return Object.assign({}, s, { mfa: user.mfa });
				}),
			(err) => message.error("Failed to load user info")
		);
	}, []);

	const disableMfa = (values) => {
		mfaDisable(values.pass).then(
			res => {
				message.success("MFA disabled!");
				history.push("/profile");
			},
			err => message.error("Failed to disable MFA: " + String(err))
		)
	};

	if (state.mfa == null) {
		return <Loader />;
	} else if (state.mfa) {
		return (
			<Card title="Disable MFA">
				<Form onFinish={disableMfa}>
					<Form.Item name="pass">
						<InputNumber placeholder="MFA code" />
					</Form.Item>
					<Form.Item>
						<Button type="primary" htmlType="submit">Submit</Button>
					</Form.Item>
				</Form>
			</Card>
		);
	} else {
		return (
			<Card
				title={
					<Steps current={state.step}>
						<Step title="Save MFA key" />
						<Step title="Confirm MFA code" />
						<Step title="MFA setup complete" />
					</Steps>
				}
			>
				<MfaSetup
					onNextStep={() =>
						setState(
							Object.assign({}, state, { step: state.step + 1 })
						)
					}
					step={state.step}
				/>
			</Card>
		);
	}
}

export default Mfa;

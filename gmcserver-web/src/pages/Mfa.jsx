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
import { fetchMe, mfaFinishSetup, mfaStartSetup } from "../GmcApi";
import QRCode from "react-qr-code";

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

	if (state.mfa == null) {
		return <Loader />;
	} else if (state.mfa) {
		return <Result title="TODO: disable MFA" status="404" />; // TODO
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

import React, { useState, useEffect } from "react";
import { Card, message, Result, Steps } from "antd";
import Loader from "../components/Loader";
import { fetchMe, mfaStartSetup } from "../GmcApi";
import QRCode from "react-qr-code";

const { Step } = Steps;

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
				<QRCode value={mfaUri} />
			</div>
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

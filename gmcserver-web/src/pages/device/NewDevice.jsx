import React, { useState } from "react";
import { useHistory } from "react-router-dom";
import { Form, Alert, Button, Input, InputNumber, PageHeader } from "antd";
import { createDevice } from "../../GmcApi";

function NewDevice() {
	const history = useHistory();
	const [state, setState] = useState(null);

	const submit = (vals) => {
		createDevice(vals.name, vals.lat, vals.lon).then(
			(dev) => history.push("/device/" + dev.id),
			(error) => setState({ error })
		);
	};

	return (
		<PageHeader title="New Device" onBack={history.onBack}>
			{state && state.error ? (
				<Alert
					message={"Failed to create device: " + String(state.error)}
					type="error"
					closable
				/>
			) : undefined}
			<Form onFinish={submit}>
				<Form.Item label="Name" name="name">
					<Input />
				</Form.Item>
				<Form.Item label="Latitude" name="lat">
					<InputNumber />
				</Form.Item>
				<Form.Item label="Longitude" name="lon">
					<InputNumber />
				</Form.Item>
				<Form.Item>
					<Button type="primary" htmlType="submit">
						Create
					</Button>
				</Form.Item>
			</Form>
		</PageHeader>
	);
}

export default NewDevice;

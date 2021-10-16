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
		<PageHeader title="New Device" onBack={history.goBack}>
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
				<Form.Item label="Model" name="model">
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

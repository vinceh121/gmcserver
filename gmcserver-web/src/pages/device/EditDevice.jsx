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

import { MoreOutlined, QuestionCircleOutlined } from "@ant-design/icons";
import { Input, PageHeader, Result, Switch, Form, Tabs, Tooltip, Button, Card, Space, InputNumber, message, Row, Menu, Dropdown } from "antd";
import React, { useEffect, useState } from "react";
import { Link, useHistory, useParams } from "react-router-dom";
import Loader from "../../components/Loader";
import { disableDevice, fetchDevice, updateDevice } from "../../GmcApi";
import { proxySettingsModels } from "../../GmcTypes";

const { TabPane } = Tabs;

function EditDevice() {
	const [form] = Form.useForm();
	const history = useHistory();
	const [device, setDevice] = useState(null);
	const [deviceError, setDeviceError] = useState(null);
	const { id } = useParams();

	useEffect(() => {
		fetchDevice(id).then(
			(device) => {
				setDevice(device)
			},
			(error) => setDeviceError(error)
		);
	}, [id]);

	const onFinish = () => {
		const changedFields = [];
		for (let f in form.getFieldValue()) {
			if (form.isFieldTouched(f)) {
				changedFields.push(f);
			}
		}
		const patch = form.getFieldsValue(changedFields);
		for (const proxy in patch.proxiesSettings) {
			const vals = Object.values(patch.proxiesSettings[proxy]);
			if (vals.every(v => v === null)
				|| vals.every(v => v === "")) {
				delete patch.proxiesSettings[proxy];
			}
		}
		updateDevice(id, patch).then(
			up => message.success("Device edited!"),
			err => message.error("Failed to edit device: " + err, 5)
		);
	};

	const onDelete = () => {
		disableDevice(id, true).then(
			(res) => {
				message.success("Device deleted");
				history.push("/profile");
			}, (err) => message.error(err.description));
	};

	if (device && device.own) {
		return (
			<PageHeader
				onBack={history.goBack}
				title={device.name}
				subTitle={device.gmcId}
				extra={[
					<Button key="0" danger>
						<Link to={"/device/" + id}>Cancel</Link>
					</Button>,
					<Button key="1" type="primary" onClick={onFinish}>
						Save
					</Button>,
					<Dropdown overlay={
						<Menu>
							<Button key="2" type="link" onClick={onDelete} danger>
								Delete
							</Button>
						</Menu>
					}>
						<Button>
							<MoreOutlined />
						</Button>
					</Dropdown>
				]}
			>
				<Form
					name="deviceEdit"
					initialValues={device}
					form={form}
					// onFinish={onFinish}
					labelCol={{ span: 4 }}
					wrapperCol={{ span: 8 }}
				>
					<Tabs>
						<TabPane key="1" tab="General">
							<Form.Item
								name="name"
								label="Name"
								rules={[{ required: true }]}
							>
								<Input />
							</Form.Item>
							<Form.Item
								name="model"
								label="Model"
							>
								<Input />
							</Form.Item>
							<Form.Item
								label="Location"
							>
								<Input.Group compact>
									<Tooltip title="Latitude">
										<Form.Item name={["location", 1]} noStyle>
											<InputNumber />
										</Form.Item>
									</Tooltip>
									<Tooltip title="Longitude">
										<Form.Item name={["location", 0]} noStyle>
											<InputNumber />
										</Form.Item>
									</Tooltip>
								</Input.Group>
							</Form.Item>
							<Form.Item
								label="Disabled"
							>
								<Space>
									<Form.Item name="disabled" valuePropName="checked" noStyle>
										<Switch />
									</Form.Item>
									<Tooltip title="When a device is disabled it won't be able to register new records nor proxy any">
										<QuestionCircleOutlined />
									</Tooltip>
								</Space>
							</Form.Item>
							{device.importedFrom ?
								<Form.Item
									name="importedFrom"
									label="Imported from"
								>
									<Input disabled />
								</Form.Item>
								: undefined}
						</TabPane>
						<TabPane key="2" tab="Proxies">
							<Space direction="vertical">
								{Object.keys(proxySettingsModels).map(modelName => {
									const model = proxySettingsModels[modelName];
									return (
										<Card key={modelName} title={modelName.replace("Proxy", "")}>
											{Object.keys(model).map(valueName => {
												const valueType = model[valueName];
												return (
													<Row>
														<Form.Item
															col={{ span: 16 }} wrapperCol={{ span: 24 }}
															key={valueName} name={["proxiesSettings", modelName, valueName]}
															label={valueName}>
															{valueType === "number" ? <InputNumber /> : <Input />}
														</Form.Item>
													</Row>
												);
											})}
										</Card>
									);
								})}
							</Space>
						</TabPane>
					</Tabs>
				</Form>
			</PageHeader>
		);
	} else if (device && !device.own) {
		return (
			<Result
				status="403"
				title="You do not own this device"
			/>
		);
	} else if (deviceError) {
		return (
			<Result
				status="500"
				title="Failed to fetch device"
				subTitle={String(deviceError)}
			/>
		);
	} else {
		return <Loader subTitle="Loading device..." />;
	}
}

export default EditDevice;
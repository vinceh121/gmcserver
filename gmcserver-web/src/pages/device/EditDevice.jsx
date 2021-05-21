import { QuestionCircleOutlined } from "@ant-design/icons";
import { Input, PageHeader, Result, Switch, Form, Tabs, Tooltip, Button, Card, Space, InputNumber, message } from "antd";
import React, { useEffect, useState } from "react";
import { Link, useHistory, useParams } from "react-router-dom";
import Loader from "../../components/Loader";
import { fetchDevice, updateDevice } from "../../GmcApi";
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
				console.log(device);
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
					<Button type="primary" onClick={onFinish}>
						Save
					</Button>
				]}
			>
				<Form
					name="deviceEdit"
					initialValues={device}
					form={form}
					// onFinish={onFinish}
					labelCol={{ span: 2 }}
					wrapperCol={{ span: 6 }}
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
								name="disabled"
								label="Disabled"
							>
								<Space>
									<Switch />
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
										<Card title={modelName.replace("Proxy", "")}>
											{Object.keys(model).map(valueName => {
												const valueType = model[valueName];
												return (
													<Form.Item name={["proxiesSettings", modelName, valueName]} label={valueName}>
														{valueType === "number" ? <InputNumber /> : <Input />}
													</Form.Item>
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
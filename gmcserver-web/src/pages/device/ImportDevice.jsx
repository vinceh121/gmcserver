import React from "react";
import { Card, Input, InputNumber, PageHeader, Row, Tabs, Form, Button, message } from "antd";
import { platformImports } from "../../GmcTypes";
import { ImportOutlined } from "@ant-design/icons";
import { importDevice } from "../../GmcApi";
import { useHistory } from "react-router-dom";

const { TabPane } = Tabs;

function PlatformContent({ platform }) {
	const history = useHistory();
	return (
		<Card>
			<Form onFinish={(values) => {
				message.loading("Importing...");
				importDevice(platform.name, values)
					.then((imp) => {
						message.success("Import started!");
						history.push("/device/" + imp.deviceId);
					})
					.catch((err) => message.error("Failed to import: " + err));
			}}>
				{Object.keys(platform.fields).map(valueName => {
					const valueType = platform.fields[valueName];
					return (
						<Row>
							<Form.Item
								col={{ span: 16 }} wrapperCol={{ span: 24 }}
								key={valueName} name={valueName}
								label={valueName}>
								{valueType === "number" ? <InputNumber /> : <Input />}
							</Form.Item>
						</Row>
					);
				})}
				<Form.Item>
					<Button type="primary" htmlType="submit" icon={<ImportOutlined />}>Import</Button>
				</Form.Item>
			</Form>
		</Card>
	);
}

function ImportDevice() {
	return (
		<PageHeader title="Import device">
			<Tabs>
				{platformImports.map(p =>
					<TabPane key={p.name} tab={p.displayName}>
						<PlatformContent platform={p} />
					</TabPane>
				)}
			</Tabs>
		</PageHeader>
	);
}

export default ImportDevice;

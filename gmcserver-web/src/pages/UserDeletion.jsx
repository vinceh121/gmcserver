import React from "react";
import { Button, Card, Form, Input, message, PageHeader, Typography } from "antd";
import { useHistory } from "react-router-dom";
import { deleteMe } from "../GmcApi";

const { Text, Paragraph } = Typography;

function UserDeletion() {
	const history = useHistory();

	const makeDelete = (vals) => {
		deleteMe(vals.password).then(
			(res) => message.success(res.message),
			(err) => message.error(err.message)
		);
	};

	return (
		<PageHeader
			title="Account deletion"
			onBack={history.goBack}
		>
			<Typography>
				<Paragraph>
					Deleting your account <Text type="danger">permanently and instantly deletes any data linked to you.</Text>
					This includes your devices and all data they sent. Data that has been proxied will not be deleted.
				</Paragraph>
			</Typography>

			<Card>
				<Form onFinish={makeDelete}>
					<Form.Item
						label="Password"
						name="password"
						rules={[{ required: true }]}
					>
						<Input.Password />
					</Form.Item>
					<Form.Item>
						<Button htmlType="submit" danger>Permanently delete my account</Button>
					</Form.Item>
				</Form>
			</Card>
		</PageHeader>
	);
}

export default UserDeletion;

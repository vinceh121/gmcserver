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

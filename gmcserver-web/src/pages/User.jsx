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
import { useParams, useHistory, Link } from "react-router-dom";
import {
	List,
	PageHeader,
	Result,
	Space,
	Tag,
	Typography,
	Form,
	InputNumber,
	Button,
	Menu,
	Dropdown,
} from "antd";
import { fetchUser, logoff } from "../GmcApi";
import AdminBadge from "../components/AdminBadge";
import DeviceBadge from "../components/DeviceBadge";
import Loader from "../components/Loader";
import { DownOutlined, EditOutlined, PlusOutlined } from "@ant-design/icons";

const { Title } = Typography;

function User() {
	const history = useHistory();
	const [state, setState] = useState(null);
	const { id } = useParams();

	useEffect(() => {
		fetchUser(id).then(
			(user) => setState({ user }),
			(error) => setState({ error })
		);
	}, [id]);

	if (state && state.user && state.user.status === 401) {
		return (
			<Result
				status="404"
				title="Invalid session"
				subTitle="Your sessions seems to be invalid. Please log back in."
				extra={
					<Link to="/login">
						<Button type="primary">Login</Button>
					</Link>
				}
			/>
		);
	}

	if (state && state.user) {
		const user = state.user;

		const optionsMenu = (
			<Menu>
				<Menu.Item
					onClick={() => history.push("/profile/edit")}
				>
					<Button
						type="link"
						icon={<EditOutlined />}
					>
						Edit
					</Button>
				</Menu.Item>
				<Menu.Item
					onClick={() => {
						logoff();
						history.push("/");
					}}
				>
					<Button
						type="link"
						danger
					>
						Logout
					</Button>
				</Menu.Item>
				<Menu.Item
					onClick={() => {
						history.push("/accountDeletion");
					}}
				>
					<Button
						type="link"
						danger
					>
						Delete account
					</Button>
				</Menu.Item>
			</Menu>
		);

		return (
			<PageHeader
				onBack={history.goBack}
				title={user.username}
				subTitle={user.gmcId ? user.gmcId : undefined}
				tags={[
					user.admin ? (
						<Tag key="admin">
							<AdminBadge />
						</Tag>
					) : undefined,
					user.self ? <Tag key="you">This is you</Tag> : undefined,
				]}
				extra={[
					<Button
						icon={<PlusOutlined />}
						onClick={() => history.push("/device/new")}
					>
						New Device
					</Button>,
					<Dropdown overlay={optionsMenu} placement="bottomRight">
						<Button>
							<DownOutlined />
						</Button>
					</Dropdown>
				]}
			>
				{user.self ? (
					<Form>
						<Form.Item label="Device limit">
							<InputNumber
								disabled={true}
								value={user.deviceLimit}
							/>
						</Form.Item>
					</Form>
				) : undefined}
				<Title level={5}>Devices</Title>
				<List
					itemLayout="horizontal"
					dataSource={user.devices}
					renderItem={(item) => (
						<Link to={"/device/" + item.id}>
							<List.Item>
								{/* <Button type="text" block> */}
								<Space size="small">
									{<DeviceBadge device={item} />}
									{item.name}
								</Space>
								{/* </Button> */}
							</List.Item>
						</Link>
					)}
				></List>
			</PageHeader>
		);
	} else if (state && state.error) {
		return (
			<Result
				status="500"
				title="Failed to fetch user"
				subTitle={String(state.error)}
			/>
		);
	} else {
		return <Loader subTitle="Loading user..." />;
	}
}

export default User;

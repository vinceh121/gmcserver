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
	Switch,
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
				<Menu.Item>
					<Button
						type="link"
						icon={<EditOutlined />}
						onClick={() => history.push("/profile/edit")}
					>
						Edit
					</Button>
				</Menu.Item>
				<Menu.Item>
					<Button
						type="link"
						onClick={() => {
							logoff();
							history.push("/");
						}}
						danger
					>
						Logout
					</Button>
				</Menu.Item>
				<Menu.Item>
					<Button
						type="link"
						onClick={() => {
							history.push("/accountDeletion");
						}}
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
						<Form.Item label="2FA">
							<Switch
								checked={user.mfa}
								onChange={() => {
									history.push("/mfa");
								}}
							/>
						</Form.Item>
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

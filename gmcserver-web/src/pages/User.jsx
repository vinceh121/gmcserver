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
} from "antd";
import { fetchUser, logoff } from "../GmcApi";
import AdminBadge from "../components/AdminBadge";
import DisabledBadge from "../components/DisabledBadge";
import Loader from "../components/Loader";

const { Title } = Typography;

function setupMfa() {
	// TODO
	console.log("setupMfa() TODO");
}

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

	if (state && state.user) {
		const user = state.user;
		return (
			<PageHeader
				onBack={history.goBack}
				title={user.username}
				subTitle={user.gmcId ? user.gmcId : undefined}
				tags={[
					user.admin ? (
						<Tag>
							<AdminBadge />
						</Tag>
					) : undefined,
					user.self ? <Tag>This is you</Tag> : undefined,
				]}
				extra={[
					user.self ? (
						<Button
							key="0"
							onClick={() => {
								logoff();
								history.push("/");
							}}
							danger
						>
							Logout
						</Button>
					) : undefined,
				]}
			>
				{user.self ? (
					<Form>
						<Form.Item label="2FA">
							<Switch checked={user.mfa} onChange={setupMfa} />
						</Form.Item>
						<Form.Item label="Device limit">
							<InputNumber disabled={true} value={user.deviceLimit} />
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
									{item.disabled ? <DisabledBadge /> : ""}
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

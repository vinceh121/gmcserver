import React, { useState, useEffect } from "react";
import { useParams, useHistory, Link } from "react-router-dom";
import { List, PageHeader, Result, Space, Spin } from "antd";
import { LoadingOutlined } from "@ant-design/icons";
import { fetchUser } from "../GmcApi";
import AdminBadge from "../components/AdminBadge";
import DisabledBadge from "../components/DisabledBadge";

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
				tags={user.admin ? <AdminBadge /> : undefined}
			>
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
		return (
			<Result
				subTitle="Loading user..."
				icon={
					<Spin indicator={<LoadingOutlined spin style={{ fontSize: 34 }} />} />
				}
			/>
		);
	}
}

export default User;

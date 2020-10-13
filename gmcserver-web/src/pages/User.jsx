import React, { useState, useEffect } from "react";
import { useParams, useHistory } from "react-router-dom";
import { PageHeader, Result, Spin } from "antd";
import { LoadingOutlined } from "@ant-design/icons";
import { fetchUser } from "../GmcApi";
import AdminBadge from "../AdminBadge";

function User() {
	const history = useHistory();
	const [state, setState] = useState(null);
	const { id } = useParams();

	useEffect(() => {
		fetchUser(id).then(
			(user) => setState({ user }),
			(error) => setState({ error })
		);
	}, []);

	if (state && state.user) {
		const user = state.user;
		return (
			<PageHeader
				onBack={history.goBack}
				title={user.username}
				extra={[
					<AdminBadge/>
				]}
			></PageHeader>
		);
	} else {
		return (
			<Result
				subTitle="Loading user..."
				icon={<Spin indicator={<LoadingOutlined style={{ fontSize: 34 }} />} />}
			/>
		);
	}
}

export default User;

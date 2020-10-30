import React, { useEffect } from "react";
import { getStorage } from "../GmcApi";
import { useHistory, Link } from "react-router-dom";
import Loader from "../components/Loader";
import { Button, Result } from "antd";

function Profile() {
	const id = getStorage().getItem("userId");
	const history = useHistory();

	useEffect(() => {
		if (id) {
			history.push("/user/" + id);
		}
	}, [history, id]);

	if (id) {
		return <Loader />;
	} else {
		return (
			<Result
				status="404"
				title="Not logged in"
				subTitle="Sorry but you don't seem to be logged in"
				extra={
					<Link to="/login">
						<Button type="primary">Login</Button>
					</Link>
				}
			/>
		);
	}
}

export default Profile;

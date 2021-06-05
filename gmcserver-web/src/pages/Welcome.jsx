import React from "react";
import { Button, Result } from "antd";
import { Link } from "react-router-dom";

function Welcome() {
	return (
		<Result
			status="success"
			title="Thank you for creating an account"
			extra={
				<Link to="/">
					<Button type="primary" key="home">
						Go home
					</Button>
				</Link>
			}
		/>
	);
}

export default Welcome;

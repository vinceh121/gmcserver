import React from "react";
import { Typography, Divider } from "antd";
import { CoffeeOutlined } from "@ant-design/icons";

const { Title, Paragraph } = Typography;

function NotFound() {
	return (
		<Typography style={{padding: "16px"}}>
			<Title>404: Not found</Title>
			<Divider />
			<Paragraph>
				It looks like you're lost, here's a coffee <CoffeeOutlined />
			</Paragraph>
		</Typography>
	);
}

export default NotFound;

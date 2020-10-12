import React from "react";
import { Result, Skeleton, Typography } from "antd";
import snarckdown from "snarkdown";

import { fetchInstanceInfo } from "../GmcApi";

const { Title, Paragraph } = Typography;

class Home extends React.Component {
	state = {};

	constructor(props) {
		super(props);
		fetchInstanceInfo().then(
			(info) => {
				const aboutMd = snarckdown(info.about);
				this.setState({ info: info, aboutMarkdown: aboutMd });
			},
			(err) => {
				this.setState({ error: err });
			}
		);
	}

	render() {
		if (this.state.info) {
			return (
				<Typography style={{ padding: "16px" }}>
					<Title>{this.state.info.name}</Title>
					<Paragraph>
						<div
							dangerouslySetInnerHTML={{ __html: this.state.aboutMarkdown }}
						></div>
					</Paragraph>
				</Typography>
			);
		} else if (this.state.error) {
			return (
				<Result
					status="500"
					title="Failed to fetch instance info"
					subTitle={String(this.state.error)}
				/>
			);
		} else {
			return <Skeleton active paragraph={{ rows: 4 }} />;
		}
	}
}

export default Home;

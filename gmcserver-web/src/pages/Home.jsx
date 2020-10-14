import React, { useState, useEffect } from "react";
import { Result, Skeleton, Typography } from "antd";
import snarckdown from "snarkdown";

import { fetchInstanceInfo } from "../GmcApi";

const { Title, Paragraph } = Typography;

function Home() {
	const [state, setState] = useState(null);

	useEffect(() => {
		fetchInstanceInfo().then(
			(info) => {
				const aboutMd = snarckdown(info.about);
				setState({ info: info, aboutMarkdown: aboutMd });
			},
			(err) => {
				setState({ error: err });
			}
		);
	}, []);

	if (state && state.info) {
		return (
			<Typography style={{ padding: "16px" }}>
				<Title>{state.info.name}</Title>
				<Paragraph>
					<div
						dangerouslySetInnerHTML={{ __html: state.aboutMarkdown }}
					></div>
				</Paragraph>
			</Typography>
		);
	} else if (state && state.error) {
		return (
			<Result
				status="500"
				title="Failed to fetch instance info"
				subTitle={String(state.error)}
			/>
		);
	} else {
		return <Skeleton active paragraph={{ rows: 4 }} />;
	}
}

export default Home;

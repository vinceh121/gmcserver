/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

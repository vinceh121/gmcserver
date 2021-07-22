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

import React, { useEffect, useState } from "react";
import { Card, PageHeader } from "antd";
import Loader from "../../components/Loader";
import { useHistory, useParams } from "react-router-dom";
import DeviceChart from "../../components/DeviceChart";
import { fetchDevice, openLiveTimeline } from "../../GmcApi";

function LiveDevice() {
	const history = useHistory();
	const [device, setDevice] = useState(null);
	const [timeline, setTimeline] = useState([]);
	const [ws, setWs] = useState(null);
	const { id } = useParams();

	useEffect(() => {
		fetchDevice(id).then((dev) => setDevice(dev));
		setWs(openLiveTimeline(id));
	}, [id]);

	useEffect(() => {
		if (ws)
			ws.onmessage = (msg) =>
				setTimeline([].concat([JSON.parse(msg.data)], timeline));
	}, [ws, timeline]);

	if (device) {
		return (
			<PageHeader
				onBack={history.goBack}
				title={"Live view: " + device.name}
			>
				<Card
					style={{ margin: "16px" }}
					bodyStyle={{ height: "500px" }}
				>
					<DeviceChart timeline={timeline} />
				</Card>
			</PageHeader>
		);
	} else {
		return <Loader subTitle="Loading device live view..." />;
	}
}

export default LiveDevice;

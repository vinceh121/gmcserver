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

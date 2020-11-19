import React, { useEffect, useState } from "react";
import { Card } from "antd";
import Loader from "../components/Loader";
import { useParams } from "react-router-dom";
import DeviceChart from "../components/DeviceChart";
import { fetchDevice, openLiveTimeline } from "../GmcApi";

function LiveDevice() {
	const [device, setDevice] = useState(null);
	const [timeline, setTimeline] = useState([]);
	const { id } = useParams();

	useEffect(() => {
		fetchDevice(id).then((dev) => setDevice(dev));
	}, [id]);

	useEffect(() => {
		openLiveTimeline(id).onmessage = (msg) =>
			setTimeline([].concat([JSON.parse(msg.data)], timeline));
	}, [id, timeline]);

	if (device) {
		return (
			<Card
				title={"Live view: " + device.name}
				style={{ margin: "16px" }}
				bodyStyle={{ height: "500px" }}
				loading={timeline.length}
			>
				<DeviceChart timeline={timeline} />
			</Card>
		);
	} else {
		return <Loader subTitle="Loading device live view..." />;
	}
}

export default LiveDevice;

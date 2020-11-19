import React, { useState, useEffect } from "react";
import { useParams, useHistory } from "react-router-dom";
import { fetchDevice, fetchTimeline } from "../GmcApi";
import {
	Card,
	Checkbox,
	DatePicker,
	Descriptions,
	PageHeader,
	Result,
	Space,
} from "antd";
import DisabledBadge from "../components/DisabledBadge";

import Loader from "../components/Loader";
import DeviceChart from "../components/DeviceChart";

const { RangePicker } = DatePicker;

function Device() {
	const history = useHistory();
	const [device, setDevice] = useState(null);
	const [deviceError, setDeviceError] = useState(null);
	const [timeline, setTimeline] = useState(null);
	// const [timelineError, setTimelineError] = useState(null);
	const [input, setInput] = useState({});
	const { id } = useParams();

	useEffect(() => {
		fetchDevice(id).then(
			(device) => setDevice(device),
			(error) => setDeviceError(error)
		);
	}, [id]);

	useEffect(() => {
		if (device) {
			fetchTimeline(device.id, input.full, input.start, input.end).then(
				(recs) => setTimeline(recs),
				// (err) => setTimelineError(err)
			);
		}
	}, [device, input.full, input.start, input.end]);

	if (device) {
		return (
			<PageHeader
				onBack={history.goBack}
				title={device.name}
				subTitle={device.gmcId}
				tags={device.disabled ? <DisabledBadge /> : undefined}
			>
				<Descriptions size="small">
					{device.importedFrom ? (
						<Descriptions.Item label="Imported from">
							{device.importedFrom}
						</Descriptions.Item>
					) : undefined}

					{device.model ? (
						<Descriptions.Item label="Model">{device.model}</Descriptions.Item>
					) : undefined}

					{device.location ? (
						<Descriptions.Item label="Location">
							{device.location[1]}, {device.location[0]}
						</Descriptions.Item>
					) : undefined}
				</Descriptions>
				<Card bodyStyle={{ height: "500px" }} loading={!timeline}>
					<DeviceChart
						full={input.full}
						start={input.start}
						end={input.end}
						timeline={timeline}
					/>
				</Card>
				<Space direction="vertical">
					<RangePicker
						showTime
						onChange={(d) =>
							setInput(
								Object.assign({}, input, {
									start: d[0].toDate(),
									end: d[1].toDate(),
								})
							)
						}
					/>

					<Checkbox
						disabled={!device.own}
						onChange={(e) =>
							setInput(Object.assign({}, input, { full: e.target.checked }))
						}
					>
						Full timeline
					</Checkbox>
				</Space>
			</PageHeader>
		);
	} else if (deviceError) {
		return (
			<Result
				status="500"
				title="Failed to fetch device"
				subTitle={String(deviceError)}
			/>
		);
	} else {
		return <Loader subTitle="Loading device..." />;
	}
}

export default Device;

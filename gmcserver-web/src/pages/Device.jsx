import React, { useState, useEffect } from "react";
import { useParams, useHistory } from "react-router-dom";
import { LoadingOutlined } from "@ant-design/icons";
import { fetchDevice, fetchTimeline } from "../GmcApi";
import {
	Checkbox,
	DatePicker,
	Descriptions,
	PageHeader,
	Result,
	Space,
	Spin,
} from "antd";
import DisabledBadge from "../components/DisabledBadge";
import "react-vis/dist/style.css";
import {
	XYPlot,
	XAxis,
	YAxis,
	HorizontalGridLines,
	VerticalGridLines,
	LineSeries,
} from "react-vis";

const { RangePicker } = DatePicker;

const numericRecordFields = [
	"cpm",
	"acpm",
	"usv",
	"co2",
	"hcho",
	"tmp",
	"ap",
	"hmdt",
	"accy",
	"date",
];

function DeviceChart(props) {
	const device = props.device;
	const [timeline, setTimeline] = useState(null);
	const [timelineError, setTimelineError] = useState(null);

	useEffect(() => {
		if (device) {
			fetchTimeline(device.id, props.full, props.start, props.end).then(
				(recs) => {
					let tl = {};
					for (let r of recs) {
						for (let f of numericRecordFields) {
							if (!tl[f]) tl[f] = [];

							tl[f].push({ x: new Date(r.date), y: r[f] });
						}
					}
					setTimeline(tl);
				},
				(err) => setTimelineError(err)
			);
		}
	}, [device, props.full, props.start, props.end]);

	if (timeline) {
		return (
			<XYPlot height={500} width={500}>
				<VerticalGridLines />
				<HorizontalGridLines />
				<LineSeries data={timeline.cpm} />
				<XAxis tickLabelAngle={-90} />
				<YAxis />
			</XYPlot>
		);
	} else if (timelineError) {
		return (
			<Result
				status="500"
				title="Failed to load data"
				subTitle={String(timelineError)}
			/>
		);
	} else {
		return (
			<Result
				subTitle="Loading data..."
				icon={
					<Spin indicator={<LoadingOutlined spin style={{ fontSize: 34 }} />} />
				}
			/>
		);
	}
}

function Device() {
	const history = useHistory();
	const [device, setDevice] = useState(null);
	const [deviceError, setDeviceError] = useState(null);
	const [input, setInput] = useState({});
	const { id } = useParams();

	useEffect(() => {
		fetchDevice(id).then(
			(device) => setDevice(device),
			(error) => setDeviceError(error)
		);
	}, [id]);

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
				<Space direction="vertical">
					<DeviceChart
						device={device}
						full={input.full}
						start={input.start}
						end={input.end}
					/>

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
		return (
			<Result
				subTitle="Loading device..."
				icon={
					<Spin indicator={<LoadingOutlined spin style={{ fontSize: 34 }} />} />
				}
			/>
		);
	}
}

export default Device;

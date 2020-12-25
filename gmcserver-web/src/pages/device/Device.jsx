import React, { useState, useEffect } from "react";
import { useParams, useHistory, Link } from "react-router-dom";
import { fetchDevice, fetchDeviceStats, fetchTimeline } from "../../GmcApi";
import {
	Button,
	Card,
	Checkbox,
	Col,
	DatePicker,
	Descriptions,
	message,
	PageHeader,
	Result,
	Row,
	Select,
	Space,
	Statistic,
} from "antd";
import DisabledBadge from "../../components/DisabledBadge";
import Loader from "../../components/Loader";
import DeviceChart from "../../components/DeviceChart";
import UserPill from "../../components/UserPill";
import { numericRecordFields } from "../../GmcTypes";

const { Option } = Select;
const { RangePicker } = DatePicker;

function Device() {
	const history = useHistory();
	const [device, setDevice] = useState(null);
	const [stats, setStats] = useState({});
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
				(recs) => setTimeline(recs)
				// (err) => setTimelineError(err)
			);
		}
	}, [device, input.full, input.start, input.end]);

	function handleStatsChange(field) {
		fetchDeviceStats(device.id, field).then(
			(stats) => setStats(stats),
			(err) => message.error("Failed to load device stats: " + err)
		);
	}

	if (device) {
		return (
			<PageHeader
				onBack={history.goBack}
				title={device.name}
				subTitle={device.gmcId}
				tags={device.disabled ? <DisabledBadge /> : undefined}
				extra={[
					<Button key="0">
						<Link to={"/device/" + id + "/live"}>
							Live timeline
						</Link>
					</Button>,
				]}
			>
				<Descriptions
					size="small"
					extra={<UserPill user={device.owner} />}
				>
					{device.importedFrom ? (
						<Descriptions.Item label="Imported from">
							{device.importedFrom}
						</Descriptions.Item>
					) : undefined}

					{device.model ? (
						<Descriptions.Item label="Model">
							{device.model}
						</Descriptions.Item>
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
							setInput(
								Object.assign({}, input, {
									full: e.target.checked,
								})
							)
						}
					>
						Full timeline
					</Checkbox>

					<div>
						<Select onChange={handleStatsChange}>
							{numericRecordFields.map((f) => {
								return <Option value={f}>{f}</Option>;
							})}
						</Select>
						<Row gutter={16}>
							<Col span={12}>
								<Statistic
									title="Minimum"
									value={stats.min}
									suffix={stats.unit}
								/>
							</Col>
							<Col span={12}>
								<Statistic
									title="Maximum"
									value={stats.max}
									suffix={stats.unit}
								/>
							</Col>
						</Row>
						<Row gutter={16}>
							<Col span={12}>
								<Statistic
									title="Standard deviation"
									value={stats.stdDev}
								/>
							</Col>
							<Col span={12}>
								<Statistic
									title="Sample size"
									value={stats.sampleSize}
								/>
							</Col>
						</Row>
					</div>
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

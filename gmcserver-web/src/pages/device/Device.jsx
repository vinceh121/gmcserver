import React, { useState, useEffect } from "react";
import { useParams, useHistory, Link } from "react-router-dom";
import { fetchDevice, fetchDeviceStats, fetchTimeline } from "../../GmcApi";
import {
	Button,
	Card,
	Col,
	DatePicker,
	Descriptions,
	Dropdown,
	Menu,
	message,
	PageHeader,
	Result,
	Row,
	Select,
	Space,
	Statistic,
	Tabs,
} from "antd";
import DeviceBadge from "../../components/DeviceBadge";
import Loader from "../../components/Loader";
import DeviceChart from "../../components/DeviceChart";
import UserPill from "../../components/UserPill";
import { exportTypes, numericRecordFields } from "../../GmcTypes";
import DeviceCalendar from "../../components/DeviceCalendar";
import { DownOutlined } from "@ant-design/icons";
import DeviceTable from "../../components/DeviceTable";

const { TabPane } = Tabs;
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
			fetchTimeline(device.id, input.start && input.end, input.start, input.end).then(
				(recs) => setTimeline(recs)
				// (err) => setTimelineError(err)
			);
		}
	}, [device, input.start, input.end]);

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
				tags={device.disabled || device.importedFrom ? <DeviceBadge device={device} /> : undefined}
				extra={[
					<Button key="0">
						<Link to={"/device/" + id + "/live"}>
							Live timeline
						</Link>
					</Button>,
					<Dropdown key="1" overlay={
						<Menu onClick={e => { window.open("/api/v1/device/" + id + "/export/" + e.key) }}>
							{exportTypes.map(t => <Menu.Item key={t}>{t.toUpperCase()}</Menu.Item>)}
						</Menu>
					}>
						<Button>
							Export <DownOutlined />
						</Button>
					</Dropdown>,
					device.own ?
						<Button key="2">
							<Link to={"/device/" + id + "/edit"}>
								Edit
							</Link>
						</Button>
						: undefined
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
				<Tabs defaultActiveKey="1">
					<TabPane tab="Timeline" key="1">
						<Card bodyStyle={{ height: "500px" }} loading={!timeline} style={{ marginBottom: "8px" }}>
							<DeviceChart
								full={input.full}
								start={input.start}
								end={input.end}
								timeline={timeline}
							/>
						</Card>
					</TabPane>
					<TabPane tab="Table" key="2">
						<DeviceTable device={device} timeline={timeline} />
					</TabPane>
					<TabPane tab="Calendar" key="3">
						<Card bodyStyle={{ height: "500px" }} style={{ marginBottom: "8px" }}>
							<DeviceCalendar device={device} />
						</Card>
					</TabPane>
				</Tabs>
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

					<div>
						<Select
							onChange={handleStatsChange}
							style={{ width: "120px" }}
						>
							{numericRecordFields.map((f, i) => {
								return (
									<Option key={i} value={f}>
										{f}
									</Option>
								);
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
								<Statistic title="Average" value={stats.avg} />
							</Col>
						</Row>
						<Row gutter={16}>
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

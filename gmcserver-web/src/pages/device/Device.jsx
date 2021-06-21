import React, { useState, useEffect } from "react";
import { useParams, useHistory, Link } from "react-router-dom";
import { fetchDevice, fetchTimeline } from "../../GmcApi";
import {
	Button,
	Card,
	Checkbox,
	DatePicker,
	Descriptions,
	Dropdown,
	Menu,
	PageHeader,
	Result,
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
import Modal from "antd/lib/modal/Modal";
import RecordView from "../../components/RecordView";
import DeviceStats from "../../components/DeviceStats";
import ColorHash from 'color-hash';

const { TabPane } = Tabs;
const { RangePicker } = DatePicker;
const colorHash = new ColorHash();

function Device() {
	const history = useHistory();
	const [device, setDevice] = useState(null);
	const [deviceError, setDeviceError] = useState(null);
	const [timeline, setTimeline] = useState(null);
	// const [timelineError, setTimelineError] = useState(null);
	const [input, setInput] = useState({});
	const [viewRecord, setViewRecord] = useState();
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

	const MyTimeRange = () => (
		<RangePicker
			showTime onChange={(d) =>
				setInput(
					Object.assign({}, input, {
						start: d[0].toDate(),
						end: d[1].toDate(),
					})
				)
			} />
	);

	if (device) {
		return (
			<>
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
					<Tabs defaultActiveKey="timeline">
						<TabPane tab="Timeline" key="timeline">
							<Card bodyStyle={{ height: "500px" }} loading={!timeline} style={{ marginBottom: "8px" }}>
								<DeviceChart
									full={input.full}
									start={input.start}
									end={input.end}
									hiddenFields={input.hiddenFields}
									timeline={timeline}
								// onClick={(r) => setViewRecord(r)} // sighhh onClick doesn't work with slices
								/>
							</Card>
							<Checkbox.Group
								// options={numericRecordFields}
								defaultValue={numericRecordFields}
								value={input.hiddenFields}
								onChange={(list) => {
									setInput(
										Object.assign({}, input, {
											hiddenFields: list
										})
									)
								}} >
								{numericRecordFields.map((f) =>
									<Checkbox key={f} value={f} style={{ color: colorHash.hex(f) }}>{f.toUpperCase()}</Checkbox>
								)}
							</Checkbox.Group>
							<MyTimeRange />
						</TabPane>
						<TabPane tab="Stats" key="stats">
							<DeviceStats device={device} />
						</TabPane>
						<TabPane tab="Table" key="table">
							<DeviceTable
								device={device}
								timeline={timeline}
								onClick={setViewRecord}
							/>
							<MyTimeRange />
						</TabPane>
						<TabPane tab="Calendar" key="calendar">
							<Card bodyStyle={{ height: "500px" }} style={{ marginBottom: "8px" }}>
								<DeviceCalendar device={device} />
							</Card>
						</TabPane>
					</Tabs>
				</PageHeader>
				<Modal visible={viewRecord} footer={null} onCancel={() => setViewRecord()} width="50vw">
					<RecordView record={viewRecord} />
				</Modal>
			</>
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

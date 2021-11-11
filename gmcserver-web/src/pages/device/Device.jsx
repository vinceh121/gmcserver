/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import React, { useState, useEffect } from "react";
import { useParams, useHistory, useLocation, Link } from "react-router-dom";
import { exportTimeline, fetchDevice, fetchTimeline } from "../../GmcApi";
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
	Spin,
	Space,
	Tooltip
} from "antd";
import DeviceBadge from "../../components/DeviceBadge";
import Loader from "../../components/Loader";
import DeviceChart from "../../components/DeviceChart";
import UserPill from "../../components/UserPill";
import { exportTypes, numericRecordFields } from "../../GmcTypes";
import DeviceCalendar from "../../components/DeviceCalendar";
import { DownOutlined, LinkOutlined, LoadingOutlined } from "@ant-design/icons";
import DeviceTable from "../../components/DeviceTable";
import Modal from "antd/lib/modal/Modal";
import RecordView from "../../components/RecordView";
import DeviceStats from "../../components/DeviceStats";
import ColorHash from 'color-hash';
import moment from "moment";

const { TabPane } = Tabs;
const { RangePicker } = DatePicker;
const colorHash = new ColorHash();

function Device() {
	const history = useHistory();
	const location = useLocation();
	const searchParams = new URLSearchParams(location.search);
	const [device, setDevice] = useState(null);
	const [deviceError, setDeviceError] = useState(null);
	const [timeline, setTimeline] = useState(null);
	// const [timelineError, setTimelineError] = useState(null);
	const [timelineLoading, setTimelineLoading] = useState(false);
	const [input, setInput] = useState({
		start: searchParams.has("start") ? new Date(Number.parseInt(searchParams.get("start")))
			: new Date(new Date().getTime() - 12 * 60 * 60 * 1000), // 12h ago
		end: searchParams.has("end") ? new Date(Number.parseInt(searchParams.get("end"))) : new Date()
	});
	const [viewRecord, setViewRecord] = useState();
	const [exportType, setExportType] = useState();
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
				(recs) => {
					setTimeline(recs);
					setTimelineLoading(false);
				}
				// (err) => setTimelineError(err)
			);
		}
	}, [device, input.start, input.end]);

	const MyTimeRange = () => (
		<Space>
			<RangePicker
				showTime
				onChange={(d) => {
					setInput(
						Object.assign({}, input, {
							start: d[0].toDate(),
							end: d[1].toDate(),
						})
					);
					setTimelineLoading(true);
				}}
				value={[/*i'd like to interject for a */moment(input.start), moment(input.end)]}
			/>
			<Tooltip title="Copy URL to time frame">
				<Button shape="circle" icon={<LinkOutlined />} onClick={() => {
					const href = window.location.href.replace(/\?.*/, "") + "?start=" + input.start.getTime() + "&end=" + input.end.getTime();
					navigator.clipboard.writeText(href);
				}} />
			</Tooltip>
		</Space>
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
							<Menu onClick={e => setExportType(e.key)}>
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
							<Card bodyStyle={{ height: "500px" }} style={{ marginBottom: "8px" }}>
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
							{timelineLoading ? <Spin style={{ paddingRight: "16px" }} indicator={<LoadingOutlined spin style={{ fontSize: 34 }} />} /> : undefined}
							<Card style={{ marginTop: "8px" }}>
								<DeviceStats device={device} start={input.start} end={input.end} />
							</Card>
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
				<Modal visible={exportType} footer={null} onCancel={() => setExportType()} width="50vw">
					<Button onClick={() => exportTimeline(device.id, exportType)}>Export whole timeline</Button>
					<Button onClick={() => exportTimeline(device.id, exportType, input.start, input.end)}>Export current View</Button>
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

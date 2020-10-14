import React, { useState, useEffect } from "react";
import { useParams, useHistory } from "react-router-dom";
import { LoadingOutlined } from "@ant-design/icons";
import { fetchDevice } from "../GmcApi";
import { DatePicker, Descriptions, PageHeader, Result, Spin } from "antd";
import DisabledBadge from "../components/DisabledBadge";
import {
	XYPlot,
	XAxis,
	YAxis,
	HorizontalGridLines,
	LineSeries,
} from "react-vis";

const { RangePicker } = DatePicker;

function Device() {
	const history = useHistory();
	const [state, setState] = useState(null);
	//const [timeline, setTimeline] = useState(null);
	const timeline = [];
	const { id } = useParams();

	useEffect(() => {
		fetchDevice(id).then(
			(device) => setState({ device }),
			(error) => setState({ error })
		);
	}, [id]);

	if (state && state.device) {
		const device = state.device;
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

				<XYPlot></XYPlot>

				<RangePicker showTime />
			</PageHeader>
		);
	} else if (state && state.error) {
		return (
			<Result
				status="500"
				title="Failed to fetch device"
				subTitle={String(state.error)}
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

import { Col, message, Row, Select, Statistic } from "antd";
import React, { useState } from "react";
import { fetchDeviceStats } from "../GmcApi";
import { numericRecordFields } from "../GmcTypes";

const { Option } = Select;

function DeviceStats(props) {
	const device = props.device;
	const [stats, setStats] = useState();

	function handleStatsChange(field) {
		setStats(null);
		fetchDeviceStats(device.id, field).then(
			(stats) => setStats(stats),
			(err) => message.error("Failed to load device stats: " + err)
		);
	}

	return (
		<>
			<Select
				onChange={handleStatsChange}
				style={{ width: "120px" }}
				loading={stats === null}
			>
				{numericRecordFields.map((f, i) => {
					return (
						<Option key={i} value={f}>
							{f}
						</Option>
					);
				})}
			</Select>
			{stats !== undefined && stats !== null ?
				<>
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
				</> : undefined}
		</>
	);
}

export default DeviceStats;
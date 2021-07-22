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
			(stats) => {
				if (stats === null) {
					message.info("Recent records do not posses such data points");
					setStats(undefined);
				} else {
					setStats(stats);
				}
			},
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
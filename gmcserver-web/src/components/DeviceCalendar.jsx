/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import React, { useEffect, useState } from "react";
import { ResponsiveCalendar } from '@nivo/calendar'
import Loader from "./Loader";
import { numericRecordFields } from "../GmcTypes";
import { Button, message, Result, Select } from "antd";
import { ClockCircleTwoTone } from "@ant-design/icons";
import { fetchCalendar } from "../GmcApi";

const { Option } = Select;

function fuckDates(date) {
	const m = (date.getMonth() + 1).toString();
	const d = date.getDate().toString();
	return date.getFullYear() + "-" + (m.length === 1 ? "0" + m : m) + "-" + (d.length === 1 ? "0" + d : d);
}

function DeviceCalendar(props) {
	const [selectedCalendar, setSelectedCalendar] = useState("cpm")
	const [rawCalendar, setRawCalendar] = useState();
	const [bounds, setBounds] = useState();
	const [calendars, setCalendars] = useState({});

	useEffect(() => {
		if (props.device && rawCalendar === undefined) {
			fetchCalendar(props.device.id).then(
				(cal) => setRawCalendar(cal),
				(err) => message.error("Failed to fetch calendar: " + err, 5000)
			);
		}
	}, [props.device, rawCalendar]);

	useEffect(() => {
		if (rawCalendar && rawCalendar.recs && !Object.keys(calendars).length) {
			const cal = {};

			for (let field of numericRecordFields) {
				cal[field] = [];
			}

			for (let rec of rawCalendar.recs) {
				for (let field of numericRecordFields) {
					cal[field].push({ day: fuckDates(new Date(rec.date)), value: rec[field] });
				}
			}

			setCalendars(cal);
		}
	}, [rawCalendar, calendars]);

	useEffect(() => {
		if (
			rawCalendar &&
			rawCalendar.recs &&
			rawCalendar.recs.length &&
			Object.keys(calendars).length &&
			!bounds
		) {
			setBounds({
				min: fuckDates(new Date(rawCalendar.recs[0].date)),
				max: fuckDates(new Date(rawCalendar.recs[rawCalendar.recs.length - 1].date))
			});
		}
	}, [calendars, rawCalendar, bounds]);

	if (rawCalendar && rawCalendar.status === 202) {
		return (
			<Result
				title="The calendar is still loading"
				subTitle="Please check later"
				icon={<ClockCircleTwoTone twoToneColor="#ff5722" />}
				extra={[<Button onClick={() => setRawCalendar()}>Refresh</Button>]} />
		);
	}

	if (rawCalendar && rawCalendar.recs && rawCalendar.recs.length === 0) {
		return (
			<Result status="404"
				title="Couldn't generate calendar"
				subTitle="This device doesn't have enough data to produce a calendar" />
		);
	}

	if (bounds) {
		return (
			<>
				<Select
					onChange={(field) => setSelectedCalendar(field)}
					style={{ width: "120px" }}
					defaultValue="cpm"
				>
					{numericRecordFields.map((f, i) => {
						return (
							<Option key={i} value={f}>
								{f}
							</Option>
						);
					})}
				</Select>
				<ResponsiveCalendar
					data={calendars[selectedCalendar]}
					from={bounds.min}
					to={bounds.max}
					minValue="auto"
					maxValue="auto"
					monthSpacing={16}
					emptyColor="#141414"
					colors={['#61cdbb', '#97e3d5', '#e8c1a0', '#f47560']}
					margin={{ top: 40, right: 40, bottom: 40, left: 40 }}
					yearSpacing={40}
					monthBorderColor="#1f1f1f"
					dayBorderWidth={2}
					dayBorderColor="#1f1f1f"
					theme={{
						textColor: "#fff",
						tooltip: {
							container: { background: "black" },
							basic: { background: "black" }
						}
					}}
					legends={[
						{
							anchor: 'bottom-right',
							direction: 'row',
							translateY: 36,
							itemCount: 4,
							itemWidth: 42,
							itemHeight: 36,
							itemsSpacing: 14,
							itemDirection: 'right-to-left'
						}
					]}
				/>
			</>
		);
	} else {
		return <Loader subTitle="Loading calendar..." />
	}
}

export default DeviceCalendar;
import React, { useEffect, useState } from "react";
import { ResponsiveCalendar } from '@nivo/calendar'
import Loader from "./Loader";
import { numericRecordFields } from "../GmcTypes";
import { Result, Select } from "antd";
import { ClockCircleTwoTone } from "@ant-design/icons";

const { Option } = Select;

function fuckDates(date) {
	const m = date.getMonth().toString();
	const d = date.getDate().toString();
	return date.getFullYear() + "-" + (m.length === 1 ? "0" + m : m) + "-" + (d.length === 1 ? "0" + d : d);
}

function DeviceCalendar(props) {
	const [selectedCalendar, setSelectedCalendar] = useState("cpm")
	const [bounds, setBounds] = useState(null);
	const [calendars, setCalendars] = useState({});

	useEffect(() => {
		if (props.calendar && !Object.keys(calendars).length) {
			const cal = {};

			for (let field of numericRecordFields) {
				cal[field] = [];
			}

			for (let rec of props.calendar.recs) {
				for (let field of numericRecordFields) {
					cal[field].push({ day: fuckDates(new Date(rec.date)), value: rec[field] });
				}
			}

			setCalendars(cal);
		}
	}, [props, props.calendar, calendars]);

	useEffect(() => {
		if (props.calendar.recs.length && Object.keys(calendars).length && !bounds) {
			setBounds({
				min: fuckDates(new Date(props.calendar.recs[0].date)),
				max: fuckDates(new Date(props.calendar.recs[props.calendar.recs.length - 1].date))
			});
		}
	}, [calendars, props.calendar.recs, bounds]);


	if (props.calendar.inProgress) {
		return (
			<Result
				title="The calendar is still loading"
				subTitle="Please check later"
				icon={<ClockCircleTwoTone twoToneColor="#ff5722" />} />
		);
	}

	if (props.calendar.recs.length === 0) {
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
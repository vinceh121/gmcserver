import React, { useEffect, useState } from "react";

import "react-vis/dist/style.css";
import {
	FlexibleXYPlot,
	XAxis,
	YAxis,
	HorizontalGridLines,
	VerticalGridLines,
	LineSeries,
	Crosshair,
} from "react-vis";
import Loader from "./Loader";

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
];

function recordToPoints(rec) {
	const pts = [];
	for (let f in rec) {
		if (f === "date" || !rec[f] || rec[f] === "NaN") continue;
		pts.push({ x: new Date(rec.date), y: rec[f], title: f });
	}
	return pts;
}

function DeviceChart(props) {
	// we need to transform the timeline from 'record' format
	// to 'data point' format to feed react-vis
	const [timeline, setTimeline] = useState(null);
	const [plot, setPlot] = useState({});

	useEffect(() => {
		if (props.timeline) {
			let tl = {};
			for (let r of props.timeline) {
				for (let f of numericRecordFields) {
					if (!tl[f]) tl[f] = [];

					tl[f].push({ x: new Date(r.date), y: r[f] });
				}
			}
			setTimeline(tl);
		}
	}, [props.timeline]);

	if (timeline) {
		return (
			<FlexibleXYPlot
				onMouseLeave={() => setPlot({ crosshairValues: [] })}
			>
				<VerticalGridLines />
				<HorizontalGridLines />
				{numericRecordFields.map((name, i) => {
					return (
						<LineSeries
							data={timeline[name]}
							key={i}
							onNearestX={(value, { index }) => {
								setPlot({
									crosshairValues: recordToPoints(props.timeline[index]),
								});
							}}
						/>
					);
				})}
				<XAxis
					tickLabelAngle={-90}
					tickFormat={(t) => new Date(t).toLocaleString()}
					height={500}
				/>
				<YAxis />
				<Crosshair
					values={plot.crosshairValues}
					itemsFormat={(pts) =>
						pts.map((v) => {
							return { title: v.title, value: v.y };
						})
					}
				/>
			</FlexibleXYPlot>
		);
	} else {
		return <Loader subTitle="Loading timeline..." />;
	}
}

export default DeviceChart;

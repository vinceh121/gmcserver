import React, { useEffect, useState } from "react";

import Loader from "./Loader";
import { ResponsiveLine } from "@nivo/line";
import { numericRecordFields } from "../GmcTypes";

function DeviceChart(props) {
	// we need to transform the timeline from 'record' format
	// to 'data point' format to feed the graph
	const [timeline, setTimeline] = useState(null);

	useEffect(() => {
		if (props.timeline) {
			let usedFields = [];
			for (let r of props.timeline) {
				Object.keys(r)
					.filter(
						(v) =>
							!usedFields.includes(v) &&
							numericRecordFields.includes(v)
					)
					.forEach((v) => usedFields.push(v));
			}

			let tl = {};
			for (let f of usedFields) {
				tl[f] = { id: f, data: [] };
			}

			for (let r of props.timeline) {
				for (let f of usedFields) {
					tl[f].data.push({
						x: r.date,
						y: r[f] !== "NaN" ? r[f] : null,
					});
				}
			}

			setTimeline(Object.values(tl));
		}
	}, [props.timeline]);

	if (timeline) {
		return (
			<ResponsiveLine
				theme={{
					background: "#141414",
					textColor: "#ffffff",
					fontSize: 14,
					tooltip: { container: { background: "#000000" } },
					crosshair: {
						line: { stroke: "#ffffff" },
					},
					axis: {
						domain: {
							line: {
								stroke: "#777777",
								strokeWidth: 1,
							},
						},
						ticks: {
							line: {
								stroke: "#777777",
								strokeWidth: 1,
							},
							text: { fontSize: "9px" },
						},
					},
					grid: {
						line: {
							stroke: "#dddddd",
							strokeWidth: 1,
						},
					},
				}}
				data={timeline}
				margin={{ top: 5, right: 5, bottom: 60, left: 30 }}
				animate={true}
				curve="linear"
				useMesh={true}
				enableSlices="x"
				xScale={{
					type: "linear",
					min: "auto",
					max: "auto",
				}}
				axisLeft={{
					legendOffset: 12,
				}}
				axisBottom={{
					legendOffset: -12,
					format: (v) => {
						const d = new Date(v);
						return (
							d.toLocaleDateString() +
							" " +
							d.toLocaleTimeString()
						);
					},
					tickRotation: -25,
				}}
			/>
		);
	} else {
		return <Loader subTitle="Loading timeline..." />;
	}
}

export default DeviceChart;

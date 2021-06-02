import React, { useEffect, useState } from "react";

import Loader from "./Loader";
import { ResponsiveLine } from "@nivo/line";
import { TableTooltip, Chip } from "@nivo/tooltip";
import { numericRecordFields } from "../GmcTypes";

const SliceTooltip = ({slice, axis}) => {
	const otherAxis = axis === 'x' ? 'y' : 'x'

	const rows = slice.points.map(point => [
		<Chip key="chip" color={point.serieColor} />,
		point.serieId,
		<span key="value">
			{point.data[otherAxis + "Formatted"]}
		</span>,
	]);

	const date = new Date(slice.points[0].data.x);

	rows.unshift([
		<Chip key="chip" color="#ff5722" />,
		"Date",
		<span>{date.toLocaleDateString() + " " + date.toLocaleTimeString()}</span>
	]);

	return (
		<TableTooltip
			rows={rows}
		/>
	)
}

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
				margin={{ top: 5, right: 5, bottom: 90, left: 5 }}
				animate={true}
				curve="linear"
				useMesh={true}
				enableSlices="x"
				// onClick={(p) => props.onClick ? props.onClick(props.timeline[p.index]) : undefined} // sighhh onClick doesn't work with slices
				sliceTooltip={SliceTooltip}
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
				pointSize={4}
				legends={[
					{
						anchor: 'bottom',
						direction: 'row',
						justify: false,
						translateX: 0,
						translateY: 90,
						itemsSpacing: 0,
						itemDirection: 'left-to-right',
						itemWidth: 80,
						itemHeight: 20,
						itemOpacity: 0.75,
						symbolSize: 12,
						symbolShape: 'circle',
						symbolBorderColor: 'rgba(0, 0, 0, .5)',
						effects: [
							{
								on: 'hover',
								style: {
									itemBackground: 'rgba(0, 0, 0, .03)',
									itemOpacity: 1
								}
							}
						]
					}
				]}
			/>
		);
	} else {
		return <Loader subTitle="Loading timeline..." />;
	}
}

export default DeviceChart;

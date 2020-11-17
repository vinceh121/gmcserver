import React from "react";

import "react-vis/dist/style.css";
import {
	XYPlot,
	XAxis,
	YAxis,
	HorizontalGridLines,
	VerticalGridLines,
	LineSeries,
	Crosshair,
} from "react-vis";

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
	"date",
];

function DeviceChart(props) {
    const device = props.device;
	const [timeline, setTimeline] = useState(props.timeline);
	const [timelineError, setTimelineError] = useState(null);
	const [plot, setPlot] = useState({});

	useEffect(() => {
		
	}, [device, props.full, props.start, props.end]);

	if (timeline) {
		return (
			<XYPlot
				height={500}
				width={500}
				onMouseLeave={() => setPlot({ crosshairValues: [] })}
			>
				<VerticalGridLines />
				<HorizontalGridLines />
				{numericRecordFields.map((name, i) => {
					console.log(name);
					return (
						<LineSeries
							data={timeline[name]}
							key={i}
							onNearestX={(value, { index }) =>
								setPlot({
									crosshairValues: timeline[name].map((d) => d[index]),
								})
							}
						/>
					);
				})}
				<XAxis tickLabelAngle={-90} />
				<YAxis />
				<Crosshair values={plot.crosshairValues} />
			</XYPlot>
		);
	} else if (timelineError) {
		return (
			<Result
				status="500"
				title="Failed to load data"
				subTitle={String(timelineError)}
			/>
		);
	} else {
		return <Loader subTitle="Loading timeline..." />;
	}
}
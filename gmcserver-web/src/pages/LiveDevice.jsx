import React, { useState } from "react";
import { Card } from "antd";
import Loader from "../components/Loader";
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

function LiveDevice() {
	const [state, setState] = useState({ records: [] });
	const { id } = useParams();

	const recs = [];
	if (state) {
		return (
			<Card title={"Live view: " + state.name} style={{ margin: "16px" }}>
				<XYPlot width={300} height={300}>
					<HorizontalGridLines />
					<LineSeries color="red" data={state.records} />
					<XAxis title="Date" />
					<YAxis />
				</XYPlot>
			</Card>
		);
	} else {
		return <Loader subTitle="Loading device live view" />;
	}
}

export default LiveDefault;

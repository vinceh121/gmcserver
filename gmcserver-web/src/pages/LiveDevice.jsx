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
	const [state, setState] = useState(null);

	const recs = [];
	if (state) {
		return (
			<Card
				title={"Live view: " + state.name}
				style={{ margin: "16px" }}
			> 
            </Card>
		);
	} else {
		return <Loader />;
	}
}

export default LiveDefault;

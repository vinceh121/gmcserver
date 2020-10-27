import React from "react";
import { LoadingOutlined } from "@ant-design/icons";
import { Result, Spin } from "antd";

function Loader(props) {
	return (
		<Result
			subTitle={props.subTitle}
			icon={
				<Spin indicator={<LoadingOutlined spin style={{ fontSize: 34 }} />} />
			}
		/>
	);
}

export default Loader;

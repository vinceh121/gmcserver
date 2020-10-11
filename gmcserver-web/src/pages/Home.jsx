import React from "react";
import { Skeleton } from "antd";

class Home extends React.Component {
	constructor(props) {
        super(props);
        this.state = {}
        setTimeout(() => {
            this.setState({about: "henlo"}, () => {
                this.render()
            })
        }, 5000);
	}

	render() {
		if (this.state.about) {
			return <p>{this.state.about}</p>;
		} else {
			return <Skeleton active paragraph={{ rows: 4 }} />;
		}
	}
}

export default Home;

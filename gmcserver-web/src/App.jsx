import React from "react";
import "./App.less";
import { BrowserRouter as Router, Switch, Route, Link } from "react-router-dom";
import { Layout, Menu } from "antd";
// import GMCLogo from "./logo.svg";
import Home from "./pages/Home";
import NotFound from "./pages/NotFound";
import User from "./pages/User";
import Device from "./pages/Device";
import GmcMap from "./pages/GmcMap";

const { Footer, Content, Header } = Layout;

function App() {
	return (
		<Router>
			<Layout>
				<Header>
					{/* <GMCLogo /> */}
					<Menu theme="dark" mode="horizontal">
						<Menu.Item>
							<Link to="/">Home</Link>
						</Menu.Item>
						<Menu.Item>
							<Link to="/map">World map</Link>
						</Menu.Item>
						<Menu.Item>
							<Link to="/profile">Profile</Link>
						</Menu.Item>
					</Menu>
				</Header>
				<Content style={{ minHeight: "250px" }}>
					<Switch>
						<Route path="/user/:id">
							<User />
						</Route>
						<Route path="/device/:id">
							<Device />
						</Route>
						<Route path="/map">
							<GmcMap />
						</Route>
						<Route exact path="/">
							<Home />
						</Route>
						<Route path="*">
							<NotFound />
						</Route>
					</Switch>
				</Content>
				<Footer>
					Powered by <a href="https://home.gmc.vinceh121.me">GMCServer</a>{" "}
					&copy; 2020 vinceh121
				</Footer>
			</Layout>
		</Router>
	);
}

export default App;

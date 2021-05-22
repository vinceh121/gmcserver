import React from "react";
import "./App.less";
import { BrowserRouter as Router, Switch, Route, } from "react-router-dom";
import { Layout } from "antd";
// import GMCLogo from "./logo.svg";
import Home from "./pages/Home";
import NotFound from "./pages/NotFound";
import User from "./pages/User";
import GmcMap from "./pages/GmcMap";
import Profile from "./pages/Profile";
import Login from "./pages/auth/Login";
import Register from "./pages/auth/Register";
import Device from "./pages/device/Device";
import LiveDevice from "./pages/device/LiveDevice";
import NewDevice from "./pages/device/NewDevice";

import Welcome from "./pages/Welcome";
import Mfa from "./pages/Mfa";
import ToolBar from "./components/ToolBar";
import EditDevice from "./pages/device/EditDevice";

const { Footer, Content, Header } = Layout;

function App() {
	return (
		<Router>
			<Layout>
				<Header style={{ height: "46px", lineHeight: "46px" }}>
					{/* <GMCLogo /> */}
					<ToolBar />
				</Header>
				<Content style={{ minHeight: "250px" }}>
					<Switch>
						<Route path="/user/:id">
							<User />
						</Route>
						<Route path="/device/:id/live">
							<LiveDevice />
						</Route>
						<Route path="/device/new">
							<NewDevice />
						</Route>
						<Route path="/device/:id/edit">
							<EditDevice />
						</Route>
						<Route path="/device/:id">
							<Device />
						</Route>
						<Route path="/map">
							<GmcMap />
						</Route>
						<Route path="/profile">
							<Profile />
						</Route>
						<Route path="/mfa">
							<Mfa />
						</Route>
						<Route path="/login">
							<Login />
						</Route>
						<Route path="/register">
							<Register />
						</Route>
						<Route path="/welcome">
							<Welcome />
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
					Powered by{" "}
					<a href="https://home.gmc.vinceh121.me">GMCServer</a> &copy;
					2020 - 2021 vinceh121
				</Footer>
			</Layout>
		</Router>
	);
}

export default App;

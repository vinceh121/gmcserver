import React from "react";
import "./App.less";
import { BrowserRouter as Router, Switch, Route, Link } from "react-router-dom";
import { Layout, Menu } from "antd";
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

import { isLoggedin } from "./GmcApi";
import Welcome from "./pages/Welcome";
import Mfa from "./pages/Mfa";

const { Footer, Content, Header } = Layout;

function App() {
	const loggedIn = isLoggedin();
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
						{loggedIn ? (
							<Menu.Item>
								<Link to="/profile">Profile</Link>
							</Menu.Item>
						) : (
							<Menu.Item>
								<Link to="/login">Login</Link>
							</Menu.Item>
						)}
					</Menu>
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
					2020 vinceh121
				</Footer>
			</Layout>
		</Router>
	);
}

export default App;

/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import React from "react";
import "./App.less";
import { BrowserRouter as Router, Switch, Route, } from "react-router-dom";
import { Button, Layout } from "antd";
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
import UserDeletion from "./pages/UserDeletion";
import ProfileEdit from "./pages/ProfileEdit";

import { GithubFilled } from "@ant-design/icons";
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
						<Route path="/profile/edit">
							<ProfileEdit />
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
						<Route path="/accountDeletion">
							<UserDeletion />
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
					<br /><a href="https://github.com/vinceh121/gmcserver"><Button type="text" icon={<GithubFilled />} /></a>
				</Footer>
			</Layout>
		</Router>
	);
}

export default App;

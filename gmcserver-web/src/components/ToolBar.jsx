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

import React, { useEffect, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { apiDispatcher, getStorage, isLoggedin } from "../GmcApi";
import { Menu } from "antd";

function ToolBar() {
	const [loggedIn, setLoggedin] = useState(isLoggedin());
	const [currentTab, setCurrentTab] = useState("home");

	const location = useLocation();

	useEffect(() => {
		apiDispatcher.addEventListener("login", () => setLoggedin(true));
		apiDispatcher.addEventListener("logoff", () => setLoggedin(false));
	}, []);

	useEffect(() => {
		const route = location.pathname.substring(1);
		if (["", "map", "user/" + getStorage().getItem("userId"), "login"].includes(route)) {
			if (route === "") {
				setCurrentTab("home");
			} else {
				setCurrentTab(route);
			}
		} else {
			setCurrentTab(null);
		}
	}, [location.pathname]);

	return (
		<Menu theme="dark" mode="horizontal" selectedKeys={currentTab === null ? [] : [currentTab]}>
			<Menu.Item key="home">
				<Link to="/">Home</Link>
			</Menu.Item>
			<Menu.Item key="map">
				<Link to="/map">World map</Link>
			</Menu.Item>
			{loggedIn ? (
				<Menu.Item key="profile">
					<Link to="/profile">Profile</Link>
				</Menu.Item>
			) : (
				<Menu.Item key="login">
					<Link to="/login">Login</Link>
				</Menu.Item>
			)}
		</Menu>
	);
}

export default ToolBar;
/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import React, { useEffect } from "react";
import { getStorage } from "../GmcApi";
import { useHistory, Link } from "react-router-dom";
import Loader from "../components/Loader";
import { Button, Result } from "antd";

function Profile() {
	const id = getStorage().getItem("userId");
	const history = useHistory();

	useEffect(() => {
		if (id) {
			history.push("/user/" + id);
		}
	}, [history, id]);

	if (id) {
		return <Loader />;
	} else {
		return (
			<Result
				status="404"
				title="Not logged in"
				subTitle="Sorry but you don't seem to be logged in"
				extra={
					<Link to="/login">
						<Button type="primary">Login</Button>
					</Link>
				}
			/>
		);
	}
}

export default Profile;

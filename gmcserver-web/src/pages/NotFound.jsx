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
import { Typography, Divider } from "antd";
import { CoffeeOutlined } from "@ant-design/icons";

const { Title, Paragraph } = Typography;

function NotFound() {
	return (
		<Typography style={{padding: "16px"}}>
			<Title>404: Not found</Title>
			<Divider />
			<Paragraph>
				It looks like you're lost, here's a coffee <CoffeeOutlined />
			</Paragraph>
		</Typography>
	);
}

export default NotFound;

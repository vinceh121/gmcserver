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
import L from "leaflet";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { Button, Card, message, Space } from "antd";
import { fetchMap } from "../GmcApi";
import { Link } from "react-router-dom";
import gmcCpmPin from "../assets/PinBuilder";

function DeviceMarker(props) {
	const device = props.device;
	return (
		<Marker
			position={device.location.reverse()} // we return lon/lat, leaflet wants lat/lon
			icon={L.icon({
				iconSize: [42, 42],
				iconAnchor: [16, 32],
				iconUrl: gmcCpmPin({ device }),
			})}
		>
			<Popup>
				<Space direction="vertical">
					{device.name}<br />
					{device.lastRecord.cpm} CPM<br />
					{device.lastRecord.usv} µSv/h
					<Link to={"/device/" + device.id}>
						<Button type="link">More info</Button>
					</Link>
				</Space>
			</Popup>
		</Marker>
	);
}

function GmcMap() {
	const [devices, setDevices] = useState(null);
	const [input, setInput] = useState(null);

	useEffect(() => {
		if (input && input.swlon) {
			fetchMap(input).then(
				(devs) => setDevices(devs),
				(err) => message.error("Error while loading map: " + String(err))
			);
		}
	}, [input]);

	function updateInput(map) {
		if (map) {
			const bounds = map.getBounds();
			setInput({
					swlon: bounds.getSouthWest().lng,
					swlat: bounds.getSouthWest().lat,
					nelon: bounds.getNorthEast().lng,
					nelat: bounds.getNorthEast().lat
			});
		}
	}

	return (
		<Card
			style={{ margin: "16px" }}
		>
			<MapContainer
				center={[48.743611, 18.930556]}
				zoom={4}
				style={{ height: "85vh", background: "#1f1f1f" }}
				whenReady={(map) => {
					updateInput(map.target);
					map.target.on({
						moveend: () => updateInput(map.target)
					})
				}}
			>
				<TileLayer
					attribution='&amp;copy <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
					url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
				/>
				{devices ? devices.map((dev) => (
					<DeviceMarker key={dev.id} device={dev} />
				)) : undefined}
			</MapContainer>
		</Card >
	);
}

export default GmcMap;

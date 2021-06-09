import React, { useEffect, useState } from "react";
import L from "leaflet";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { Button, Card, message, Result, Space } from "antd";
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
					{device.cpm} CPM
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
		if (input && input.rect) {
			fetchMap(input.rect).then(
				(devs) => setDevices(devs),
				(err) => message.error("Error while loading map: " + String(err))
			);
		}
	}, [input]);

	function updateInput(map) {
		if (map) {
			const bounds = map.getBounds();
			setInput({
				rect: [
					bounds.getSouthWest().lng,
					bounds.getSouthWest().lat,
					bounds.getNorthEast().lng,
					bounds.getNorthEast().lat,
				],
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

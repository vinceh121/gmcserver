import React, { useEffect, useState } from "react";
import L from "leaflet";
import { MapContainer, TileLayer, Marker, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { Button, Card, Result, Space, Spin } from "antd";
import { fetchMap } from "../GmcApi";
import { Link } from "react-router-dom";
import gmcCpmPin from "../assets/PinBuilder";
import { LoadingOutlined } from "@ant-design/icons";

function DeviceMarker(props) {
	const device = props.device;
	return (
		<Marker
			position={device.location.reverse()}
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
	const [loading, setLoading] = useState(true);
	const [devices, setDevices] = useState(null);
	const [error, setError] = useState(null);
	const [input, setInput] = useState(null);

	useEffect(() => {
		if (input && input.rect) {
			setLoading(true);
			fetchMap(input.rect).then(
				(devs) => {
					setDevices(devs); 
					setLoading(false); 
				},
				(err) => setError(err)
			);
		}
	}, [input]);

	function updateInput(map) {
		if (map) {
			const bounds = map.getBounds();
			setInput({
				rect: [
					bounds.getSouthWest().lat,
					bounds.getSouthWest().lng,
					bounds.getNorthEast().lat,
					bounds.getNorthEast().lng,
				],
			});
		}
	}
	if (error) {
		return (
			<Result
				status="500"
				title="Failed to load devices"
				subTitle={String(error)}
			/>
		);
	} else {
		return (
			<Card
				title="World Map"
				style={{ margin: "16px" }}
				extra={
					loading ? (
						<Spin
							indicator={
								<LoadingOutlined
									spin
									style={{ fontSize: 21 }}
								/>
							}
						/>
					) : undefined
				}
			>
				<MapContainer
					center={[48.743611, 18.930556]}
					zoom={4}
					style={{ height: "80vh", background: "#1f1f1f" }}
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
}

export default GmcMap;

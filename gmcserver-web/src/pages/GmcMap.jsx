import React, { createRef, useEffect, useState } from "react";
import L from "leaflet";
import { Map, TileLayer, Marker, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { Button, Card, Result, Space, Spin } from "antd";
import { LoadingOutlined } from "@ant-design/icons";
import { fetchMap } from "../GmcApi";
import { Link } from "react-router-dom";
import gmcCpmPin from "../assets/PinBuilder";

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
					{device.name}<br/>
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
	const [devices, setDevices] = useState([]);
	const [error, setError] = useState(null);
	const [input, setInput] = useState(null);

	const mapRef = createRef();

	useEffect(() => {
		if (input && input.rect)
			fetchMap(input.rect).then(
				(devs) => setDevices(devs),
				(err) => setError(err)
			);
	}, [input]);

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
					devices.length ? undefined : (
						<Spin
							indicator={
								<LoadingOutlined
									spin
									style={{ fontSize: 34 }}
								/>
							}
						/>
					)
				}
			>
				<Map
					ref={mapRef}
					center={[48.743611, 18.930556]}
					zoom={4}
					style={{ height: "500px" }}
					onMoveend={(e) => {
						const map = mapRef.current;
						if (map != null) {
							const bounds = map.leafletElement.getBounds();
							setInput({
								rect: [
									bounds.getSouthWest().lat,
									bounds.getSouthWest().lng,
									bounds.getNorthEast().lat,
									bounds.getNorthEast().lng,
								],
							});
						}
					}}
				>
					<TileLayer
						attribution='&amp;copy <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
						url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
					/>

					{devices.map((dev) => (
						<DeviceMarker key={dev.id} device={dev} />
					))}
				</Map>
			</Card>
		);
	}
}

export default GmcMap;

import React, { useEffect, useState } from "react";
import L from "leaflet";
import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import { Button, Card, Space } from "antd";
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

function DeviceLayer() {
	const [devices, setDevices] = useState([]);
	const [error, setError] = useState(null); // TODO figure out how to use this
	const [input, setInput] = useState(null);

	useEffect(() => {
		if (input && input.rect)
			fetchMap(input.rect).then(
				(devs) => setDevices(devs),
				(err) => setError(err)
			);
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

	const map = useMapEvents({
		moveend: () => updateInput(map)
	});
	if (input === null) {
		updateInput(map);
	}

	return <>
		{
			devices.map((dev) => (
				<DeviceMarker key={dev.id} device={dev} />
			))
		}
	</>
}

function GmcMap() {
	return (
		<Card
			title="World Map"
			style={{ margin: "16px" }}
		// extra={ // TODO: figure out how to do this
		// 	devices.length ? undefined : (
		// 		<Spin
		// 			indicator={
		// 				<LoadingOutlined
		// 					spin
		// 					style={{ fontSize: 34 }}
		// 				/>
		// 			}
		// 		/>
		// 	)
		// }
		>
			<MapContainer
				center={[48.743611, 18.930556]}
				zoom={4}
				style={{ height: "500px" }}
			>
				<TileLayer
					attribution='&amp;copy <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
					url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
				/>

				<DeviceLayer />
			</MapContainer>
		</Card>
	);
}

export default GmcMap;

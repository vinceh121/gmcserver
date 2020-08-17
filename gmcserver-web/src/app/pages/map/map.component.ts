import { Component, OnInit, NgZone } from '@angular/core';
import { tileLayer, latLng, Map, Layer, LatLngBounds, marker, LatLngTuple, icon, MarkerOptions } from 'leaflet';
import { RequestService } from '../../request.service';
import { MapDevice } from '../../types';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

interface DeviceMarkerOptions extends MarkerOptions {
	deviceId: string;
}

@Component({
	selector: 'app-map',
	templateUrl: './map.component.html',
	styleUrls: ['./map.component.scss']
})
export class MapComponent implements OnInit {
	options = {
		layers: [tileLayer('http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 18, attribution: '© OpenStreemap Contributors' })],
		zoom: 3,
		center: latLng(51, 10)
	};

	markers: Layer[] = [];
	map: Map;

	rawSvg: string;

	constructor(private req: RequestService, private router: Router,
		private ngZone: NgZone, private http: HttpClient) { }

	ngOnInit(): void {
		this.http.get('/assets/map/gmc-cpm-pin.svg', { responseType: 'text' }).subscribe(
			(icon: string) => {
				this.rawSvg = icon;
			},
			err => {
				console.error('Failed to get base icon: ' + err);
			}
		)
	}

	fetchDevices(bounds: LatLngBounds): void {
		// lowerLeftX
		// lowerLeftY
		// upperRightX
		// upperRightY
		const rect: number[] = [
			bounds.getSouthWest().lat,
			bounds.getSouthWest().lng,
			bounds.getNorthEast().lat,
			bounds.getNorthEast().lng
		];
		this.req.getMap(rect).subscribe(
			(devs: MapDevice[]) => {
				console.log('got ' + devs.length + ' devices');
				this.markers = [];
				for (const dev of devs) {
					const m = marker(dev.location.reverse() as LatLngTuple, { // fucking mongo doing stuff in reverse
						icon: icon({
							iconSize: [42, 42],
							iconAnchor: [16, 32],
							iconUrl: this.generateIcon(dev)
						}),
						deviceId: dev.id
					} as DeviceMarkerOptions).addEventListener('click', this.onMarkerClick.bind(this));
					this.markers.push(m);
				}
			},
			err => {
				console.error(err);
			}
		);
	}

	generateIcon(dev: MapDevice): string {
		let color: string;

		if (dev.cpm >= 0 && dev.cpm < 50) {
			color = '#4caf50';
		} else if (dev.cpm >= 50 && dev.cpm < 100) {
			color = '#ffeb3b';
		} else if (dev.cpm >= 100 && dev.cpm < 1000) {
			color = '#ffc107';
		} else if (dev.cpm >= 1000 && dev.cpm < 2000) {
			color = '#ff5722';
		} else { // fallback color
			color = '#29b6f6';
		}

		let svg = this.rawSvg.replace('#0066a7', color);
		if (dev.cpm != undefined)
			svg = svg.replace('{{CPM}}', dev.cpm.toString());
		else
			svg = svg.replace('{{CPM}}', '');
		svg = 'data:image/svg+xml,' + encodeURIComponent(svg);
		return svg;
	}

	onMapReady(map: Map) {
		this.map = map;
		this.fetchDevices(this.map.getBounds());
	}

	onMove() {
		this.fetchDevices(this.map.getBounds());
	}

	onMarkerClick(event: any) {
		this.ngZone.run(() => {
			const id: string = event.target.options.deviceId;
			this.router.navigate(['/device/', id]);
		});
	}

}

import { Component, OnInit, NgZone } from '@angular/core';
import { tileLayer, latLng, Map, Layer, LatLngBounds, marker, LatLngTuple, icon, MarkerOptions } from 'leaflet';
import { RequestService } from '../request.service';
import { MapDevice } from '../types';
import { Router } from '@angular/router';

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

  constructor(private req: RequestService, private router: Router, private _ngZone: NgZone) { }

  ngOnInit(): void {
  }

  fetchDevices(bounds: LatLngBounds): void {
    const rect: number[][] = [
      [bounds.getSouthWest().lng, bounds.getSouthWest().lat],
      [bounds.getNorthWest().lng, bounds.getNorthWest().lat],
      [bounds.getNorthEast().lng, bounds.getNorthEast().lat],
      [bounds.getSouthEast().lng, bounds.getSouthEast().lat],
      [bounds.getSouthWest().lng, bounds.getSouthWest().lat]
    ];
    this.req.getMap(rect).subscribe(
      (devs: MapDevice[]) => {
        console.log('got ' + devs.length + ' devices');
        this.markers = [];
        for (let dev of devs) {
          let m = marker(dev.location.reverse() as LatLngTuple, { // fucking mongo doing stuff in reverse
            icon: icon({
              iconSize: [32, 32],
              iconAnchor: [14, 32],
              iconUrl: '/assets/map/place-black-18dp.svg'
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

  onMapReady(map: Map) {
    this.map = map;
    this.fetchDevices(this.map.getBounds());
  }

  onMove() {
    this.fetchDevices(this.map.getBounds());
  }

  onMarkerClick(event: any) {
    this._ngZone.run(() => {
      const id: string = event.target.options.deviceId;
      console.log('router: ' + this.router);
      this.router.navigate(['/device/', id]);
    });
  }

}

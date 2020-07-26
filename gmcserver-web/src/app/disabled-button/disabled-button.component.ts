import { Component, OnInit, Input } from '@angular/core';
import { Device } from '../types';
import { RequestService } from '../request.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
	selector: 'app-disabled-button',
	templateUrl: './disabled-button.component.html',
	styleUrls: ['./disabled-button.component.scss'],
	inputs: ['device']
})
export class DisabledButtonComponent implements OnInit {
	@Input() device: Device;

	constructor(private _req: RequestService, private _snackBar: MatSnackBar) {
	}

	ngOnInit(): void {
	}

	submitToggle() {
		if (!this.device.own) {
			this._snackBar.open('You do not own this device', 'OK');
			return;
		}
		this._req.updateDevice(this.device.id, { disabled: !this.device.disabled }).subscribe(
			res => {
				if (res.changed == 1) {
					this._snackBar.open('The device has been ' + (this.device.disabled ? 'enabled' : 'disabled'), 'OK', { duration: 2000 });
					this.device.disabled = !this.device.disabled;
				} else {
					this._snackBar.open('Something unexpected happened: changed ' + res.changed + ' fields', 'OK', { duration: 5000 });
				}
			},
			err => {
				this._snackBar.open('An error occured: ' + err, 'OK', { duration: 5000 });
			}
		);
	}

}

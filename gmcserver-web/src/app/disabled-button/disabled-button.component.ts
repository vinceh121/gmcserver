import { Component, OnInit, Input } from '@angular/core';
import { Device } from '../types';
import { RequestService } from '../request.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
	selector: 'app-disabled-button',
	templateUrl: './disabled-button.component.html',
	styleUrls: ['./disabled-button.component.scss']
})
export class DisabledButtonComponent implements OnInit {
	@Input() device: Device;

	constructor(private req: RequestService, private snackBar: MatSnackBar) {
	}

	ngOnInit(): void {
	}

	submitToggle() {
		if (!this.device.own) {
			this.snackBar.open('You do not own this device', 'OK');
			return;
		}
		this.req.updateDevice(this.device.id, { disabled: !this.device.disabled }).subscribe(
			res => {
				if (res.changed === 1) {
					this.snackBar.open('The device has been ' + (this.device.disabled ? 'enabled' : 'disabled'), 'OK', { duration: 2000 });
					this.device.disabled = !this.device.disabled;
				} else {
					this.snackBar.open('Something unexpected happened: changed ' + res.changed + ' fields', 'OK', { duration: 5000 });
				}
			},
			err => {
				this.snackBar.open('An error occured: ' + err, 'OK', { duration: 5000 });
			}
		);
	}

}

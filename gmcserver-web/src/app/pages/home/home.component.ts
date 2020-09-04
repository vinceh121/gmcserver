import { Component, OnInit } from '@angular/core';
import { InstanceInfo } from 'src/app/types';
import { RequestService } from 'src/app/request.service';

@Component({
	selector: 'app-home',
	templateUrl: './home.component.html',
	styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
	info: InstanceInfo;

	constructor(private req: RequestService) { }

	ngOnInit(): void {
		this.req.getInstanceInfo().subscribe(
			res => {
				this.info = res;
			},
			err => {
				console.error(err);
			}
		);
	}

}

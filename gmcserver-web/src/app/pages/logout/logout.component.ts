import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { RequestService } from '../../request.service';

@Component({
	selector: 'app-logout',
	templateUrl: './logout.component.html',
	styleUrls: ['./logout.component.scss']
})
export class LogoutComponent implements OnInit {
	loggedout: boolean = false;

	constructor(private req: RequestService, private router: Router) { }

	ngOnInit(): void {
		this.req.logout(); // i know it's sync for now, might do server-side token invalidation later
		this.loggedout = true;
		this.router.navigate(['/']);
	}

}

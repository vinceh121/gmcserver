import { Component, OnInit } from '@angular/core';
import { RequestService } from '../request.service';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { User } from '../types';
@Component({
	selector: 'app-user',
	templateUrl: './user.component.html',
	styleUrls: ['./user.component.scss']
})
export class UserComponent implements OnInit {
	user: User;
	errorMsg: string;

	constructor(private req: RequestService, private route: ActivatedRoute) { }

	ngOnInit(): void {
		this.route.paramMap.subscribe((params: ParamMap) => {
			const id: string = params.get('id');
			this.req.getUser(id).subscribe(
				user => {
					this.user = user;
				},
				err => {
					console.error(err);
					if (err.error instanceof ErrorEvent) {
						this.errorMsg = err.error.error;
					} else {
						this.errorMsg = err.status + ': ' + err.error;
					}
				});
		});
	}

}

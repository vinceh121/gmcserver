import { Component, OnInit } from '@angular/core';
import { RequestService } from '../../request.service';
import { ActivatedRoute, Params } from '@angular/router';
import { User } from 'src/app/types';
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
		this.route.queryParams.subscribe((params: Params) => {
			const id: string = params['id'];
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

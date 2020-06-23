import { Component, OnInit } from '@angular/core';
import { RequestService } from '../request.service';
import { HttpErrorResponse } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
/*import { NgForm } from '@angular/forms';*/

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  username: string;
  password: string;
  progress: boolean = false;

  constructor(private req: RequestService) { }

  ngOnInit(): void {
  }

  executeLogin(): void {
    console.log('lul');
    if (this.username != null && this.password != null
      && this.username.length != 0 && this.password.length != 0)
      this.req.login(this.username, this.password)
      /*.pipe(catchError((e: HttpErrorResponse) => {
        this.progress = false;
      }))*/
      .subscribe((r: any) => {
        console.log(r);
      });
  }
}

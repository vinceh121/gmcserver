import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class RequestService {
  token: string;
  headers: HttpHeaders;

  constructor(private http: HttpClient) {
    this.headers = new HttpHeaders();
    this.headers.set('Authorization', this.token);
  }

  public get<T>(path: string): Observable<T> {
    return this.http.get<T>(path, { headers: this.headers });
  }

  public post<T>(path: string): Observable<T> {
    return this.http.post<T>(path, { headers: this.headers });
  }
}

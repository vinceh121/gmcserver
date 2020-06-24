import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Device, Record } from './types';
import { DRIVERS, Locker } from 'angular-safeguard'

@Injectable({
  providedIn: 'root'
})
export class RequestService {
  baseUrl: string = 'http://127.0.0.1:80/';
  headers: HttpHeaders;

  constructor(private http: HttpClient, private locker: Locker) {
    this.updateHeaders();
  }

  public updateHeaders(): void {
    const tok = this.getToken(); // find out why dynamic setting isn't working
    console.log('token: ' + tok);
    if (tok != null)
      this.headers = new HttpHeaders({ Authorization: tok });
    else
      this.headers = new HttpHeaders();
  }

  public login(username: string, password: string): Observable<any> {
    return this.http.post(this.getPath('auth/login'), { username: username, password: password }, { headers: this.headers });
  }

  public setLoginInfo(id: string, token: string): void {
    this.locker.set(DRIVERS.LOCAL, 'user-id', id);
    this.locker.set(DRIVERS.LOCAL, 'token', token);
    this.updateHeaders();
  }

  public getUserId(): string {
    return this.locker.get(DRIVERS.LOCAL, 'user-id');
  }

  public getToken(): string {
    return this.locker.get(DRIVERS.LOCAL, 'token');
  }

  public checkAuth(): boolean {
    return this.getToken() != null;
  }

  public getDevice(id: string): Observable<Device> {
    return this.get<Device>('device/' + id);
  }

  public getDeviceTimeline(id: string, full: boolean): Observable<Record[]> {
    return this.get<Record[]>('device/' + id + '/timeline' + (full ? '?full=y' : ''));
  }

  public getPath(path: string): string {
    return this.baseUrl + path;
  }

  private get<T>(path: string): Observable<T> {
    return this.http.get<T>(this.baseUrl + path, { headers: this.headers });
  }
}

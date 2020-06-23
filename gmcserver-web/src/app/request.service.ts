import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Device, Record } from './types';

@Injectable({
  providedIn: 'root'
})
export class RequestService {
  token: string = 'NWVkN2Q5YjhkZTIyNjMwYTAxMDVlN2Jm.NDY2MTc2NzM=.4PbIYEheO7y2WtiPOcXm8nSRyl4PLMgr2/SPnjMmuJA'; // TODO remove this
  baseUrl: string = 'http://127.0.0.1:80/';
  headers: HttpHeaders;

  constructor(private http: HttpClient) {
    this.updateHeaders();
  }

  public updateHeaders(): void {
    this.headers = new HttpHeaders();
    this.headers.append('Authorization', this.token);
  }

  public login(username: string, password: string): Observable<any> {
    return this.http.post(this.getPath('auth/login'), { username: username, password: password }, { headers: this.headers });
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
    console.log(this.headers);
    return this.http.get<T>(this.baseUrl + path, { headers: this.headers });
  }
}

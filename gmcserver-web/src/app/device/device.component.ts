import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Device, Record } from '../types';
import { RequestService } from '../request.service';

@Component({
  selector: 'app-device',
  templateUrl: './device.component.html',
  styleUrls: ['./device.component.scss']
})
export class DeviceComponent implements OnInit {
  public device: Device;
  public fullTimeline: boolean;

  constructor(private req: RequestService, private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe((params: ParamMap) => {
      const id: string = params.get('id');
      this.req.get<Device>('device/' + id).subscribe((dev: Device) => this.device = dev);
    });
  }

  public fetchTimeline(): void {
    this.req.get<Record[]>('device/timeline' + (this.fullTimeline ? '?full=y' : '')).subscribe((records: Record[]) => this.device.timeline = records);
  }

}

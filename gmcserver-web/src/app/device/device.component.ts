import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Sort } from '@angular/material/sort';
import { ChartDataSets, ChartOptions } from 'chart.js';
import { Device, Record } from '../types';
import { RequestService } from '../request.service';
import { Label } from 'ng2-charts';

@Component({
  selector: 'app-device',
  templateUrl: './device.component.html',
  styleUrls: ['./device.component.scss']
})
export class DeviceComponent implements OnInit {
  public device: Device;
  public fullTimeline: boolean = false;
  sortedData: Record[] = [];

  chartData: ChartDataSets[] = [
    { label: 'CPM' },
    { label: 'ACPM' },
    { label: 'µSv', yAxisID: 'usv' }
  ]

  chartLabels: any[] = []

  chartOptions: any = {
    scales: {
      yAxes: [
        {
          id: 'main',
          type: 'linear',
          position: 'left'
        },
        {
          id: 'usv',
          type: 'linear',
          position: 'right'
        }
      ]
    }
  }

  constructor(private req: RequestService, private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe((params: ParamMap) => {
      const id: string = params.get('id');
      this.req.getDevice(id).subscribe((dev: Device) => {
	      this.device = dev;
	      this.fetchTimeline();
      });
    });
  }

  public fetchTimeline(): void {
    console.log('full = '+this.fullTimeline);
    this.req.getDeviceTimeline(this.device.id, this.fullTimeline).subscribe((records: any) => {
      this.device.timeline = records.records;
      this.sortedData = records.records;
      this.buildChart();
    });
  }

  buildChart(): void { // TODO find a better way to do that
    this.chartData[0].data = this.device.timeline.map(r => r.cpm);
    this.chartData[1].data = this.device.timeline.map(r => r.acpm);
    this.chartData[2].data = this.device.timeline.map(r => r.usv);
    this.chartLabels = this.device.timeline.map(r => new Date(r.date).toLocaleString());
  }

  sort(sort: Sort): void {
    const data = this.device.timeline;
    if (!sort.active || sort.direction === '') {
      this.sortedData = data;
      return;
    }

    this.sortedData = data.sort((a, b) => {
      const isAsc = sort.direction === 'asc';
      switch (sort.active) {
        case 'date': return compare(a.date, b.date, isAsc);
        case 'cpm': return compare(a.cpm, b.cpm, isAsc);
        case 'acpm': return compare(a.cpm, b.cpm, isAsc);
        case 'usv': return compare(a.usv, b.usv, isAsc);
        default: return 0;
      }
    });
  }
}

function compare(a: number | string | Date, b: number | string | Date, isAsc: boolean) {
  return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
}

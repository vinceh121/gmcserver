import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ChartDataSets, ChartOptions } from 'chart.js';
import { Device, Record } from '../types';
import { RequestService } from '../request.service';
import { MatDatepickerInputEvent } from '@angular/material/datepicker';

@Component({
	selector: 'app-device',
	templateUrl: './device.component.html',
	styleUrls: ['./device.component.scss']
})
export class DeviceComponent implements OnInit {
	public device: Device;
	public fullTimeline = false;
	displayedColumns = ['date', 'cpm', 'acpm', 'usv'];
	tableData: MatTableDataSource<Record> = new MatTableDataSource();
	errorMsg: string = null;
	startDate: Date;
	endDate: Date;

	lineTension = 0.1;

	chartData: ChartDataSets[] = [
		{ label: 'CPM', lineTension: this.lineTension, data: [] },
		{ label: 'ACPM', lineTension: this.lineTension, data: [] },
		{ label: 'µSv', yAxisID: 'usv', lineTension: this.lineTension, data: [] }
	];

	chartLabels: any[] = [];

	chartOptions: ChartOptions = {
		spanGaps: false,
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
			],
			xAxes: [
				{
					type: 'time',
					position: 'bottom',
					time: {
						unit: 'minute',
						tooltipFormat: 'HH:mm:ss D/M/YYYY',
						displayFormats: {
							minute: 'll H:mm'
						}
					}
				}
			]
		}
	};

	@ViewChild(MatSort, { static: true }) sort: MatSort = new MatSort();

	constructor(private req: RequestService, private route: ActivatedRoute) {
	}

	ngOnInit(): void {
		this.route.paramMap.subscribe((params: ParamMap) => {
			const id: string = params.get('id');
			this.req.getDevice(id).subscribe((dev: Device) => {
				this.device = dev;
				if (this.device.own) {
					this.displayedColumns.push('ip');
				}
				this.fetchTimeline();
			}, err => {
				console.error(err);
				if (err.error instanceof ErrorEvent) {
					this.errorMsg = err.error.error;
				} else {
					this.errorMsg = err.status + ': ' + err.error.description;
				}
			});
		});
	}

	public fetchTimeline(): void {
		this.req.getDeviceTimeline(this.device.id, this.fullTimeline, this.startDate, this.endDate).subscribe((records: any) => {
			this.device.timeline = records.records;
			this.tableData.data = this.device.timeline;
			this.tableData.sort = this.sort;
			this.buildChart();
		}, err => {
			console.error(err);
			if (err.error instanceof ErrorEvent) {
				this.errorMsg = err.error.error;
			} else {
				this.errorMsg = err.status + ': ' + err.error.description;
			}
		});
	}

	buildChart(): void { // TODO find a better way to do that
		this.chartData[0].data = this.device.timeline.map(r => r.cpm);
		this.chartData[1].data = this.device.timeline.map(r => r.acpm);
		this.chartData[2].data = this.device.timeline.map(r => r.usv);
		this.chartLabels = this.device.timeline.map(r => new Date(r.date));
	}

	startChange(event: MatDatepickerInputEvent<Date>) {
		this.startDate = event.value;
		this.fetchTimeline();
	}

	endChange(event: MatDatepickerInputEvent<Date>) {
		this.endDate = event.value;
		this.fetchTimeline();
	}
}

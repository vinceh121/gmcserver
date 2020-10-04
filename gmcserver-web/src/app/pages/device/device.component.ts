import { Component, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';
import { MatSort } from '@angular/material/sort';
import { MatTableDataSource } from '@angular/material/table';
import { ChartDataSets, ChartOptions } from 'chart.js';
import { Device, Record } from 'src/app/types';
import { RequestService } from '../../request.service';
import { MatDatepickerInputEvent } from '@angular/material/datepicker';
import zoomPlugin from 'chartjs-plugin-zoom';

const TICK_COLOR = '#b3b3b3';

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

	chartOptions: (ChartOptions & {zoom: any}) = {
		spanGaps: false,
		tooltips: {
			mode: 'index'
		},
		legend: {
			labels: {
				fontColor: 'white'
			}
		},
		scales: {
			yAxes: [
				{
					id: 'main',
					type: 'linear',
					position: 'left',
					ticks: { fontColor: TICK_COLOR }
				},
				{
					id: 'usv',
					type: 'linear',
					position: 'right',
					ticks: { fontColor: TICK_COLOR }
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
					},
					ticks: { fontColor: TICK_COLOR }
				}
			]
		},
		zoom: {
			pan: {
				enabled: true,
				mode: 'x',
				onPanComplete: this.fetchTimeline,
				rangeMin: {
					x: null,
					y: null
				},
				rangeMax: {
					x: null,
					y: null
				}
			},
			zoom: {
				enabled: true,
				//drag: true,
				modle: 'x',
				onZoomComplete: function({ chart }) { console.log(`I was zoomed!!!`); },
				rangeMin: {
					x: null,
					y: null
				},
				rangeMax: {
					x: null,
					y: null
				}
			}
		}
	};

	chartPlugins = [zoomPlugin]

	@ViewChild(MatSort, { static: true }) sort: MatSort = new MatSort();

	constructor(private req: RequestService, private route: ActivatedRoute) {
		console.log('lol: '+JSON.stringify(zoomPlugin))
	}

	ngOnInit() {
		this.route.params.subscribe((params: Params) => {
			const id: string = params.id;
			this.req.getDevice(id).subscribe(dev => {
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
					this.errorMsg = err.status + ': ' + err.error;
				}
			});
		});
	}

	public fetchTimeline(): void {
		this.req.getDeviceTimeline(this.device.id, this.fullTimeline, this.startDate, this.endDate).subscribe((records: any) => {
			this.device.timeline = records.records;
			for (const r of this.device.timeline) { // come on ts you should cast that easly
				r.date = new Date(r.date);
			}
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
		this.chartLabels = this.device.timeline.map(r => r.date);
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

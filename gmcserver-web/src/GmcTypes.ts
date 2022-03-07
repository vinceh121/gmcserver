/*
 * GMCServer, lightweight service to log, analyze and proxy Geiger counter data.
 * Copyright (C) 2020 Vincent Hyvert
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

export interface Record {
	date: Date;
	cpm: number;
	acpm: number;
	usv: number;
}

export interface MapDevice {
	id: string;
	name?: string;
	location?: number[];
	cpm: number;
}

export interface Device extends MapDevice {
	name?: string;
	owner: User;
	own?: boolean;
	disabled: boolean;
	gmcId?: number;
	model?: string;
	importedFrom?: string;
	timeline?: Record[];
	proxiesSettings: object;
}

export interface Intent {
	name: string;
	extras: any;
}

export interface User {
	id: string;
	username: string;
	admin: boolean;
	deviceLimit?: number;
	devices?: Device[];
	gmcId?: number;
	mfa?: boolean;
	self?: boolean;
}

export interface InstanceInfo {
	host?: string;
	name?: string;
	about?: string;
	captcha: boolean;
}

export interface LoginResult {
	token: string;
	id: string;
	mfa?: boolean;
}

export interface ErrorResult extends Error {
	status: number;
	description: string;
	extras?: any;
}

export interface MfaStartSetupResponse {
	mfaUri: string;
}

export interface DeviceStats {
	field: string;
	device: string;
	avg: number;
	min: number;
	max: number;
	stdDev: number;
	sampleSize: number;
}

export interface DeviceCalendar {
	id: string;
	deviceId: string;
	lastCalculationDate: Date;
	recs: Record[];
	inProgress: boolean;
}

export interface DeviceUpdate {
	changed: number;
}

export interface UserUpdateParams {
	username?: string;
	email?: string;
	currentPassword?: string;
	newPassword?: string;
}

export interface ImportStarted {
	deviceId?: string;
}

export interface MapRequest {
	swlon: number;
	swlat: number;
	nelon: number;
	nelat: number;
}

export const numericRecordFields = [
	"cpm",
	"acpm",
	"usv",
	"co2",
	"hcho",
	"tmp",
	"ap",
	"hmdt",
	"accy",
];

export const exportTypes = [
	"csv"
]

export const proxySettingsModels = {
	GmcmapProxy: {
		userId: "number",
		deviceId: "number"
	},
	RadmonProxy: {
		user: "string",
		password: "string"
	},
	SafecastProxy: {
		deviceId: "number",
		apiKey: "string"
	}
}

export const platformImports = [
	{ name: "gmcmap", displayName: "gmcmap.com", fields: { gmcmapId: "number" } },
	{ name: "safecast", displayName: "Safecast", fields: { safecastId: "number" } },
	{ name: "uradmonitor", displayName: "uRadMonitor", fields: { uradmonitorId: "string" } },
	{ name: "radmon", displayName: "Radmon", fields: { radmonUsername: "string" } }
];

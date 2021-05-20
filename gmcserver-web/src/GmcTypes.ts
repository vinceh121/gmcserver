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
	mfa: boolean;
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

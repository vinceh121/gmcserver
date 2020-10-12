export interface Record {
	date: Date;
	cpm: number;
	acpm: number;
	usv: number;
}

export interface MapDevice {
	id: string;
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
	deviceLimit: number;
	devices?: Device[];
	gmcId?: number;
	mfa?: boolean;
	self?: boolean;
}

export interface InstanceInfo {
	host?: string;
	name?: string;
	about?: string;
}

export interface LoginResult {
	token: string;
	id: string;
	mfa: boolean;
}

export interface ErrorResult {
	status: number;
	description: string;
	extras?: any;
}

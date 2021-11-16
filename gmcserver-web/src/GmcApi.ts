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

import {
	LoginResult,
	ErrorResult,
	InstanceInfo,
	User,
	Device,
	Record,
	MapDevice,
	MfaStartSetupResponse,
	DeviceStats,
	DeviceCalendar,
	DeviceUpdate,
	UserUpdateParams,
	ImportStarted,
} from "./GmcTypes";

const baseUrl: string = "/api/v1";
const storage: Storage = window.localStorage;

export const apiDispatcher = new EventTarget();

export const request = async (
	ref: string,
	options?: RequestInit
): Promise<Response> => {
	if (!options) {
		options = {};
	}
	if (!options.headers) {
		options.headers = {};
	}

	const token = getStorage().getItem("token");
	if (token) {
		(options.headers as any)["Authorization"] = token;
	}

	const res = await fetch(baseUrl + ref, options);
	return res;
};

export const getStorage = (): Storage => {
	return storage;
};

export const isLoggedin = (): boolean => {
	return getStorage().getItem("token") != null;
};

export const saveSession = (login: LoginResult): void => {
	storage.setItem("userId", login.id);
	storage.setItem("token", login.token);
	storage.setItem("mfaRequired", String(login.mfa));
};

export const login = async (
	username: string,
	password: string
): Promise<LoginResult> => {
	const res = await request("/auth/login", {
		method: "POST",
		body: JSON.stringify({ username, password }),
	});

	if (res.status === 403) {
		// XXX Figure out why this variable declaration is necessary for Linter/Typescript
		//	to understand that it is an error object
		const errres = (await res.json()) as ErrorResult;
		throw errres;
	} else if (res.status !== 200) {
		throw new Error("Login failed: " + res.status + ": " + res.statusText);
	}

	apiDispatcher.dispatchEvent(new Event("login"));

	const login = (await res.json()) as LoginResult;
	saveSession(login);
	return login;
};

export const logoff = (): void => {
	apiDispatcher.dispatchEvent(new Event("logoff"));
	storage.removeItem("userId");
	storage.removeItem("token");
	storage.removeItem("mfaRequired");
};

export const register = async (
	username: string,
	email: string,
	password: string,
	captchaAnswer: string,
	captchaId: string
): Promise<LoginResult> => {
	const res = await request("/auth/register", {
		method: "POST",
		body: JSON.stringify({ username, email, password, captchaAnswer, captchaId }),
	});

	if (res.status !== 200) {
		const errres = (await res.json()) as ErrorResult;
		throw errres;
	}

	apiDispatcher.dispatchEvent(new Event("login"));

	const login = (await res.json()) as LoginResult;
	saveSession(login);
	return login;
};

export const mfaStartSetup = async (): Promise<MfaStartSetupResponse> => {
	const res = await request("/auth/mfa", { method: "PUT", body: "{}" });
	return (await res.json()) as MfaStartSetupResponse;
};

export const mfaFinishSetup = async (pass: number): Promise<object> => {
	const res = await request("/auth/mfa", {
		method: "PUT",
		body: JSON.stringify({ pass }),
	});
	return (await res.json()) as object;
};

export const mfaSubmit = async (pass: number): Promise<LoginResult> => {
	const res = await request("/auth/mfa", {
		method: "POST",
		body: JSON.stringify({ pass })
	});

	apiDispatcher.dispatchEvent(new Event("login"));

	const mfa = (await res.json()) as LoginResult;
	saveSession(mfa);
	return mfa;
};

export const mfaDisable = async (pass: number): Promise<object> => {
	const res = await request("/auth/mfa", {
		method: "DELETE",
		body: JSON.stringify({ pass }),
	});
	const obj = (await res.json()) as object;
	if (res.status !== 200) {
		throw obj;
	}
	return obj;
};

export const updateMe = async (params: UserUpdateParams): Promise<ErrorResult> => {
	const res = await request("/user/me", {
		method: "PUT",
		body: JSON.stringify(params)
	});
	const obj = (await res.json()) as ErrorResult;
	if (res.status !== 200) {
		throw obj;
	}
	return obj;
};

export const deleteMe = async (password: string): Promise<ErrorResult> => {
	const res = await request("/user/me", {
		method: "DELETE",
		body: JSON.stringify({ password })
	});

	const obj = (await res.json()) as ErrorResult;
	if (res.status !== 200) {
		throw obj;
	}
	return obj;
};

export const fetchInstanceInfo = async (): Promise<InstanceInfo> => {
	const res = await request("/instance/info");
	return (await res.json()) as InstanceInfo;
};

export const fetchUser = async (id: string): Promise<User> => {
	const res = await request("/user/" + id);
	return (await res.json()) as User;
};

export const fetchMe = async (): Promise<User> => {
	//return fetchUser("me");
	const id = getStorage().getItem("userId");
	if (id == null) {
		throw new Error("Cannot fetch self: not logged it");
	}
	return fetchUser(id);
};

export const fetchDevice = async (id: string): Promise<Device> => {
	const res = await request("/device/" + id);
	return (await res.json()) as Device;
};

export const fetchDeviceStats = async (
	id: string,
	field: string,
	start: Date | undefined,
	end: Date | undefined
): Promise<DeviceStats | null> => {
	const params: URLSearchParams = new URLSearchParams();
	if (start) {
		params.append("start", String(start.getTime()));
	}
	if (end) {
		params.append("end", String(end.getTime()));
	}

	const res = await request("/device/" + id + "/stats/" + field + "?" + params.toString());
	if (res.status === 204) {
		return null;
	}
	return (await res.json()) as DeviceStats;
};

export const createDevice = async (
	name: string,
	lat: number,
	lon: number
): Promise<Device> => {
	const res = await request("/device", {
		method: "POST",
		headers: {
			"Content-Type": "application/json",
		},
		body: JSON.stringify({ name: name, position: [lat, lon] }),
	});
	return (await res.json()) as Device;
};

export const updateDevice = async (
	id: string,
	data: Device
): Promise<DeviceUpdate> => {
	const res = await request("/device/" + id, {
		method: "PUT",
		headers: {
			"Content-Type": "application/json"
		},
		body: JSON.stringify(data)
	});
	if (res.status !== 200) {
		throw Error(res.statusText + ": " + (await res.json()).description);
	}
	return (await res.json()) as DeviceUpdate;
};

export const disableDevice = async (id: string, del: boolean): Promise<ErrorResult> => {
	const res = await request("/device/" + id, {
		method: "DELETE",
		body: JSON.stringify({ delete: del })
	});

	if (res.status !== 200) {
		throw Error(res.statusText + ": " + (await res.json()).description);
	}

	return (await res.json()) as ErrorResult;
};

export const fetchTimeline = async (
	id: string,
	full: boolean,
	start: Date | undefined,
	end: Date | undefined
): Promise<Record[]> => {
	const params: URLSearchParams = new URLSearchParams();
	if (full) {
		params.append("full", "y");
	}
	if (start) {
		params.append("start", String(start.getTime()));
	}
	if (end) {
		params.append("end", String(end.getTime()));
	}

	const res = await request(
		"/device/" + id + "/timeline?" + params.toString()
	);
	return (await res.json()) as Record[];
};

export const fetchCalendar = async (id: string): Promise<DeviceCalendar> => {
	const res = await request("/device/" + id + "/calendar");
	return (await res.json()) as DeviceCalendar;
};

export const fetchMap = async (rect: number[]): Promise<MapDevice[]> => {
	const res = await request("/map/" + JSON.stringify(rect));
	return (await res.json()) as MapDevice[];
};

export const fetchCaptcha = async (): Promise<String> => {
	const res = await request("/captcha");
	return ((await res.json()) as any).id;
};

export const exportTimeline = async (id: string, type: string, start: Date | undefined, end: Date | undefined) => {
	let url = undefined;
	if (start && end) {
		url = "/api/v1/device/" + id + "/export/" + type + "?start=" + start.getTime() + "&end=" + end.getTime();
	} else {
		url = "/api/v1/device/" + id + "/export/" + type;
	}
	window.open(url, "_blank");
};

export const importDevice = async (platform: string, options: object): Promise<ImportStarted> => {
	const res = await request("/import/" + platform, {
		method: "POST",
		body: JSON.stringify(options)
	});
	return ((await res.json()) as ImportStarted);
};

export const openLiveTimeline = (id: string): WebSocket => {
	return new WebSocket(
		"wss://" + window.location.host + baseUrl + "/device/" + id + "/live"
	); // TODO is this good?
	//	return new WebSocket("wss://gmc.vinceh121.me/api/v1/device/" + id + "/live"); // fuck you proxy dev server not supporting websockets
};

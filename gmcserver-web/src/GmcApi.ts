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
} from "./GmcTypes";

const baseUrl: string = "/api/v1";
const storage: Storage = window.localStorage;

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

	const login = (await res.json()) as LoginResult;
	saveSession(login);
	return login;
};

export const logoff = (): void => {
	storage.removeItem("userId");
	storage.removeItem("token");
	storage.removeItem("mfaRequired");
};

export const register = async (
	username: string,
	email: string,
	password: string
): Promise<LoginResult> => {
	const res = await request("/auth/register", {
		method: "POST",
		body: JSON.stringify({ username, email, password }),
	});

	if (res.status !== 200) {
		const errres = (await res.json()) as ErrorResult;
		throw errres;
	}

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
	field: string
): Promise<DeviceStats> => {
	const res = await request("/device/" + id + "/stats/" + field);
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

export const fetchTimeline = async (
	id: string,
	full: boolean,
	start: Date,
	end: Date
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

export const fetchMap = async (rect: number[]): Promise<MapDevice[]> => {
	const res = await request("/map/" + JSON.stringify(rect));
	return (await res.json()) as MapDevice[];
};

export const openLiveTimeline = (id: string): WebSocket => {
	return new WebSocket(
		"wss://" + window.location.host + baseUrl + "/device/" + id + "/live"
	); // TODO is this good?
	//	return new WebSocket("wss://gmc.vinceh121.me/api/v1/device/" + id + "/live"); // fuck you proxy dev server not supporting websockets
};

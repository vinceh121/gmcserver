import {
	LoginResult,
	ErrorResult,
	InstanceInfo,
	User,
	Device,
	Record,
	MapDevice,
} from "./GmcTypes";

const baseUrl: string = "/api/v1";
const storage: Storage = window.localStorage;

export const request = async (
	ref: string,
	options?: any
): Promise<Response> => {
	if (!options) {
		options = {};
	}
	if (!options.headers) {
		options.headers = {};
	}

	const token = getStorage().getItem("token");
	if (token) {
		options.headers.Authorization = token;
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
	storage.setItem("userId", login.id);
	storage.setItem("token", login.token);
	storage.setItem("mfaRequired", String(login.mfa));
	return login;
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
	return fetchUser("me");
};

export const fetchDevice = async (id: string): Promise<Device> => {
	const res = await request("/device/" + id);
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

	const res = await request("/device/" + id + "/timeline?" + params.toString());
	return (await res.json()) as Record[];
};

export const fetchMap = async (rect: number[]): Promise<MapDevice[]> => {
	const res = await request("/map/" + JSON.stringify(rect));
	return (await res.json()) as MapDevice[];
};

export const openLiveTimeline = (id: string): WebSocket => {
	return new WebSocket(baseUrl + "/" + id + "/live");
};

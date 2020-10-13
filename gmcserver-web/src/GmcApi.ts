import { LoginResult, ErrorResult, InstanceInfo, User } from "./GmcTypes";

const baseUrl: string = "https://gmc.vinceh121.me/api/v1";
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

	const token = storage.getItem("token");
	if (token) {
		options.headers.Authorization = token;
	}

	const res = await fetch(baseUrl + ref, options);
	return res;
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
		throw (await res.json()) as ErrorResult;
	} else if (res.status !== 200) {
		throw "Login failed: " + res.status + ": " + res.statusText;
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

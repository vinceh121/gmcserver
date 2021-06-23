const CracoLessPlugin = require("craco-less");

const config = {
	devServer: {
		proxy: {
			"/api/v1": {
				target: "http://localhost:8081",
				headers: {
					host: "gmc.vinceh121.me",
				},
				secure: false,
			},
		},
	},
	plugins: [
		{
			plugin: CracoLessPlugin,
			options: {
				lessLoaderOptions: {
					lessOptions: {
						modifyVars: { "@primary-color": "#ff5722" },
						javascriptEnabled: true,
					},
				},
			},
		},
	],
};

module.exports = config;

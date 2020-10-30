import cpmPin from "./GmcCpmPin.svg";

let cpmPinSvg;

fetch(cpmPin).then((res) => res.text().then((text) => (cpmPinSvg = text)));

function getColor(cpm) {
	if (cpm >= 0 && cpm < 50) {
		return "#4caf50";
	} else if (cpm >= 50 && cpm < 100) {
		return "#ffeb3b";
	} else if (cpm >= 100 && cpm < 1000) {
		return "#ffc107";
	} else if (cpm >= 1000 && cpm < 2000) {
		return "#ff5722";
	} else {
		// fallback color
		return "#29b6f6";
	}
}

function gmcCpmPin(props) {
	const device = props.device;
	return (
		"data:image/svg+xml," +
		encodeURIComponent(
			cpmPinSvg
				.replace("{color}", getColor(device.cpm))
				.replace("{cpm}", device.cpm)
		)
	);
}

export default gmcCpmPin;

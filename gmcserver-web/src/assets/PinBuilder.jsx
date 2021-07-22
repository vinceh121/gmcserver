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
	const cpm = device.cpm ? device.cpm : "";
	return (
		"data:image/svg+xml," +
		encodeURIComponent(
			cpmPinSvg
				.replace("{color}", getColor(device.cpm))
				.replace("{cpm}", cpm)
		)
	);
}

export default gmcCpmPin;

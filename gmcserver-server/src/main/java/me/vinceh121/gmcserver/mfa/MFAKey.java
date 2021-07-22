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
package me.vinceh121.gmcserver.mfa;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.codec.binary.Base32;

public class MFAKey {
	private byte[] key;
	private String algorithm;
	private int digits, period;

	public byte[] getKey() {
		return this.key;
	}

	public void setKey(final byte[] key) {
		this.key = key;
	}

	public String getAlgorithm() {
		return this.algorithm;
	}

	public void setAlgorithm(final String algorithm) {
		this.algorithm = algorithm;
	}

	public int getDigits() {
		return this.digits;
	}

	public void setDigits(final int digits) {
		this.digits = digits;
	}

	public int getPeriod() {
		return this.period;
	}

	public void setPeriod(final int period) {
		this.period = period;
	}

	public String toURI(final String accountName) {
		try {
			return "otpauth://totp/" + URLEncoder.encode(accountName, "UTF-8") + "?algorith=" + this.algorithm
					+ "&digits=" + this.digits + "&period=" + this.period + "&secret="
					+ new Base32().encodeToString(this.key);
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}

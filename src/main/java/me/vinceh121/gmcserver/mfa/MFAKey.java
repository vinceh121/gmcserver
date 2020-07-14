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
			return "otpauth://totp/"
					+ URLEncoder.encode(accountName, "UTF-8")
					+ "?algorith="
					+ this.algorithm
					+ "&digits="
					+ this.digits
					+ "&period="
					+ this.period
					+ "&secret="
					+ new Base32().encodeToString(this.key);
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}

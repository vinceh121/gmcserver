package me.vinceh121.gmcserver.mfa;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.codec.binary.Base32;

public class MFAKey {
	private byte[] key;
	private String algorithm;
	private int digits, period;

	public byte[] getKey() {
		return key;
	}

	public void setKey(byte[] key) {
		this.key = key;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
	}

	public int getDigits() {
		return digits;
	}

	public void setDigits(int digits) {
		this.digits = digits;
	}

	public int getPeriod() {
		return period;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public String toURI(final String accountName) {
		try {
			return "otpauth://totp/"
					+ URLEncoder.encode(accountName, "UTF-8")
					+ "?algorith="
					+ algorithm
					+ "&digits="
					+ digits
					+ "&period="
					+ period
					+ "&secret="
					+ new Base32().encodeToString(key);
		} catch (final UnsupportedEncodingException e) {
			throw new IllegalStateException(e);
		}
	}
}

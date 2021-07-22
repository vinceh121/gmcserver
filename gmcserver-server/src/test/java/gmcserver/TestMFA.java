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
package gmcserver;

import java.security.InvalidKeyException;
import java.time.Instant;
import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.entities.User;
import me.vinceh121.gmcserver.mfa.MFAKey;
import me.vinceh121.gmcserver.mfa.MFAManager;

class TestMFA {

	@ParameterizedTest
	@CsvSource({ "30, 6, HmacSHA1, 512", "30, 6, HmacSHA256, 512", "30, 6, HmacSHA512, 1024",
			"30, 8, HmacSHA256, 512" })
	void testMFAManager(final String duration, final String length, final String algo, final String keysize)
			throws InvalidKeyException {
		final GMCServer srv = Mockito.mock(GMCServer.class);
		final Properties props = new Properties();
		props.setProperty("totp.duration", duration);
		props.setProperty("totp.length", length);
		props.setProperty("totp.algo", algo);
		props.setProperty("totp.keysize", keysize);
		Mockito.when(srv.getConfig()).thenReturn(props);

		final MFAManager mfa = new MFAManager(srv);
		Mockito.when(srv.getMfaManager()).thenReturn(mfa);
		final MFAKey key = mfa.generateKey();

		System.out.println("Key: " + key.toURI("accountName"));
		System.out.println("TOTP password: " + mfa.generateOneTimePassword(key, Instant.now()));

		final User user = new User();
		user.setMfaKey(key);

		Assertions.assertTrue(mfa.verifyCode()
			.setUser(user)
			.setPass(mfa.generateOneTimePassword(key, Instant.now()))
			.execute()
			.toCompletionStage()
			.toCompletableFuture()
			.isCompletedExceptionally());

		user.setMfa(true);

		Assertions.assertFalse(mfa.verifyCode()
			.setUser(user)
			.setPass(mfa.generateOneTimePassword(key, Instant.now()))
			.execute()
			.toCompletionStage()
			.toCompletableFuture()
			.isCompletedExceptionally());
	}
}

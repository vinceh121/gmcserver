package gmcserver;

import java.util.Properties;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import me.vinceh121.gmcserver.GMCServer;
import me.vinceh121.gmcserver.mfa.MFAManager;

class TestMFA {

	@ParameterizedTest
	@CsvSource({ "30, 6, HmacSHA1, 512", "30, 6, HmacSHA256, 512", "30, 6, HmacSHA512, 1024",
			"30, 8, HmacSHA256, 512" })
	void testMFAManager(final String duration, final String length, final String algo, final String keysize) {
		final GMCServer srv = Mockito.mock(GMCServer.class);
		final Properties props = new Properties();
		props.setProperty("totp.duration", duration);
		props.setProperty("totp.length", length);
		props.setProperty("totp.algo", algo);
		props.setProperty("totp.keysize", keysize);
		Mockito.when(srv.getConfig()).thenReturn(props);

		final MFAManager mfa = new MFAManager(srv);
		System.out.println("Key: " + mfa.generateKey());
		// System.out.println("TOTP: "+mfa.generateOneTimePassword(key, Instant.now()));
	}

}

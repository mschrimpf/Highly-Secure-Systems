package nz.ac.aut.hss.server;

import nz.ac.aut.hss.distribution.protocol.*;
import nz.ac.aut.hss.distribution.server.ClientListRequestHandler;
import nz.ac.aut.hss.distribution.server.KeyAuthority;
import nz.ac.aut.hss.util.ECCKeyGen;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Martin Schrimpf
 * @created 27.08.2014
 */
public class ClientListRequestHandlerTest {
	private ClientListRequestHandler handler;
	private KeyAuthority authority;

	@Before
	public void setUp() throws IOException, ClassNotFoundException {
		authority = new KeyAuthority();
		handler = new ClientListRequestHandler(authority);
	}

	@Test
	public void empty() {
		Message msg = handler.processInput("1", new ClientListRequestMessage());
		assertTrue(msg instanceof ClientListMessage);
		assertTrue(((ClientListMessage) msg).phonePublicKey.isEmpty());
	}

	@Test
	public void singleEntry() {
		final String phone = "12345";
		authority.addClientPublicKey(phone, ECCKeyGen.create());
		Message msg = handler.processInput("1", new ClientListRequestMessage());
		assertTrue(msg instanceof ClientListMessage);
		assertEquals(1, ((ClientListMessage) msg).phonePublicKey.size());
		assertThat(((ClientListMessage) msg).phonePublicKey.keySet(), contains(phone));
	}

	@Test
	public void invalidRequest() {
		Message msg = handler.processInput("1", new JoinRequestMessage());
		assertTrue(msg instanceof ProtocolInvalidationMessage);
	}
}

package nz.ac.aut.hss.client.communication;

import nz.ac.aut.hss.distribution.auth.MessageAuthenticator;
import nz.ac.aut.hss.distribution.crypt.ClientMessageEncrypter;
import nz.ac.aut.hss.distribution.crypt.CryptException;
import nz.ac.aut.hss.distribution.crypt.Encryption;
import nz.ac.aut.hss.distribution.crypt.RSA;
import nz.ac.aut.hss.distribution.protocol.ClientCommunicationMessage;
import nz.ac.aut.hss.distribution.protocol.EncryptedClientCommunicationMessage;
import nz.ac.aut.hss.distribution.protocol.Message;
import nz.ac.aut.hss.distribution.util.ObjectSerializer;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * One client communication serves as the intermediary between this app and one other client.
 * For multiple clients, multiple client communications are needed.
 * @author Martin Schrimpf
 * @created 03.09.2014
 */
public class ClientCommunication {
	private final String partnerPhoneNumber;
	protected final ClientMessageEncrypter messageEncrypter;
	private final MessageAuthenticator authenticator;
	protected final ObjectSerializer serializer;
	protected final PrivateKey privateKey;
	private final Encryption encryption;
	protected PublicKey partnerPublicKey;
	private final SMSSender smsSender;

	public ClientCommunication(final String partnerPhoneNumber, final ServerCommunication serverCommunication,
							   final SMSSender smsSender, final PrivateKey ownPrivateKey)
			throws CommunicationException, ClientDoesNotExistException {
		if (partnerPhoneNumber == null || partnerPhoneNumber.length() == 0)
			throw new IllegalArgumentException("partnerPhoneNumber is null or empty");
		this.partnerPhoneNumber = partnerPhoneNumber;
		if (smsSender == null) throw new IllegalArgumentException("smsSender is null");
		this.smsSender = smsSender;
		if (ownPrivateKey == null) throw new IllegalArgumentException("ownPrivateKey is null");
		privateKey = ownPrivateKey;

		partnerPublicKey = serverCommunication.requestClient(partnerPhoneNumber);
		encryption = new RSA(privateKey, partnerPublicKey);
		messageEncrypter = new ClientMessageEncrypter(ownPrivateKey);

		try {
			authenticator = new MessageAuthenticator();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Could not create authenticator", e);
		}
		serializer = new ObjectSerializer();
	}

	public void sendMessage(final String content, final boolean confidential, final boolean authenticate)
			throws CommunicationException {
		Message msg;
		/* encrypt */
		if (confidential) {
			final String encryptedContent;
			try {
				encryptedContent = encryption.encrypt(content);
			} catch (CryptException e) {
				throw new CommunicationException("Could not encrypt message", e);
			}
			msg = new EncryptedClientCommunicationMessage(encryptedContent);
		} else {
			msg = new ClientCommunicationMessage(content);
		}
		/* authenticate */
		if (authenticate) {
			try {
				msg.authentication = authenticator.hash(msg, privateKey);
			} catch (CryptException | IOException e) {
				throw new CommunicationException("Could not create authentication", e);
			}
		}
		/* serialize */
		final String serial;
		try {
			serial = serializer.serialize(msg);
		} catch (IOException e) {
			throw new CommunicationException("Could not serialize message", e);
		}
		smsSender.send(partnerPhoneNumber, serial);
	}

	public Message stringToMessage(final String str) throws IOException, ClassNotFoundException {
		final Object obj;
		obj = serializer.deserialize(str);
		if (!(obj instanceof Message)) {
			throw new ClassCastException("Expected " + Message.class.getName() + ", got " + obj.getClass().getName());
		}
		return (Message) obj;
	}

	public boolean isMessageConfidential(final Message message) {
		return message instanceof EncryptedClientCommunicationMessage;
	}

	public String getPlainMessage(Message msg) throws IOException, CryptException, ClassNotFoundException {
		if (!isMessageConfidential(msg)) {
			verifyClientCommunicationClass(msg);
			return ((ClientCommunicationMessage) msg).content;
		}
		return encryption.decrypt(((EncryptedClientCommunicationMessage) msg).content);
	}

	private void verifyClientCommunicationClass(final Message msg) throws IllegalArgumentException {
		if (!(msg instanceof ClientCommunicationMessage))
			throw new IllegalArgumentException(
					"Expected " + ClientCommunicationMessage.class.getSimpleName() + ", got " +
							msg.getClass().getName());
	}

	public boolean isMessageAuthentic(final Message message) throws IOException, CryptException {
		if (partnerPublicKey == null) throw new IllegalStateException("partnerPublicKey has not been set");
		return authenticator.verify(message, partnerPublicKey);
	}
}

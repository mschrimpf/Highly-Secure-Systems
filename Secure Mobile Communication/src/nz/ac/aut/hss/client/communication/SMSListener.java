package nz.ac.aut.hss.client.communication;

/**
 * @author Martin Schrimpf
 * @created 04.09.2014
 */
public interface SMSListener {
	public void receive(final String phone, final String textContent);
}
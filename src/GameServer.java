import java.rmi.Remote;
import java.rmi.RemoteException;

/* 
 * THIS INTERFACE EXTENDS REMOTE AND DECLARES REMOTE METHOD 
 */

public interface GameServer extends Remote {

	/* Register a new user 
	 * 
	 * @param nickUtente ---> nickname 
	 * @param password ---> password
	 * 
	 */
	String registra_utente(String nickUtente, String password) throws RemoteException;
	
}

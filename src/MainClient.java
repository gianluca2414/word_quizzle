import java.awt.EventQueue;
import java.io.*;
import java.net.*;

/* 
 * MAIN OF A CLIENT
 * FIRST OF ALL IT GETS SERVER IP AND SERVER PORT, THEN IT CREATES A CLIENT ISTANCE
 * MOREOVER, IT CREATES A THREAD THAT STARTS THE CLIENT WITH HIS INITIAL WINDOW, WHERE IT WILL REGISTER OR LOG IN
 * 
 */


public class MainClient {

	private static final int server_port = 1234; //server port
	private static final int RMI_port = 5678; //RMI port
	
	public static void main(String[] args) {

		InetAddress server_address = null;

		try {
			server_address = InetAddress.getByName("localhost"); //server IP and server port
		} catch (UnknownHostException uhe) {
			System.out.println("Errore acquisizione IP e porta server: " + uhe.getMessage());
			uhe.printStackTrace();
		} 
		
		try {
			
			//create a client istance 
			 Client c = new Client(server_address,server_port,RMI_port); 
			 
			 	//Main thread of a client 
				EventQueue.invokeLater(new Runnable() {
						public void run() {
							try {
								SchermataInizialeGUI window = new SchermataInizialeGUI(c,null);
								window.frame.setVisible(true);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});
			
		} catch (IOException ioe) {
			System.out.println("Errore creazione schermata iniziale: " + ioe.getMessage());
			ioe.printStackTrace();
		}
		
	}
}

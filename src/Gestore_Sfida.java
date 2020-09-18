import java.io.*;
import java.net.*;
import java.nio.channels.SocketChannel;

/*
 * CLIENT-SIDE CLASS THAT HANDLES WAITING FOR CHALLENGE REQUESTS
 * WHEN THE CLIENT RECEIVES A CHALLENGE REQUEST VIA UDP FROM THE SERVER, IT WILL DECIDE TO ACCEPT OR REJECT IT USING SPECIFIC BUTTONS 
 * 
 */


public class Gestore_Sfida implements Runnable {
	
	private DatagramSocket socket_udp; //UDP socket 
	private int client_port; //client port that receives UDP message from the server
	private SchermataOperazioniGUI schermata; //main window
	private Client cliente; //istance of client 
	
	
	public Gestore_Sfida(SocketChannel sc,Client c,SchermataOperazioniGUI frame) throws SocketException { //builder
		
		this.client_port = sc.socket().getLocalPort(); //port client from TCP channel
		this.socket_udp = new DatagramSocket(client_port); 
		this.schermata = frame;
		this.cliente = c;
	}
	
	
	
	
	public void run() { //thread that waits for challenge requests will run this task
		
		//Thread is a daemon, so it will end when the main thread of a client ends (when a user does logout)
		while(!Thread.interrupted()) { 
						
			byte[] req = new byte[256];  
			
			DatagramPacket richiesta = new DatagramPacket(req,req.length); //packet where puts challenge request
				
				try {
					socket_udp.receive(richiesta); //receive challenge request from UDP socket and put it into the packet 
				} catch (IOException ioe) {
					System.out.println("Ricezione richiesta sfida sulla socket UDP: " + ioe.getMessage());
					return;
				}
				
				String s = ""; 
				try {
					s = new String(req,"UTF-8"); 
				} catch (UnsupportedEncodingException e) {
					System.out.println("Errore sul formato dei caratteri in gestore sfida: " + e.getMessage());
					e.printStackTrace();
				} 

				String[] array = s.split(" "); 
				
				if(array[0].equals("RICHIESTA_SFIDA")) { //check the content of the request
					
					String sfidante = array[1]; //utente that sends the request
					int k = Integer.parseInt(array[3]); //number of words to translate
					int t2 = Integer.parseInt(array[4]); //duration of the match
					cliente.setNumParole(k);
					cliente.setDurataPartita(t2);
					this.schermata.arrivaRichiesta(sfidante); //window for the challenged user
				}
			}	
		}
}

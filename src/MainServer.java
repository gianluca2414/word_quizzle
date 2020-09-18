import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.*;

/*
 * MAIN OF THE SERVER
 * IT USES A SELECTOR TO KNOWS CLIENT WHO WANTS TO CONNECT AND TO CONTROL ITS CHANNELS 
 * WITH THE SAME SELECTOR IT CHECKS IF THERE ARE REQUESTS FROM CLIENTS (READABLE CHANNELS) OR IF THE SERVER HAS TO WRITE TO A CLIENT (WRITABLE CHANNELS)
 * 
 */

public class MainServer {
	
	private static Server serv = null; //istance of Server class
	private static final String name = "GameServer"; //remote service name 
	private static final int RMI_port = 5678; //port of RMI service
	private static final int server_port = 1234; //server port 
	
	
	public static void main(String[] args) {

		ServerSocketChannel server = null; //server-side channel
		Selector sel = null; //selector
		
		try {
			
			serv = new Server(); //server istance 
						
			LocateRegistry.createRegistry(RMI_port); 
			
			Registry reg = LocateRegistry.getRegistry(RMI_port); //registry reference 
			
			//Register the remote object to the registry, linking its name with its reference 
			//If there is already an entry for that object, it will be overwritten
			reg.rebind(name,serv);
			
		} catch (Exception e){
			System.out.println("Errore nella registrazione main server: " + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		
		
		try { 
			
			sel = Selector.open(); //create a selector
			
			server = ServerSocketChannel.open(); //server socket channel where arrives connection requests
			
			server.bind(new InetSocketAddress("localhost",server_port));//server socket channel IP and port
			
			//Server socket channel works in non-blocking mode. In this way if it can't process a request it can go ahead to other requests
			server.configureBlocking(false);
			
			//Register server socket channel to the selector and retrieve its key 
			//Wait for accept method (new client)
			server.register(sel,SelectionKey.OP_ACCEPT);
			
			while(true) { //server loop
				
				sel.selectedKeys().clear(); //initialize selector's key set 
				
				System.out.println("SERVER WQ IN ESECUZIONE\nIn attesa di nuovi utenti ... ");
				System.out.println();

				sel.select();//key set of ready channels (blocking method)
								
				for(SelectionKey sel_key : sel.selectedKeys()) {//iterate over key set to find the key of the ready channel
					
					try {
						
						//METHOD: ACCEPT
						if(sel_key.isAcceptable()) {//first connection of a client 
							
								System.out.println("*** INIZIO ACCEPTABLE ***");
								
								ServerSocketChannel ch = (ServerSocketChannel)sel_key.channel();//server socket channel
								SocketChannel client = ch.accept(); //connection accepted, create a socket channel for the new client 
								
								System.out.println("New Client: [ " + client.getRemoteAddress() + " ]"); 
								
								client.configureBlocking(false); //client's socket channel works in blocking mode
															
								ByteBuffer msg = ByteBuffer.allocateDirect(512);
								
								//Register client's socket channel and retrieve its key
								//In this way the server will be able to check if there is a request from that client
								client.register(sel,SelectionKey.OP_READ,msg);
								
								System.out.println("*** FINE ACCEPTABLE ***");
								System.out.println();

						}
						
						//METHOD: READ
						if(sel_key.isReadable()) {//check if a client has sent a request 
							
								System.out.println("*** INIZIO READABLE ***");
							
								SocketChannel channel = (SocketChannel)sel_key.channel(); //channel ready for a read operation
								ByteBuffer req = (ByteBuffer) sel_key.attachment();
								
								boolean stop = false;
								String messaggio = ""; 
								
								while(!stop) { //loop where the server read the message from the client 
									
									req.clear(); 
									
									channel.read(req); //read the message and put it into a Byte Buffer
									
									req.flip(); 
									
									CharBuffer cb = StandardCharsets.UTF_8.decode(req); //decode the content of the buffer
									
									messaggio = messaggio + cb.toString(); 
																		
									if(messaggio.endsWith(".")) stop = true; //check if the server has read all the message on the channel
								
								}
															
								System.out.println("RICHIESTA CLIENT: " + messaggio);
								
								String[] array = messaggio.split(" "); 
								
								req.clear(); //clear the buffer to insert the reply to the client 
																																
								String risp = serv.analizzaRichiesta(sel,messaggio,channel); //analyze the type of request 
								
								System.out.println("RISPOSTA SERVER: " + risp);
								
								req = ByteBuffer.wrap(risp.getBytes("UTF-8")); //insert reply in the buffer
								
								String[] elem = risp.split(" "); 
								
								if(array[0].equals("SFIDA") == false) { 
									sel_key.interestOps(SelectionKey.OP_WRITE); //prepare channel to reply to the client that sends the request 
									sel_key.attach(req);
								} else {
									if(elem[0].equals("Sfida_rifiutata:")) { 
										sel_key.interestOps(SelectionKey.OP_WRITE);
										sel_key.attach(req);
									}
								}
								
								System.out.println("*** FINE READABLE ***");
								System.out.println();
							
						}
						
						//METHOD: WRITE
						if(sel_key.isWritable()) { //check if a client channel is ready to receive a reply
							
								System.out.println("*** INIZIO WRITABLE ***");
							
								SocketChannel channel = (SocketChannel) sel_key.channel();//channel ready
								
								
								ByteBuffer risp = (ByteBuffer) sel_key.attachment();
																								
								while(risp.hasRemaining()) {
									channel.write(risp); //write on the channel the reply contained in the buffer
								}
																
								risp.flip(); 
								
								CharBuffer cb = StandardCharsets.UTF_8.decode(risp); //decode the content of the buffer
								
								String tmp = cb.toString(); 
																
								if(tmp.equals("Logout effettuato con successo .")) { //if the server receives a logout request from a client 
									sel_key.cancel(); //delete the key associated to the client channel
									channel.close(); //close client channel
								} else {
									
									//Server waits other requests from that client 
									sel_key.interestOps(SelectionKey.OP_READ); 
									
								}
								
								System.out.println("*** FINE WRITABLE ***");
								System.out.println();
							
						}
						
					} catch (IOException ioe) {
						
						//In this section server handles what happens if the user clicks on 'X' button on the top right corner of the window
						
						System.out.println("Selettore nel main del server: " + ioe.getMessage() + "\n");	
						
						SocketChannel sc = (SocketChannel) sel_key.channel(); //client socket channel 
						
						serv.rimuoviClient(sc); //delete the user from a structure that contains online users 
						
						sel_key.cancel(); //delete the key of client channel from the selector
						
						sc.close(); //close the channel
						
					}
				}
			}
		} catch (IOException ioe) {
			System.out.println("Errore di I/O nel main del server: " + ioe.getMessage());
		} finally {
			try {
				server.close(); //close the server socket channel
			} catch (IOException e) {
				System.out.println("Errore di chiusura del serversocketchannel: " + e.getMessage());
			}
		}
	}
}

import java.rmi.registry.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * THIS CLASS IMPLEMENTS ALL CLIENT-SIDE METHODS  
 * THERE ARE SOME AUXILIARY METHODS TO READ/WRITE WITH A TCP CHANNEL AND COMMUNICATE WITH SERVER
 * 
 */


public class Client {
	
	private SocketChannel socket_tcp; //channel with the server 
	private int parole; //words to translate (chosen by server)
	private int durata_partita; //match duration (chosen by server)
	private boolean connesso; //var that check if a user is online or not
	private InetAddress server_addr; //server IP
	private int reg_port; //registry port
	private int server_port; //server port
	private Gestore_Sfida gest; //istance that wait challenge requests from other clients
	private Thread gestore; //this var creates a manager thread that handles challenge requests
	private boolean partito; //boolean that check if manager thread is started
	
	
	public Client(InetAddress server_address,int serverport,int regport) throws IOException { //constructor
		
		this.server_addr = server_address;
		this.server_port = serverport;
		this.reg_port = regport;
		this.connesso = false;
		this.gest = null;
		this.partito = false;
	}
	
	
	
	//********************************************************  START AUXILIARY CLIENT METHODS *******************************************************//
	
	
	/* Write a request on TCP channel and send it to the server
	 * 
	 * @param request ---> user request to forward
	 * 
	 */
	public void write_to_server(String request) throws IOException {
		
		ByteBuffer richiesta = ByteBuffer.wrap(request.getBytes("UTF-8")); //put the request in a ByteBuffer
		
		while(richiesta.hasRemaining()) { //write on the channel
			socket_tcp.write(richiesta);
		}
		
	}
	
	
	
	/* Return the number of words to translate 
	 * 
	 */
	public int getNumParole() {
		return this.parole;
	}
	
	
	
	/* Duration of a match in milliseconds
	 * 
	 */
	public int durataPartita() {
		return this.durata_partita;
	}
	
	
	
	/* Set the duration of the match to client
	 * 
	 * @param tempo ---> time to set on client 
	 * 
	 */
	public void setDurataPartita(int tempo) {
		this.durata_partita = tempo;
	}
	
	
	
	/* Set the number of words to translate to client
	 * 
	 * @param num ---> number of words 
	 * 
	 */
	public void setNumParole(int num) {
		this.parole = num;
	}
	
	
	
	/* Start the manager thread which waits for challenge requests and informs the client
	 * This thread start as soon as user does login and it ends when user does logout
	 * 
	 * @param schermata ---> main window 
	 * @param nickUtente ---> username 
	 * 
	 */
	public void avviaGestore(SchermataOperazioniGUI schermata,String nickUtente) {

		if(this.partito == false) { 
			
			System.out.println("GESTORE_SFIDA avviato");
			
			try {
				
				//Task that waits for challenge requests (see Gestore_Sfida class)
				this.gest = new Gestore_Sfida(this.socket_tcp,this,schermata);
				
			} catch (SocketException e) {
				System.out.println("Errore avvio gestore sfida: " + e.getMessage());
				e.printStackTrace();
			} 
			
			this.gestore = new Thread(this.gest); //pass the task to a thread
			gestore.setDaemon(true); 
			this.gestore.start(); //start thread
			this.partito = true; //this variable avoid to start many manager threads (every user has only one manager thread)
		}
	}
	
	
	
	/* Read a message from a TCP channel
	 * 
	 * 
	 */
	public String read_from_server() throws IOException {
		
		String risp = ""; 
		boolean ok = false; 
		ByteBuffer risposta = ByteBuffer.allocateDirect(512); 
		
		
		while(ok == false) {
									
			risposta.clear(); 
						
			socket_tcp.read(risposta); //read message from channel and put it into a Byte Buffer
												
			risposta.flip();
			
			CharBuffer cb = StandardCharsets.UTF_8.decode(risposta); //decode buffer content 
			
			risp = risp + cb.toString(); //build the message received from the server
						
			//Check if reading is done 
			if(risp.endsWith(".")) {
				ok = true; 
			}
			
		}
		
		return risp; 
	}
	
	

	
	//******************************************************** END AUXILIARY CLIENT METHODS *******************************************************//
	
	
	//******************************************************** START CLIENT METHODS *******************************************************//
	
	
	/* Register a new user 
	 * 
	 * @param nickUtente ---> nickname 
	 * @param password ---> password 
	 * 
	 */
	public String registra_utente(String nickUtente,String password) throws NotBoundException {
		
		String name = "GameServer"; //remote service name 
		String esito = "";
		
		try {
			
			Registry reg = LocateRegistry.getRegistry(reg_port); //reference to registry (creates by the server)
			
			Remote obj = reg.lookup(name); //copy of stub linked to remote object 
			
			GameServer gs = (GameServer) obj; //explicit cast 
			
			String str = gs.registra_utente(nickUtente, password); //call remote method
			
			//Check the result of the registration
			if(str.equals("Registrazione effettuata .")) {
				System.out.println(esito);
				esito = str;
			}
			else if(str.equals("Utente già registrato .")) {
				System.out.println(esito);
				esito = str;
			} else if(str.equals("Errore credenziali .")){
				System.out.println("Registrazione fallita .");
				esito = "Registrazione fallita .";
			}
			
		} catch (RemoteException re) {
			System.out.println("Errore registra_utente lato client: " + re.getMessage());
			re.printStackTrace();
		}	
		
		return esito; //result of the registration
	}
	
	
	
	/* Login method
	 * 
	 * @param nickUtente ---> nickname 
	 * @param password ---> password 
	 * 
	 */
	public String login(String nickUtente, String password) throws IOException {
		
		if(connesso == true) { //user has already logged in
			System.out.println("LOGIN " + nickUtente + ": Utente già connesso .");
			return "Utente già connesso .";
		}
		
		SocketAddress server = new InetSocketAddress(this.server_addr,this.server_port); //socket address with IP and server port
		this.socket_tcp = SocketChannel.open(server); //open the channel with the server
		this.socket_tcp.configureBlocking(true); //read and write are in blocking mode 
				
		String richiesta = "LOGIN " + nickUtente + " " + password + " ."; //string for login request 
				
		write_to_server(richiesta); //send login request to the server
				
		String risposta = read_from_server(); //server reply
		
		if(risposta.equals("Login effettuato con successo .")) { 
			connesso = true;//if server answer with a positive reply 
			return "Login effettuato con successo .";
		} else if(risposta.equals("Username e/o password errati .")){
			return "Username e/o password errati .";
		} else if(risposta.equals("Errore credenziali .")) {
			return "Errore credenziali .";
		} else if(risposta.equals("Utente già connesso .")) {
			return "Utente già connesso .";
		} else {
			return "Utente non registrato .";
		}
	}
	
	
	
	/* Logout method
	 * 
	 * @param nickUtente ---> nickname 
	 * 
	 */
	public void logout(String nickUtente) throws IOException {
				
		if(!connesso) { //check if the user has already logged in
			System.out.println("LOGOUT " + nickUtente + ": Utente non connesso .");
			return;
		}
		
		String richiesta = "LOGOUT " + nickUtente + " ."; //string for logout request
				
		write_to_server(richiesta); //send logout request to the server
				
		connesso = false; 
		
		socket_tcp.close();		
		
	}
	
	
	
	/* Method to add a friend 
	 * 
	 * @param nickUtente ---> nickname 
	 * @param nickAmico ---> nickname of the friend 
	 * 
	 */
	public String aggiungi_amico(String nickUtente,String nickAmico) throws IOException {
		
		if(!connesso) { //check if user has already logged in 
			System.out.println("AGGIUNGI_AMICO " + nickUtente + " " + nickAmico + ": Utente non connesso .");
			return "Utente non connesso .";
		}
		
		String richiesta = "AGGIUNGI_AMICO " + nickUtente + " " + nickAmico + " ."; //string for add_friend request 
		
		write_to_server(richiesta); //send add_friend request to the server
		
		String risposta = read_from_server(); //read server reply 
		
		System.out.println(risposta); 
		return risposta;
		
	}
	
	
	
	/* List of friends of a user
	 * 
	 * @param nickUtente ---> nickname 
	 * 
	 */
	public JSONObject lista_amici(String nickUtente) throws IOException, ParseException {
				
		if(connesso == false) { //check if user has already logged in 
			System.out.println("LISTA_AMICI " + nickUtente + ": Utente non connesso .");
			return null;
		}
		
		String richiesta = "LISTA_AMICI " + nickUtente + " ."; //string for list request
		
		write_to_server(richiesta); //send list request to the server
		
		String risposta = read_from_server(); //read server reply
		risposta = risposta.substring(0,risposta.length() - 1); 
				
		JSONParser parser = new JSONParser(); 
		JSONObject lista_amici = (JSONObject) parser.parse(risposta); //convert the answer from string to JSON object
		
		return lista_amici; 
	
	}
	
	
	
	/* Challenge request method
	 * 
	 * @param nickUtente ---> nickname
	 * @param nickAmico ---> nickname of the challenged user 
	 * @param finestra ---> window where the mathc takes place 
	 * 
	 */
	public void sfida(String nickUtente,String nickAmico,SchermataSfidaGUI finestra) throws IOException, InterruptedException {
		
		if(!connesso) { //check if user has already logged in
			System.out.println("SFIDA " + nickUtente + " " + nickAmico + ": Utente non connesso .");
			return;
		}
		
		String richiesta = "SFIDA " + nickUtente + " " + nickAmico + " ."; //string for challenge request
		
		write_to_server(richiesta); //send challenge request to the server
		
		String risposta = read_from_server(); //read server reply
		
		String[] array = risposta.split(" "); 
				
		//Check the answer of the challenged user
		if(array[1].equals("accettata")) { 
			this.parole = Integer.parseInt(array[2]); //number of words to translate
			this.durata_partita = Integer.parseInt(array[3]); //duration of the match (in milliseconds)
			System.out.println("\nSfida accettata .\nDovrai tradurre " + this.parole + " parole in " + this.durata_partita/1000 + " secondi .\n");
			finestra.iniziaSfida(); //method to start the match and change the window 
		} else {
			finestra.sfidaRifiutata(); //window for rejected challenge 
			System.out.println("\n" + risposta + "\n"); 
		}
	}
	
	
	
	/* Retrieve the score of a user
	 * 
	 * @param nickUtente ---> nickname 
	 * 
	 */
	public String mostra_punteggio(String nickUtente) throws IOException {
		
		if(!connesso) { //check if user has already logged in
			System.out.println("MOSTRA_PUNTEGGIO "  + nickUtente + ": Utente non connesso .");
			return "Errore: richiesta punteggio .";
		}
		
		String richiesta = "MOSTRA_PUNTEGGIO " + nickUtente + " ."; //string for score request
		
		write_to_server(richiesta); //send score request to the server
		
		String risposta = read_from_server(); //read server reply 
		risposta = risposta.substring(0,risposta.length() - 1); 
		
		return risposta; 
	}
	
	
	
	/* Retrieve the ranking of a user and his friends
	 * 
	 * @param nickUtente ---> nickname 
	 * 
	 */
	public JSONObject mostra_classifica(String nickUtente) throws IOException, ParseException {
		
		if(!connesso) { //check if user has already logged in
			System.out.println("MOSTRA_CLASSIFICA " + nickUtente + ": Utente non connesso .");
			return null;
		}
		
		String richiesta = "MOSTRA_CLASSIFICA " + nickUtente + " ."; //string for ranking request
		
		write_to_server(richiesta); //send ranking request to the server
		
		String risposta = read_from_server(); //read server reply
		
		String[] array = risposta.split(" "); 
		
		if(array[0].equals("Errore")) { //
			return null;
		}
		
		risposta = risposta.substring(0,risposta.length() - 1); 
		
		JSONParser parser = new JSONParser(); 
		JSONObject lista_amici = (JSONObject) parser.parse(risposta); //convert from string to JSON object
		
		return lista_amici; 
	}


	//******************************************************** END CLIENT METHODS *******************************************************//
	
}

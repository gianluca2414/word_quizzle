import java.rmi.RemoteException;
import java.rmi.server.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.nio.file.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/*
 * THIS CLASS IMPLEMENTS ALL SERVER-SIDE METHODS TO PROCESS REQUESTS FROM CLIENTS
 * THERE ARE AUXILIARY METHODS TO READ A MESSAGE FROM A CLIENT, WRITE A MESSAGE TO A CLIENT, TO CREATE AND MODIFY A JSON FILE,
 * TWO METHODS THAT WORK ON A MAP AND A METHOD CALLED BY MAINSERVER TO ANALYZE REQUESTS AND CALLS THE RIGHT FUNCTION TO PROCESS THEM
 * 
 */

public class Server extends UnicastRemoteObject implements GameServer{
	
	private static final long serialVersionUID = 1L; //serialization and deserialization use compatible version of the same classes
	private JSONObject JSONUtenti_Registrati; //JSON file that mantains registered users
	private JSONObject JSONAmici; //JSON file that mantains friends list of each user 
	private JSONObject JSONPunti; //JSON file that mantains the score of each user
	private String Registrazioni = "Utenti_Registrati.json"; //JSON file for registrations
	private String Amicizie = "Amicizie.json"; //JSON file for the friendships
	private String Punteggio = "Punteggi.json"; //JSON file for the scores
	
	//This structure mantains online users and their channels with the server
	private ConcurrentHashMap<String,SocketChannel> Utenti_Online; 
	
	//Struttura dati usata per tenere traccia di quali utenti sono coinvolti in una sfida e quali invece possono ricevere richieste
	//This structure mantains users involved in a match and available users
	private ConcurrentHashMap<String,String> Sfide; 
	
	
	
	public Server() throws RemoteException { //builder
		
		//Build and dynamically export the remote object
		//Create a server socket channel to handle remote methods invocation
		super(); 
		
		try {
						
			JSONUtenti_Registrati = creaFile(Registrazioni); 
			
			JSONAmici = creaFile(Amicizie);
			
			JSONPunti = creaFile(Punteggio); 
			
			Utenti_Online = new ConcurrentHashMap<String,SocketChannel>();  
			
			Sfide = new ConcurrentHashMap<String,String>(); 
						
		} catch (IOException ioe) {
			System.out.println("Errore nel costruttore del server: " + ioe.getMessage());
			ioe.printStackTrace();
		}
	}

	
	//******************************************************** START SERVER AUXILIARY METHODS *******************************************************//
	
	
	/* Method that creates a JSON file
	 * 
	 * @param filename ---> filename to create
	 * 
	 */
	public JSONObject creaFile(String filename) throws IOException {
		
		File file = new File(filename); //new file
		
		if(!(file.exists())) {
			file.createNewFile(); 
			
			FileOutputStream stream = new FileOutputStream(file); //stream to write on the file 
			FileChannel fc = stream.getChannel(); //channel linked to the stream
			
			JSONObject obj = new JSONObject(); 
			String s = obj.toJSONString();
			
			byte[] array = s.getBytes("UTF-8"); 
			ByteBuffer bb = ByteBuffer.wrap(array); 
			
			fc.write(bb); 
			
			bb.clear(); 
			
			//Closing channel and stream
			fc.close();
			stream.close();
		}
		
		return apriFile(filename); //return file content in JSON format
		
	}
	
	
	
	/* Method that opens a file and retrieves his content in JSON format
	 * 
	 * @param filename ---> file 
	 * 
	 */
	public JSONObject apriFile(String filename) {
		
		FileChannel fc = null; 
		JSONObject contenuto = null; 
		int byteLetti; 
		
		try {
			
			fc = FileChannel.open(Paths.get(filename),StandardOpenOption.READ); //open file in read mode
			int dim = (int) fc.size(); //file size
			
			ByteBuffer bb = ByteBuffer.allocateDirect(dim + 1); 
			boolean stop = false;
			
			while(!stop) { 
				
				byteLetti = fc.read(bb); //reading from the channel and saving in the buffer
				
				if(byteLetti == -1) stop = true; 
			}
			
			fc.close(); //closing the channel
			bb.flip(); //read the content of the buffer
			
			CharBuffer cb = StandardCharsets.UTF_8.decode(bb); //decode buffer content
			String s = cb.toString();
			
			JSONParser parser = new JSONParser(); 
				
			try {
				
				contenuto = (JSONObject) parser.parse(s); //convert from a sring to a JSON object
					
			} catch (ParseException e) {
				System.out.println("Errore nella trasformazione da stringa a JSON: " + e.getMessage());
				e.printStackTrace();
			}		
			
		} catch (IOException ioe) {
			System.out.println("Errore nel recupero del file: " + ioe.getMessage());
			ioe.printStackTrace();
		}
		
		return contenuto; 
	}
	
	
	
	/* Method that write on a JSON file
	 * 
	 * @param filename ---> filename to write
	 * @param obj ---> JSON object linked to the file 
	 * 
	 */
	public void scriviFile(String filename,JSONObject file) {
		
		File f = new File(filename); 
		
		try {
			
			if(!(f.createNewFile())) { //if file has been already created, delete it and create a new one 
				f.delete();
				f.createNewFile();
			}
			
			FileChannel fc = FileChannel.open(Paths.get(filename),StandardOpenOption.WRITE); //open the file in write mode
			
			byte[] array = file.toJSONString().getBytes("UTF-8"); //convert from JSON object to string 
			
			ByteBuffer bb = ByteBuffer.allocateDirect(array.length); 
			
			bb = ByteBuffer.wrap(array); //insert the string into a Byte Buffer
			
			while(bb.hasRemaining()) { //write the content of the buffer on the channel
				fc.write(bb);
			}
			
			fc.close(); //closing the channel
			
		} catch (IOException ioe) {
			System.out.println("Errore nella scrittura su file JSON: " + ioe.getMessage());
			ioe.printStackTrace();
		}
	}
	
	
	
	/* Method that checks if a user is online or offline
	 * 
	 * @param nickUtente ---> username to check 
	 * 
	 */
	public boolean connesso(String nickUtente) {
		
		return Utenti_Online.containsKey(nickUtente); 
				
	}
	
	
	
	/* Method that removes from a structure a user that throws an exception
	 * 
	 * @param sc ---> channel of the user to remove
	 * 
	 */
	public boolean rimuoviClient(SocketChannel sc) {
		
		for(Map.Entry<String,SocketChannel> elem: Utenti_Online.entrySet()) { //iterating the structure 
			
			String utente = elem.getKey(); 
			
			SocketChannel tmp = Utenti_Online.get(utente); //get the value (channel) from the key (user)
			
			if(tmp.equals(sc)) {
				Utenti_Online.remove(utente); //remove the user linked with this channel
				return true; 
			}
		}
		
		return false; 
	}
	
	
	
	/* Method that changes the state of a user involved in a match
	 * 
	 * @param utente ---> username 
	 * @param stato ---> new state to set in the map  
	 * 
	 */
	public synchronized void cambiaStatoUtente(String utente,String stato) {
		this.Sfide.put(utente,stato);
	}
	
	
	
	/*Method that sets the 'busy' state of a user. Control and modification of the state must be atomic, otherwise another thread could take control of the structure and 
	 * makes unsafe operations
	 * 
	 * @param utente ---> nickname of the challenged user
	 * @param channel ---> channel of the user that sends the challenge request
	 * @param sel ---> selector of MainServer
	 * 
	 */
	public synchronized boolean occupaUtente(String utente, SocketChannel channel, Selector sel) {
		
		if(this.Sfide.get(utente).equals("occupato")) { //check the state of the user
			
			try {
				
				String risp = "Sfida rifiutata: utente occupato ."; //if he's already busy in another match, write the reply to the user that sends the request
				ByteBuffer bb = ByteBuffer.allocateDirect(risp.length() + 1); 
				bb = ByteBuffer.wrap(risp.getBytes("UTF-8")); 
				
				while(bb.hasRemaining()) { //write the message on the channel
					channel.write(bb);
				}
				
				channel.keyFor(sel).interestOps(SelectionKey.OP_READ); 
				sel.wakeup(); //retrieve control to the main server selector
				
			} catch (IOException ioe) {
				System.out.println("Errore scrittura utente occupato: " + ioe.getMessage());
				ioe.printStackTrace();
			}
			
			return false; 
			
		} else {
			
			//If the challenged user is free, set his state to 'busy' and wait his reply 
			cambiaStatoUtente(utente,"occupato");
			return true;
		}
	}
	
	
	
	/* Method that updates the JSON file of the scores at the end of a match
	 * Threads that handle matches call it, so this method is synchronized
	 * 
	 * @param user1 ---> player 1
	 * @param user2 ---> player 2
	 * @param p1 ---> score of player 1
	 * @param p2 ---> score of player 2
	 * 
	 */
	@SuppressWarnings("unchecked")
	public synchronized void aggiornaJSON(String user1, String user2, int p1, int p2) {
				
		if(JSONPunti.containsKey(user1) == false) { //player 1 isn't in JSON file of the scores
			JSONPunti.put(user1,p1);		
		} else {
			
			//if player 1 is already present then add it the score passed by argument
			int punti_aggiornati = Integer.parseInt(String.valueOf(JSONPunti.get(user1)));
			punti_aggiornati = punti_aggiornati + p1;
			JSONPunti.put(user1,punti_aggiornati);
		}
		
		if(JSONPunti.containsKey(user2) == false) { //player 2 isn't in JSON file of the scores
			JSONPunti.put(user2,p2);		
		} else {
			
			//if player 2 is already present then add it the score passed by argument
			int punti_aggiornati = Integer.parseInt(String.valueOf(JSONPunti.get(user2)));
			punti_aggiornati = punti_aggiornati + p2;
			JSONPunti.put(user2,punti_aggiornati);
		}
		
		scriviFile(Punteggio,JSONPunti); //update JSON file of the scores
	}
	
	
	
	/* Method that analyzes a request and calls the specific function
	 * 
	 * @param s ---> main server selector
	 * @param msg ---> request message from a user
	 * @param sc ---> user's channel
	 * 
	 */
	public String analizzaRichiesta(Selector s,String msg,SocketChannel sc) throws IOException {
		
		String[] array = msg.split(" "); 
		String esito = ""; 
		
		if(array[0].equals("LOGIN")) { //REQUEST: LOGIN
			
			esito = login(array[1],array[2],sc); //call the login method from server
						
		} else if(array[0].equals("LOGOUT")) { //REQUEST: LOGOUT

			esito = logout(array[1]); //call the logout method from server
			
		} else if(array[0].equals("AGGIUNGI_AMICO")) { //REQUEST: AGGIUNGI AMICO
			
			esito = aggiungi_amico(array[1],array[2]); //call the method that adds a friend from server
			
		} else if(array[0].equals("LISTA_AMICI")) { //REQUEST: LISTA AMICI
			
			JSONObject obj = lista_amici(array[1]); //call the friend list method from server
			
			if(obj == null) {  
				esito = "Errore recupero lista amici.";
			} else {
				esito = obj.toJSONString() + " .";
			}
			
		} else if(array[0].equals("MOSTRA_PUNTEGGIO")) { //REQUEST: MOSTRA PUNTEGGIO
			
			esito = mostra_punteggio(array[1]); //call the score method from server
			
		} else if(array[0].equals("MOSTRA_CLASSIFICA")) { //REQUEST: MOSTRA CLASSIFICA
			
			JSONObject obj1 = mostra_classifica(array[1]); //call the ranking method from server 
			
			if(obj1 == null) {  
				esito = "Errore recupero classifica o nessun amico presente.";
			} else {
				esito = obj1.toJSONString() + " .";
			}
			
		} else if(array[0].equals("SFIDA")) { //REQUEST: SFIDA
			
			SocketChannel sc2 = Utenti_Online.get(array[2]); //socket channel of the challenged user
			
			boolean ris_sfida = sfida(s,array[1],sc,array[2],sc2); //call the match method from server
			
			//If the match method return true so it runs the server-side thread that handles the match
			//This thread forwards the UDP request to the challenged user and it waits for its reply
			if(ris_sfida == false) { 
				return esito = "Sfida_rifiutata: errore sui parametri della sfida .";
			} 
			
		} else { //REQUEST: UNSUPPORTED OPERATION
			
			esito = "Errore: operazione non supportata"; 
			
		}
		
		return esito; //result of the request, it will be written on the channel with the client 
	}
	
	
	//******************************************************** END SERVER AUXILIARY METHODS *******************************************************//
	
	
	//******************************************************** START SERVER METHODS *******************************************************//
	
	/* Registration method 
	 * 
	 * @param nickUtente ---> username 
	 * @param password ---> password 
	 * 
	 */
	@SuppressWarnings("unchecked")
	public synchronized String registra_utente(String nickUtente, String password) {
		
		@SuppressWarnings("unused")
		String esito = ""; 
		
		if(nickUtente.equals("") || password.equals("") || nickUtente.equals(null) || password.equals(null)) { //checking parameters
			return esito = "Errore credenziali .";
		}
		
		if(JSONUtenti_Registrati.containsKey(nickUtente)) { //user has already registered
			return esito = "Utente già registrato .";
		}  else {
			JSONUtenti_Registrati.put(nickUtente,password); //insert the user in the JSON file of the registrations
			scriviFile(Registrazioni,JSONUtenti_Registrati); //update the JSON file
			
			return esito = "Registrazione effettuata .";
		}
	}
	
	
	
	/* Login method 
	 * 
	 * @param nickUtente ---> username
	 * @param password ---> password 
	 * @param sc ---> user's channel
	 * 
	 */
	public String login(String nickUtente, String password, SocketChannel sc) {
		
		@SuppressWarnings("unused")
		
		String esito_login = ""; 
		
		if(nickUtente.equals("") || password.equals("") || nickUtente.equals(null) || password.equals(null)) { //checking parameters
			return esito_login = "Errore credenziali .";
		}
		
		if(connesso(nickUtente) == true ) { //user has already registered
			return esito_login = "Utente già connesso .";
		} 
			
		if(JSONUtenti_Registrati.containsKey(nickUtente)) { //check if the user has already registered
			if(JSONUtenti_Registrati.get(nickUtente).equals(password)) { //if the user has already registered, check his password
					Utenti_Online.put(nickUtente,sc); //if username and password are right, add the user to the structure that mantains online users
					cambiaStatoUtente(nickUtente,"libero"); //set user's state to 'free'
					return esito_login = "Login effettuato con successo .";
			} else {
				return esito_login = "Username e/o password errati ."; //wrong password
			} 
		} else {
			return esito_login = "Utente non registrato ."; //the user isn't registered
		}
	}
	
	
	
	/* Logout method
	 * 
	 * @param nickUtente ---> username
	 * 
	 */
	public String logout(String nickUtente) {
				
		@SuppressWarnings("unused")
		String esito = ""; 
		
		if(nickUtente.equals("") || nickUtente.equals(null)) { //checking parameters
			return esito = "Errore parametri .";
		}
								
		if(Utenti_Online.containsKey(nickUtente)) { 
			Utenti_Online.remove(nickUtente);
			return esito = "Logout effettuato con successo .";
		} else {
			return esito = "Logout fallito .";
		}	
	}
	
	
	
	/* Method that adds a friend
	 * 
	 * @param nickUtente ---> nickname 
	 * @param nickAmico ---> nickname of a friend
	 * 
	 */
	@SuppressWarnings("unchecked")
	public String aggiungi_amico(String nickUtente, String nickAmico) {
		
		@SuppressWarnings("unused")
		String esito = ""; 
		boolean controllo_U = false, controllo_A = false; 
		Object check_put_U = null, check_put_A = null; 
		
		if(nickUtente.equals("") || nickAmico.equals("") || nickUtente.equals(null) || nickAmico.equals(null)) { //checking parameters
			return esito = "Errore nelle credenziali .";
		}
		
		if(!(connesso(nickUtente))) { //user is offline
			return esito = "Utente non connesso ."; 
		}
		
		if(!(JSONUtenti_Registrati.containsKey(nickAmico))) { //friend is not registered
			return esito = "Amico/a non registrato/a .";
		}
		
		if(nickUtente.equals(nickAmico)) { 
			return esito = "Non si può essere amici di se stessi .";
		}
		
		if(JSONAmici.containsKey(nickUtente) == false) {
			JSONArray array = new JSONArray();
			JSONAmici.put(nickUtente,array);
		}
		
		if(JSONAmici.containsKey(nickAmico) == false) { 
			JSONArray array1 = new JSONArray();
			JSONAmici.put(nickAmico,array1);
		}
				
		JSONArray amici_U = (JSONArray) JSONAmici.get(nickUtente); 
		JSONArray amici_A = (JSONArray) JSONAmici.get(nickAmico); 
		
		if(!(amici_U.contains(nickAmico))) { 
			controllo_U = amici_U.add(nickAmico); 
			check_put_U = JSONAmici.put(nickUtente,amici_U);  
		} else {
			return esito = nickAmico + " è già tuo amico/a ."; 
		}

		if(!(amici_A.contains(nickUtente))) { 
			controllo_A = amici_A.add(nickUtente); 
			check_put_A = JSONAmici.put(nickAmico,amici_A); 
		}
		
		scriviFile(Amicizie,JSONAmici); //update the JSON file 
		
		if(controllo_U == true && controllo_A == true && check_put_U != null && check_put_A != null) { 
			return esito = "Amicizia con " + nickAmico + " aggiunta ."; 
		} else {
			return esito = "Errore: amicizia con " + nickAmico +  " non aggiunta .";
		}
	}
	
	
	
	/* Method that retrieves a friends' list to the user
	 * 
	 * @param nickUtente ---> username
	 * 
	 */
	@SuppressWarnings("unchecked")
	public JSONObject lista_amici(String nickUtente) {
		
		if(nickUtente.equals("") || nickUtente.equals(null)) { //checking parameters
			return null;
		}
		
		if(!(connesso(nickUtente))) { //user is offline
			return null;
		}
		
		if(JSONAmici.containsKey(nickUtente) == false) { //the user has no friends, so intialise the specific file
			JSONArray array = new JSONArray(); 
			JSONObject obj = new JSONObject(); 
			JSONAmici.put(nickUtente,array); 
			return obj;
		}
				
		JSONArray amici = (JSONArray) JSONAmici.get(nickUtente); 
		
		JSONObject lista_amici = new JSONObject(); 
		
		lista_amici.put(nickUtente,amici); 
		
		return lista_amici; 
	}
	
	
	
	/* Method that retrieves the score of a user
	 * 
	 * @param nickUtente ---> username
	 * 
	 */
	public String mostra_punteggio(String nickUtente) {
		
		String esito = ""; 
		
		if(nickUtente.equals("") || nickUtente.equals(null)) { //checking parameters
			return esito = "Errore credenziali .";
		}
		
		if(!(connesso(nickUtente))) { //user is offline
			return esito = "Utente non connesso .";
		}
		
		JSONPunti = apriFile(Punteggio); //open JSON file
		
		if(JSONPunti.containsKey(nickUtente) == false) { //if the user doesn't have play any match, its score will be 0
			esito = "0 .";
		} else {
			esito = String.valueOf(JSONPunti.get(nickUtente)) + " ."; 
		}
								
		return esito; 
		
	}
	
	
	
	/* Method that retrieves the ranking of a user and his friends
	 * 
	 * @param nickUtente ---> username
	 * 
	 */
	@SuppressWarnings("unchecked")
	public JSONObject mostra_classifica(String nickUtente) {
		
		if(nickUtente.equals("") || nickUtente.equals(null)) { //checking parameters
			return null;
		}
		
		if(!(connesso(nickUtente))) { //user is offline
			return null;
		}
		
		if(JSONAmici.containsKey(nickUtente) == false) { //user has no friends
			return null;
		}
		
		try {
			
			JSONPunti = apriFile(Punteggio); //open JSON file
			
		} catch (Exception e) {
			System.out.println("Errore apertura file JSON in mostra_classifica: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
				
		JSONArray amici = (JSONArray) JSONAmici.get(nickUtente); 
		int dim = amici.size(); 
		
		//Each item of this array has two fields: username and score
		ArrayList<Punteggi> puntiUtenti = new ArrayList<Punteggi>(); 
		
		int punti; 
		String amico = ""; 
		
		int punti_U = 0; //if a user doesn't have play any match, his score will be 0
		if(JSONPunti.containsKey(nickUtente)){  
			punti_U = Integer.parseInt(String.valueOf(JSONPunti.get(nickUtente)));
		}
		puntiUtenti.add(new Punteggi(nickUtente,punti_U)); //update the ranking gradually 
		
		for(int i = 0; i < dim; i++) { //iterate over the array of frienship
			
			amico = (String) amici.get(i); //username of a friend
			
			if(JSONPunti.containsKey(amico)) { //check if he has already played some matches
				punti = Integer.parseInt(String.valueOf(JSONPunti.get(amico))); 
			} else {
				punti = 0; 
			}
			puntiUtenti.add(new Punteggi(amico,punti)); 
		}
		
		Collections.sort(puntiUtenti); //sort the array in descending order based on user's score
		
		JSONObject classifica = new JSONObject(); 
		JSONArray array = new JSONArray(); 
		
		for(int j = 0; j < puntiUtenti.size(); j++) { //
			
			Punteggi p = puntiUtenti.get(j); 
			String nome = p.getUtente(); //username
			int points = p.getPunteggio(); //user's score
			JSONObject obj = new JSONObject();  
			obj.put(nome,points); 
			array.add(obj); 
			
		}
		
		classifica.put(nickUtente,array);  
				
		return classifica; 
	}
	
	
	
	 /* Method that handles a challenge request or forwards it to another user
	 * 
	 * @param sel ---> main server selector
	 * @param nickUtente ---> user that sends a challenge request
	 * @param sc1 ---> channel of the user that sends a challenge request
	 * @param nickAmico ---> challenged user
	 * @param sc2 ---> channel of the challenged user 
	 * 
	 */
	public boolean sfida(Selector sel,String nickUtente,SocketChannel sc1,String nickAmico,SocketChannel sc2) throws IOException {
				
		if(!(connesso(nickUtente)) || !(connesso(nickAmico)) || nickUtente.equals("") || nickAmico.equals("")
			|| nickUtente.equals(null) || nickAmico.equals(null)) { //checking parameters
			return false; 
		}
	
		JSONArray array = (JSONArray) JSONAmici.get(nickUtente); 
		
		if(array == null) { 
			return false;
		} else if (array.contains(nickAmico) == false) { //if there isn't a friendship beetwen two players, the match can't start
			return false;
		}
		
		//If there aren't errors and two players are friends, it sets the state of the user that sends the request to busy.
		//In this way I simulate his waiting and I avoid that he will receive other challenge requests
		cambiaStatoUtente(nickUtente,"occupato");

		
		ThreadSfida task = new ThreadSfida(this,sel,nickUtente,nickAmico,sc1,sc2,this.Utenti_Online); //server-side task that handles the match
		Thread t = new Thread(task); 
		t.start(); 
				
		return true; 
	}	
	
	//******************************************************** END SERVER METHODS *******************************************************//
}

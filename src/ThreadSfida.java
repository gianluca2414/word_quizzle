import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/* 
 * THIS CLASS IMPLEMENTS THE THREAD THAT HANDLES A MATCH
 * IT STARTS AS SOON AS THE SERVER RECEIVES A CHALLENGE REQUEST AND FIRST OF ALL IT CHECKS IF THE CHALLENGED USER IS FREE OR BUSY.
 * IF HE'S BUSY, THEN RETURN TO MAIN SERVER SELECTOR. IF HE'S FREE, THIS THREAD FORWARDS THE CHALLENGE REQUEST VIA UDP TO HIM.
 * WHEN IT RECEIVES THE ANSWER VIA TCP, THE TREAD FORWARDS IT TO THE USER THAT HAS SENT THE REQUEST.
 * IF THE OTHER PLAYER HAS ACCEPTED THE REQUEST THE MATCH STARTS, OTHERWISE IT RETURNS TO MAIN SERVER SELECTOR.
 * THIS THREAD USES A SELECTOR TO READ/WRITE WORDS TO THE PLAYERS, SOME DATA STRUCTURES TO TRANSLATE WORDS, STORE SCORES AND THE RESULT OF THE MATCH.
 * 
 */


public class ThreadSfida implements Runnable {
		
	private static int timeout = 15000; //time between a challenge request and the answer (in milliseconds)
	
	//Nicknames of the players
	private String utente1;
	private String utente2;
	
	//Number of words to translate 
	private int num1;
	private int num2;
	
	private int k; //number of words to translate
	private int durata_partita; //duration of the match (in milliseconds)
	
	//Variables that checks if a players has terminated the match
	private boolean finito1;
	private boolean finito2;
	
	private boolean errore_utente1;
	private boolean errore_utente2;
	
	//Main server selector
	private Selector main_sel;
	
	//Server instance
	private Server server;
	
	//Channels of the players
	private SocketChannel ch1;
	private SocketChannel ch2;
	
	//Structures that mantain words from the server and their translations
	private ArrayList<ItalianoInglese> parole1;
	private ArrayList<ItalianoInglese> parole2;
	
	private static String dizionario = "Dizionario.txt"; //file where server takes the words to translate 
	private String contenuto_dizionario; 
	
	private static String Server_Traduzioni = "https://api.mymemory.translated.net"; //URL of server that checks translations
	
	//Scores for each translation
	private int nonData = 0;
	private int ok = 3;
	private int errata = -1;
	private int extra = 5;
	
	//Counters to make a final report of the match
	private int corrette1;
	private int sbagliate1;
	private int nonDate1;
	private int corrette2;
	private int sbagliate2;
	private int nonDate2;
	
	private ConcurrentHashMap<String,SocketChannel> Utenti_Online; //online users
	
	
	
	public ThreadSfida (Server srv,Selector s, String u1, String u2, SocketChannel sc1, SocketChannel sc2,ConcurrentHashMap<String,SocketChannel> connessi) { //builder
		
	
		//Remove from the channels of the players from main server selector
		SelectionKey sk1 = sc1.keyFor(s);
		SelectionKey sk2 = sc2.keyFor(s);
		sk1.interestOps(0);
		sk2.interestOps(0);
		
		this.server = srv;
		this.main_sel = s;
		
		this.utente1 = u1;
		this.utente2 = u2;
		
		this.num1 = 0;
		this.num2 = 0;
		
		this.ch1 = sc1;
		this.ch2 = sc2;
				
		this.k = 10;
		this.durata_partita = 60000;
		
		this.parole1 = new ArrayList<ItalianoInglese>();
		this.parole2 = new ArrayList<ItalianoInglese>();
		
		this.corrette1 = 0;
		this.sbagliate1 = 0;
		this.nonDate1 = 0;
		this.corrette2 = 0;
		this.sbagliate2 = 0;
		this.nonDate2 = 0;
		
		this.contenuto_dizionario = apriDizionario(dizionario);
		
		this.Utenti_Online = connessi;
		
		this.finito1 = false;
		this.finito2 = false;
		
		this.errore_utente1 = false;
		this.errore_utente2 = false;
		
	}
	
	
	
	/* Method that opens a file and retrieves his content
	 * 
	 * @param filename ---> name of file to open
	 * 
	 */
	private String apriDizionario(String filename) {
		
		FileChannel fc = null; 
		int byteLetti; 
		String str = "";
		
		try {
			
			fc = FileChannel.open(Paths.get(filename),StandardOpenOption.READ); //open file channel in read mode 
			int dim = (int) fc.size(); //file size 
			
			ByteBuffer bb = ByteBuffer.allocateDirect(dim + 1); 
			boolean stop = false; 
			
			while(!stop) { //loop for the reading 
				
				byteLetti = fc.read(bb); //read from file channel and save bytes in to a Byte Buffer
				
				if(byteLetti == -1) { 
					stop = true; 
				}
			}
			
			fc.close(); //close the file channel
			bb.flip(); //read the content of the buffer
			
			CharBuffer cb = StandardCharsets.UTF_8.decode(bb); //decode the content of the buffer
			str = cb.toString();	
			
		} catch (IOException ioe) {
			System.out.println("Errore nel recupero del file dizionario: " + ioe.getMessage());
			ioe.printStackTrace();
		}
		return str;
	}
	
	
	
	/* Method that provides translations of words sent by the server
	 *  
	 */
	private String[] ottieniTraduzioni() throws IOException, ParseException {
		
		URL url = new URL(Server_Traduzioni); //create the URL to the specific translation server
		String[] trad = new String[this.parole1.size()]; //array that stores translations
		
		for(int i = 0; i < parole1.size(); i++ ) { 
			
			String parola = parole1.get(i).getParola_ita(); //italian word provided by the server
			
			//Request to translate from italian to english
			String request = "/get?q=" + parola + "&langpair=it|en"; 
			
			URL richiesta = new URL(url,request); 
			
			//Open connection to the resource denoted by the URL
			URLConnection connection = richiesta.openConnection(); 
			connection.connect();
			
			BufferedReader bd = new BufferedReader(new InputStreamReader(connection.getInputStream())); 
			
			String line = ""; 
			StringBuffer sb = new StringBuffer(); 
			
			while((line = bd.readLine()) != null) { //read the content of the stream 
				sb.append(line);
			}
									
			JSONParser parser = new JSONParser(); 
			JSONObject obj = (JSONObject) parser.parse(sb.toString()); 
			JSONObject tmp = (JSONObject) obj.get("responseData"); 
			String parola_tradotta = (String) tmp.get("translatedText"); //translated word
			parola_tradotta = stringa_standard(parola_tradotta); 
			
			trad[i] = parola_tradotta; //insert the translated word in the specific data structure
		
		}
		
		return trad; 
	}


	
	/* Method that compares translations of the server with those of the players
	 * 
	 * @param parole ---> structure that contains an italian word, an english word and a field for the score 
	 * @param trad ---> array that contains the words translated by the server
	 * 
	 */
	private void controllaTraduzione(ArrayList<ItalianoInglese> parole,String[] trad) throws IOException, ParseException {
		
		for(int i = 0; i < parole.size(); i++) { 
			
			String word = parole.get(i).getParola_eng(); //english word written by the user
			
			System.out.println("***************");
			System.out.println("Parola in italiano: " + parole.get(i).getParola_ita());
			System.out.println("Parola in inglese: " + word);
			System.out.println("Traduzione del server: " + trad[i]);
			System.out.println("***************\n");

			//Check the translation and set the score 
			if(word.equals("") || word.equals("prossimaparola")) {
				parole.get(i).setTot(this.nonData); //no answer 
			} else if (word.equals(trad[i])) {
				parole.get(i).setTot(this.ok); //right answer
			} else {
				parole.get(i).setTot(this.errata); //wrong answer
			}	
		}
	}
	
	
	
	/* Method that counts the number of right answers, wrong answers and missed answers
	 * 
	 */
	private void conta() {
		
		for(int i = 0; i < this.parole1.size(); i++) { //player 1
				
			int p = parole1.get(i).getTot(); //score of a translation
				
			if (p == 0) this.nonDate1++;
			else if (p < 0) this.sbagliate1++;
			else this.corrette1++;
		}
	
		
		
		for(int j = 0; j < this.parole2.size(); j++) { //player 2
				
			int p = parole2.get(j).getTot(); //score of a translation
								
			if (p == 0) this.nonDate2++;
			else if (p < 0) this.sbagliate2++;
			else this.corrette2++;
		}
	}
	
	
	
	/* Method that retrieves the total score of a player
	 * 
	 * @param array ---> structure that contains the score for each translated word
	 * 
	 */
	private int puntiTotali(ArrayList<ItalianoInglese> array) {
		
		int punti = 0; 
		
		for(int i = 0; i < array.size(); i++) { //compute the final score
			
			punti = punti + array.get(i).getTot();
			
		}
		
		return punti; //retrieve the final score
		
	}
	
	
	
	/* Metodo that uniforms a string and deletes all special characters 
	 * 
	 * @param str ---> string to modify
	 * 
	 */
	private String stringa_standard(String str) {
		
		str = str.trim(); //delete empty spaces after and before it
		
		str = str.toLowerCase(); //all characters in lower case
		
		//Replace all special characters with ""
		str = str.replaceAll("[\\-\\^\\.\\,\\_\\!\\£\\$\\%\\&\\?\\@\\#\\:\\'\\;\\+\\*]",""); 
				
		return str; 
		
	}
	
	
	
	public void run() { //task of the thread that handles a match
		
		//Check the state of the challenged user 
		boolean occupo_sfidato = server.occupaUtente(utente2,ch1,main_sel); 
		
		if(occupo_sfidato == false) { //the challenges user is busy
			return;
		}
		
		this.contenuto_dizionario = contenuto_dizionario.replaceAll("\r\n","\n"); 
				
		String[] diz_array = this.contenuto_dizionario.split("\n"); 
				
		for(int i = 0; i < k; i++) { //choose randomically k words for the match
			int rand = (int)(Math.random()*diz_array.length);
			String s = diz_array[rand];
			ItalianoInglese elem1 = new ItalianoInglese(s);
			ItalianoInglese elem2 = new ItalianoInglese(s);
			parole1.add(elem1);
			parole2.add(elem2);
		}
		
		diz_array = null;
		this.contenuto_dizionario = null;
		
		Selector selector = null; //selector that handles the match
		
		try {
			
			selector = Selector.open(); 
			
			SocketAddress sa = this.ch2.getRemoteAddress(); //address of the challenged user
			
			DatagramSocket socket_udp = new DatagramSocket(); 
			String request = "RICHIESTA_SFIDA " + this.utente1 + " " + this.utente2 + " " + this.k + " " + this.durata_partita + " ."; //challenge request
			byte[] array = request.getBytes("UTF-8"); 
			DatagramPacket dp = new DatagramPacket(array,array.length,sa); //datagram where it inserts the request 
			
			socket_udp.send(dp); //send the request via UDP
			socket_udp.close(); 
			
			this.ch2.register(selector,SelectionKey.OP_READ); //register the channel of the challenged user to wait his answer
			selector.selectedKeys().clear();
			
			int chiavi_pronte = selector.select(timeout); 
			
			//If the number of ready keys is zero, the time is expired and the challenged user didn't answer
			if(chiavi_pronte == 0) { 
				
				String s = "Sfida rifiutata ."; //answer to the request
				ByteBuffer bb = ByteBuffer.allocateDirect(s.length() + 1); 
				bb = ByteBuffer.wrap(s.getBytes("UTF-8")); 
				
				while(bb.hasRemaining()) { //read the answer on the channel
					this.ch1.write(bb);
				}
				
				//set users' state to free
				server.cambiaStatoUtente(utente1,"libero");
				server.cambiaStatoUtente(utente2,"libero");
				
				//Keys linked to the channels of the users 
				SelectionKey chiave1 = ch1.keyFor(this.main_sel); 
				SelectionKey chiave2 = ch2.keyFor(this.main_sel);
				
				//Wait for read/write operations on the main server selector
				chiave1.interestOps(SelectionKey.OP_READ);
				chiave2.interestOps(SelectionKey.OP_READ);
				this.main_sel.wakeup(); 
				return;
				
			} else { //the challenged user has answered to the request
				
				String risp = "";
				ByteBuffer buff = ByteBuffer.allocateDirect(256); 
				boolean stop = false;
				
				while(!stop) { 
													
					buff.clear(); 
																					
					ch2.read(buff); //read the answer from the channel and save it into a Byte Buffer
																					
					buff.flip(); 
					
					CharBuffer cb = StandardCharsets.UTF_8.decode(buff); //decode the content of the buffer
					
					risp = risp + cb.toString(); 
					
					if(risp.endsWith(".")) stop = true; //check if the reading is end
					
				}
				
				risp = risp.substring(0,risp.length() - 2);
				String[] arr = risp.split(" ");
				
				if(arr[1].equals("accettata")) { //ANSWER: REQUEST ACCEPTED
					
					String str = "Sfida accettata " + this.k + " " + this.durata_partita + " ."; //answer to send 
					ByteBuffer bb = ByteBuffer.allocateDirect(str.length() + 1); 
					bb = ByteBuffer.wrap(str.getBytes("UTF-8")); 
					
					while(bb.hasRemaining()) {//write the answer on the channel of the user that sent the challenge request
						this.ch1.write(bb);
					}
					
					SelectionKey key = ch2.keyFor(selector);
					key.interestOps(0); 
					
				} else { //ANSWER: REQUEST REJECTED
					
					String tmp = "Sfida rifiutata ."; //answer to send
					ByteBuffer bb = ByteBuffer.allocateDirect(tmp.length() + 1); 
					bb = ByteBuffer.wrap(tmp.getBytes("UTF-8")); 
					
					while(bb.hasRemaining()) {//write the answer on the channel of the user that sent the challenge request
						this.ch1.write(bb);
					}
					
					//set users' state to free				
					server.cambiaStatoUtente(utente1,"libero");
					server.cambiaStatoUtente(utente2,"libero");
					
					//Keys linked to the channels of the users 
					SelectionKey chiave1 = ch1.keyFor(this.main_sel); 
					SelectionKey chiave2 = ch2.keyFor(this.main_sel);
					
					//Wait for read/write operations on the main server selector
					chiave1.interestOps(SelectionKey.OP_READ);
					chiave2.interestOps(SelectionKey.OP_READ);
					this.main_sel.wakeup(); 
					return;
					
				} 
			}
			
			this.ch1.register(selector,SelectionKey.OP_WRITE); //register the channel of the user that sent the challenge request
			this.ch2.keyFor(selector).interestOps(SelectionKey.OP_WRITE); //register the channel of the challenged user
			
			while(!this.finito1 || !this.finito2) { //this loop runs until the players end the words or when the duration of the match expires
								
				selector.selectedKeys().clear();
				
				selector.select(); // blocking method that waits for ready channels
				
				for(SelectionKey key: selector.selectedKeys()) {
					
					try {
					
						//OPERATION: WRITE
						if(key.isWritable()) { //check if a channel is ready to write on it an italian word
													
							SocketChannel channel = (SocketChannel) key.channel();
							
							String parola_da_tradurre = "";
							
							if(channel.equals(this.ch1)) {
								 parola_da_tradurre = this.parole1.get(num1).getParola_ita() + " ."; 
								 num1++;
							} else if (channel.equals(this.ch2)){
								 parola_da_tradurre = this.parole2.get(num2).getParola_ita() + " .";
								 num2++;
							}							
							
							ByteBuffer risp = ByteBuffer.allocateDirect(128); 
							risp = ByteBuffer.wrap(parola_da_tradurre.getBytes("UTF-8")); 
										
							while(risp.hasRemaining()) {
								channel.write(risp); //write the italian word on the channel
							}
														
							key.interestOps(SelectionKey.OP_READ); 
							
						}
						
						//OPERATION: READ
						if(key.isReadable()) { //check if a channel is ready to read a message inside it
							
							SocketChannel channel = (SocketChannel) key.channel();
							
							ByteBuffer bb = ByteBuffer.allocate(128);  
							
							String mess = ""; 
							
							boolean stop = false;
							
							while(!stop) { //read the translate word from the channel 
																
								bb.clear(); 
																								
								channel.read(bb); 
																								
								bb.flip(); 
								
								CharBuffer cb = StandardCharsets.UTF_8.decode(bb); //decode the content of the buffer
								
								mess = mess + cb.toString(); 
								
								if(mess.endsWith(".")) stop = true; 
								
							}
							
							mess = mess.substring(0,mess.length() - 2); 
																																			
							if(mess.equals("FINE_PARTITA")) { //the match is end
								
								key.cancel(); //delete the key linked with the channel
								
								if(channel.equals(this.ch1)) { //check which player has ended the match 
									this.finito1 = true;
								} else {
									this.finito2 = true;
								}
								
							} else { 
							
								mess = stringa_standard(mess); 
								
								//Put the translate word in the specific array, checking the right player that writes it
								if(channel.equals(ch1)) {
																	
									parole1.get(num1 - 1).setParola_eng(mess); 
								
									if(num1 == k) { //player 1 has translated all word of the match 
										key.cancel();
										finito1 = true; 
									} else {
										key.interestOps(SelectionKey.OP_WRITE); 
									}
								
								} else if(channel.equals(this.ch2)){
																	
									parole2.get(num2 - 1).setParola_eng(mess); 
								
									if(num2 == k) { //player 2 has translated all word of the match 
										key.cancel();
										finito2 = true; 
									} else {
										key.interestOps(SelectionKey.OP_WRITE); 
									}
								}
							}							
						}
					
					} catch (IOException ioe) {
						
						//This section handles what happen when a player clicks on 'X' button on the top right corner of the window
						
						System.out.println("Ciclo del selettore in ThreadSfida: " + ioe.getMessage() + "\n");
						
						SocketChannel sc = (SocketChannel) key.channel(); //channel that throws the I/O exception
						
						//Check what player has closed the window and remove him from the structures that handle a match
						if(sc.equals(this.ch1)) { 
							this.ch1.close();
							Utenti_Online.remove(this.utente1);
							this.errore_utente1 = true;
							this.finito1 = true;
						} else {
							this.ch2.close();
							Utenti_Online.remove(this.utente2);
							this.errore_utente2 = true;
							finito2 = true;
						}
						
						key.cancel(); 
						continue;
					}
				}				
			}
			
			//If a player throws an exception, all words of the match are counted as 'missed'
			if(this.errore_utente1 == true) {
				for(int i = 0; i < parole1.size(); i++) {
					parole1.get(i).setParola_ita("");
				}
			}
			
			if(this.errore_utente2 == true) {
				for(int j = 0; j < parole2.size(); j++) {
					parole2.get(j).setParola_ita("");
				}
			}
			
			//Translations from online server
			String[] traduzioni = ottieniTraduzioni(); 
						
			//Check translations
			controllaTraduzione(parole1,traduzioni);
			controllaTraduzione(parole2,traduzioni);
			
			conta(); //count right, wrong and missed answers
			
			int punti1 = 0; 
			int punti2 = 0;
			
			//Compute the score of player 1
			if(this.errore_utente1 == false) {
				punti1 = puntiTotali(parole1);
			} else {
				
				punti1 = Integer.MIN_VALUE;
			}
			
			//Compute the score of player 2
			if(this.errore_utente2 == false) {
				punti2 = puntiTotali(parole2);
			} else {
				
				punti2 = Integer.MIN_VALUE;
			}
			
			String win = "pareggio"; 
			
			if(punti1 > punti2) { //player 1 has won
				punti1 = punti1 + (this.extra*2);
				win = this.utente1;
			}
			
			if(punti1 < punti2){ //player 2 has won 
				punti2 = punti2 + (this.extra*2);
				win = this.utente2;
			}
			
			if(punti1 == punti2) { //the match end in a draw
				punti1 = punti1 + this.extra;
				punti2 = punti2 + this.extra;
			}
			
			//Result of the match for each player
			String[] ris_sfida = new String[2]; 
			ris_sfida[0] = "UTENTE 1 " + this.corrette1 + " " + this.sbagliate1 + " " + this.nonDate1 + " " + punti1 + " " + win + " .";
			ris_sfida[1] = "UTENTE 2 " + this.corrette2 + " " + this.sbagliate2 + " " + this.nonDate2 + " " + punti2 + " " + win + " .";
			
			if(this.errore_utente1 == false) { //send the final result to player 1
				ByteBuffer bb1 = ByteBuffer.allocateDirect(ris_sfida.length); 
				bb1 = ByteBuffer.wrap(ris_sfida[0].getBytes("UTF-8")); 
			
				while(bb1.hasRemaining()) { 
					this.ch1.write(bb1);
				}
			}
			
			if(this.errore_utente2 == false) { //send the final result to player 2
				ByteBuffer bb2 = ByteBuffer.allocateDirect(ris_sfida.length); 
				bb2 = ByteBuffer.wrap(ris_sfida[1].getBytes("UTF-8")); 
				
				while(bb2.hasRemaining()) { 
					this.ch2.write(bb2);
				}
			}
			
			if(this.errore_utente1 == true) { 
				punti1 = 0;
			}
			
			if(this.errore_utente2 == true) { 
				punti2 = 0;
			}
			
			server.aggiornaJSON(this.utente1,this.utente2,punti1,punti2); //update JSON file of scores

		} catch (Exception e) {
			System.out.println("Errore thread sfida server: " + e.getMessage());
			e.printStackTrace();
		}
		
		//Set the state of the players to free
		server.cambiaStatoUtente(utente1,"libero");
		server.cambiaStatoUtente(utente2,"libero");
		
		//Return to main server selector
		if(this.errore_utente1 == false) { 
			SelectionKey chiave1 = ch1.keyFor(this.main_sel);
			chiave1.interestOps(SelectionKey.OP_READ);
		}
		
		//Return to main server selector
		if(this.errore_utente2 == false) {
			SelectionKey chiave2 = ch2.keyFor(this.main_sel);			
			chiave2.interestOps(SelectionKey.OP_READ);
		}
		
		this.main_sel.wakeup(); 
	}
}

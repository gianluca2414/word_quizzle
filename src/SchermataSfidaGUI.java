import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class SchermataSfidaGUI {

	private JFrame frame; //current frame
	private Client client; //istance of Client
	private String utente; //user that send the challenge request
	private String amico; //challenged user
	private JTextField textFieldTraduzione; //text area where a user will write the english words during the match 
	private JLabel lblParolaIta; //label that contains the italian word written by the server
	private JLabel lblAttesaRisposta; //label that contain the final score
	private JLabel lblParolaEng; //label next to the text area where the user will write the english word
	private JLabel lblNumParole; //label that count the number of words to translate 
	private JButton btnConferma; //button to confirm the answer and go ahead 
	private JButton btnProssima; //button to skip the current word 
	private JButton btnHome; //button to go back to the main window 
	private int num_parole = 0; //number of words to translate 
	private JButton btnAccetta; //button to accept a challenge request
	private JButton btnRifiuta; //button to reject a challenge request
	private Timer timer_t1; //Timer thread. It simulates the waiting for the answer of the challenged user  (T1 = 15 seconds)
	private Timer timer_t2; //Timer thread. It simulates the duration of the match (T2 = 60 seconds)
	private TimerTask task_t1; //task to pass to thread timer_t1
	private TimerTask task_t2; //task to pass to timer_t2
	private boolean sfidato; //boolean variable to check if a user has sent the challenge request or has received it 
	
	
	
	
	public SchermataSfidaGUI(JFrame old,String nickUtente,String nickAmico,Client c,boolean s) { //builder
		
		this.frame = new JFrame(); //new frame that contains the components of a match 
		this.client = c; 
		this.utente = nickUtente; //nickUtente
		this.amico = nickAmico; //nickAmico
		
		this.timer_t1 = new Timer();
		this.timer_t2 = new Timer(); 
		
		this.sfidato = s;
		
		//Remove old components from this frame 
		old.getContentPane().removeAll();
		old.getContentPane().revalidate();
		old.getContentPane().repaint();
		
		this.frame = old; 
		initialize(); 
		
	}
	
	
	
	/* Method that creates a thread and call the specific method for a match 
	 * 
	 */
	private void Sfida() {
		
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					client.sfida(utente,amico,SchermataSfidaGUI.this);
				} catch (Exception e) {
					System.out.println("Errore metodo sfida in schermata sfida: " + e.getMessage());
					e.printStackTrace();
				}
			}
		});
	}
	
	
	
	/* Method called when the challenged user rejects the request or when timeout T1 expires
	 * In this case it removes the components of the match and it informs the user that sends the request
	 * 
	 */
	public void sfidaRifiutata() {
		
		lblAttesaRisposta.setVisible(true); //label that contains the reply of the challenged user
		btnHome.setVisible(true); //button to go back to the main window
		
		//Labels and buttons of a match 
		textFieldTraduzione.setVisible(false);
		lblParolaIta.setVisible(false);
		lblParolaEng.setVisible(false);
		btnConferma.setVisible(false);
		btnProssima.setVisible(false);
		lblNumParole.setVisible(false);
		
		lblAttesaRisposta.setText(amico + " ha rifiutato la sfida!"); //comunicate the reject to the user 
	}
	
	
	
	/* Metodo called when the challenged user accepts the request 
	 * It shows the components of the match 
	 * 
	 */
	public void iniziaSfida() { 
		
		//Labels and buttons to accept o reject the request
		lblAttesaRisposta.setVisible(false);
		btnAccetta.setVisible(false);
		btnRifiuta.setVisible(false);
		
		//Labels and buttons of a match
		textFieldTraduzione.setVisible(true);
		lblParolaIta.setVisible(true);
		lblParolaEng.setVisible(true);
		btnConferma.setVisible(true);
		btnProssima.setVisible(true);
		lblNumParole.setVisible(true);
		lblNumParole.setText(num_parole + 1 + "/10");
		
		task_t2 = new TimerTask() { 
			
			@Override
			public void run() {
				String fine = "FINE_PARTITA ."; //message to send to the server 
				
				try {
					
					client.write_to_server(fine); //sedn the message to the serverr
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				esitoSfida(); //result of the match
			}
		};
		
		int T2 = client.durataPartita(); //duration of the match
		
		//The Timer thread will start after timeout T2. In this way I can handle the duration of the match because if the thread will start 
		//so the match will be end. For this reason the task of this thread retrieves the result of the match and the scores of the players
		timer_t2.schedule(task_t2,T2);
		
		try {
			
			String parola_ita = client.read_from_server(); //read the italian word from the server
			lblParolaIta.setText("Parola in italiano: " + parola_ita); //update the label 
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	/* Method that modify the labels to retrieve the result of the match 
	 * 
	 */
	private void esitoSfida() { 
		
		//Labels and buttons of the match
		textFieldTraduzione.setVisible(false);
		lblParolaIta.setVisible(false);
		lblParolaEng.setVisible(false);
		btnConferma.setVisible(false);
		btnProssima.setVisible(false);
		lblNumParole.setVisible(false);
		
		lblAttesaRisposta.setVisible(true); //label that contains the result of the match
		lblAttesaRisposta.setText("In attesa dei risultati ...");
		
		try {
			
			String risp = client.read_from_server(); //read the result of the match from the server

			String[] esito = risp.split(" "); 
			
			String str = "Sfida terminata\n"
					+ "Risposte corrette: " + esito[2] + "\n"
					+ "Risposte errate: " + esito[3] + "\n"
					+ "Risposte non date: " + esito[4] + "\n"
					+ "Punti totalizzati: " + esito[5] + "\n";
			
			System.out.println(str);
		
			//Build a simple report about the match 
			String esito_sfida = "<html> Sfida terminata<br>"
					+ "Risposte corrette:  " + esito[2] + "<br> "
					+ "Risposte errate:  " + esito[3] + "<br>"
					+ "Risposte non date:  " + esito[4] + "<br>"
					+ "Punti totalizzati:  " + esito[5];
		
			//Check the result of the match to inform the user 
			if(esito[6].equals("pareggio")) {
				esito_sfida = esito_sfida + "<br> Sfida finita in parità! </html>";
			} else if(esito[6].equals(utente)) {
				esito_sfida = esito_sfida + "<br> Hai vinto! </html>";
			} else {
				esito_sfida = esito_sfida + "<br> Hai perso! </html>";
			}
			
			lblAttesaRisposta.setText(esito_sfida); //update the specific label 
			btnHome.setVisible(true); //show the button to go back to the main window
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	
	
	/* Method that allows to the challenged user to handle the challenge request
	 * 
	 * @param sfidante ---> user that sends the challenge request
	 * 
	 */
	public void gestisciRichiesta(String sfidante) {
		
		//Labels and buttons of the match 
		textFieldTraduzione.setVisible(false);
		lblParolaIta.setVisible(false);
		lblParolaEng.setVisible(false);
		btnConferma.setVisible(false);
		btnProssima.setVisible(false);
		lblNumParole.setVisible(false);
		btnHome.setVisible(false);
		
		//Buttons to accept or reject the request
		btnAccetta.setVisible(true);
		btnRifiuta.setVisible(true);
		
		//Label that contains the request 
		lblAttesaRisposta.setText(sfidante.toUpperCase() + " ti ha sfidato !!");
		lblAttesaRisposta.setVisible(true);
		
		task_t1 = new TimerTask() { 

			public void run() {
				
				@SuppressWarnings("unused")
				SchermataOperazioniGUI schermata_home = new SchermataOperazioniGUI(client,frame,utente); //costruttore per tornare alla schermata principale
			}
		};
		
		int T1 = 15000; 
		
		//The Timer thread will start after timeout T1. In this way I can handle the waiting of the answer because if the thread will start 
		//so the timeout will be expired. For this reason the task of this thread calls the builder to go back to the main window
		timer_t1.schedule(task_t1,T1);
	}

	
	
	private void initialize() {
		
		frame.setResizable(false);
		frame.setVisible(true);
		frame.getContentPane().setBackground(new Color(135, 206, 250));
		frame.setBounds(100, 100, 600, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		//Label with the name of the game  
		JLabel lblTitolo = new JLabel("WORD QUIZZLE");
		lblTitolo.setForeground(new Color(255, 0, 0));
		lblTitolo.setFont(new Font("Rockwell Extra Bold", Font.BOLD, 40));
		lblTitolo.setBounds(111, 11, 416, 64);
		frame.getContentPane().add(lblTitolo);
		
		//Label with the name of the two players 
		JLabel lblSfidanti = new JLabel(this.utente.toUpperCase() + " vs " + this.amico.toUpperCase());
		lblSfidanti.setHorizontalAlignment(SwingConstants.CENTER);
		lblSfidanti.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 18));
		lblSfidanti.setBounds(10, 84, 574, 26);
		lblSfidanti.setVisible(true);
		frame.getContentPane().add(lblSfidanti);
		
		//Label that contains the result of the match 
		lblAttesaRisposta = new JLabel("In attesa della risposta di " + this.amico.toUpperCase() + " ...");
		lblAttesaRisposta.setHorizontalAlignment(SwingConstants.CENTER);
		lblAttesaRisposta.setFont(new Font("Rockwell Extra Bold", Font.ITALIC, 16));
		lblAttesaRisposta.setBounds(10, 121, 574, 149);
		frame.getContentPane().add(lblAttesaRisposta);
		
		//Label that contains the italian word written by the server
		lblParolaIta = new JLabel("Parola in italiano:");
		lblParolaIta.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		lblParolaIta.setBounds(25, 167, 559, 26);
		lblParolaIta.setVisible(false);
		frame.getContentPane().add(lblParolaIta);
		
		//Label next to the text area where the user will write the translation 
		lblParolaEng = new JLabel("Parola tradotta in inglese:");
		lblParolaEng.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		lblParolaEng.setBounds(25, 204, 286, 19);
		lblParolaEng.setVisible(false);
		frame.getContentPane().add(lblParolaEng);
		
		//Text area where the user will write the english word
		textFieldTraduzione = new JTextField();
		textFieldTraduzione.setBounds(285, 205, 269, 20);
		textFieldTraduzione.setVisible(false);
		textFieldTraduzione.setColumns(10);
		frame.getContentPane().add(textFieldTraduzione);
		
		//Label that mantains the number of words to translate
		lblNumParole = new JLabel("");
		lblNumParole.setHorizontalAlignment(SwingConstants.CENTER);
		lblNumParole.setBackground(new Color(135, 206, 250));
		lblNumParole.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		lblNumParole.setBounds(233, 269, 166, 14);
		lblNumParole.setVisible(false);
		frame.getContentPane().add(lblNumParole);
		
		//Button to confirm the answer
		btnConferma = new JButton("CONFERMA");
		btnConferma.setForeground(new Color(0, 128, 0));
		btnConferma.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 11));
		btnConferma.setBounds(70, 299, 113, 45);
		btnConferma.setVisible(false);
		frame.getContentPane().add(btnConferma);
		
		//Class that implements ActionListener interface and handles the click on the confirm button
		//It takes the word written in the specific text area and sends it to the server
		btnConferma.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String s = textFieldTraduzione.getText();
				s = s + " .";
				textFieldTraduzione.setText("");
				
				try {
					
					client.write_to_server(s);
				
					num_parole++;
					lblNumParole.setText(num_parole + 1 + "/10");
				
					if(num_parole == client.getNumParole()) {
						timer_t2.cancel();
						timer_t2.purge();
						esitoSfida();
					} else {
						String prossima_parola = client.read_from_server();
						prossima_parola = prossima_parola.substring(0,prossima_parola.length() - 2);
						lblParolaIta.setText("Parola in italiano: " + prossima_parola);
					}
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		
		//Button to skip the current italian word 
		btnProssima = new JButton("SALTA");
		btnProssima.setForeground(new Color(255, 0, 0));
		btnProssima.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 11));
		btnProssima.setBounds(439, 299, 113, 45);
		btnProssima.setVisible(false);
		frame.getContentPane().add(btnProssima);
		
		//Class that implements ActionListener interface and handles the click on the skip button
		//It writes a special message to the server to inform it that the user haven't written the english word
		btnProssima.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				String s = "prossimaparola .";
				textFieldTraduzione.setText("");
				
				try {
					
					client.write_to_server(s);
				
					num_parole++;
					lblNumParole.setText(num_parole + 1 + "/10");

				
					if(num_parole == client.getNumParole()) {
						timer_t2.cancel();
						timer_t2.purge();
						esitoSfida();
					} else {
						String prossima_parola = client.read_from_server();
						prossima_parola = prossima_parola.substring(0,prossima_parola.length() - 2);
						lblParolaIta.setText("Parola in italiano: " + prossima_parola);
					}
					
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		});
		
		//Button to go back to the main window 
		btnHome = new JButton("HOME");
		btnHome.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		btnHome.setBounds(253, 297, 119, 45);
		btnHome.setVisible(false);
		frame.getContentPane().add(btnHome);
		
		//Class that implements ActionListener interface and handles the click on the home button
		//The home button appears at the end of the match or if the challenged user rejects the request.
		//When a user clicks on it, it calls the builde method to go back to the main window
		btnHome.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
					@SuppressWarnings("unused")
					SchermataOperazioniGUI schermata = new SchermataOperazioniGUI(client,frame,utente);
			}				
		});
				
		//Button to accept a challege request
		btnAccetta = new JButton("ACCETTA");
		btnAccetta.setForeground(new Color(0, 128, 0));
		btnAccetta.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 13));
		btnAccetta.setBounds(70, 252, 113, 40);
		btnAccetta.setVisible(false);
		frame.getContentPane().add(btnAccetta);
		
		//Class that implements ActionListener interface and handles the click on the accept button
		//The accept button appears when the user receives a challenge request
		//When the user clicks on it, it sends the message to the server, that starts the match and provides the specific GUI for the match
		btnAccetta.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				timer_t1.cancel();
				timer_t1.purge();
				
				int k = client.getNumParole(); //number of words to translate
				int T2 = client.durataPartita(); //duration of the match (in milliseconds)
				
				String str = "Sfida accettata .\nDovrai tradurre " + k + " parole in " + T2/1000 + " secondi .";  
				System.out.println("\n" + str + "\n");
				
				try {
					
					client.write_to_server(str); //write the answer to the server
					
				} catch (IOException ioe) {
					System.out.println("Errore scrittura risposta sfida in gestore sfida: " + ioe);
					ioe.printStackTrace();
				}
				
				iniziaSfida(); //method that starts the match and provides the specific GUI
			}
		});
		
		//Button to reject a challenge request
		btnRifiuta = new JButton("RIFIUTA");
		btnRifiuta.setForeground(new Color(255, 0, 0));
		btnRifiuta.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 13));
		btnRifiuta.setBounds(441, 252, 113, 40);
		btnRifiuta.setVisible(false);
		frame.getContentPane().add(btnRifiuta);
		
		//Class that implements ActionListener interface and handles the click on the reject button
		//The reject button appears when the user receives a challenge request
		//When the user clicks on it, it sends the answer to the server and calls the method to go back to the main window
		btnRifiuta.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				timer_t1.cancel();
				timer_t1.purge();
				
				String str = "Sfida rifiutata ."; //message to send to the server
				System.out.println("\n" + str + "\n");
					
				try {
					
					client.write_to_server(str); //send the answer to the server
					
				} catch (IOException e) {
					System.out.println("Errore scrittura sfida rifiutata in gestore sfida: " + e.getMessage());
					e.printStackTrace();
				}
				
				@SuppressWarnings("unused")
				SchermataOperazioniGUI schermata_home = new SchermataOperazioniGUI(client,frame,utente);
			}
		});
		
		if(this.sfidato == true) {
			Sfida(); 
		} 
	}
}

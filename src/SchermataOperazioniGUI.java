import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import javax.swing.JTextField;
import javax.swing.JList;

/*
 * THIS CLASS IMPLEMENTS THE MAIN WINDOW THAT WILL BE AVAILABLE IMMEDIATLY AFTER A LOGIN OF A USER.
 * I USED SWING FUNCTIONS TO USE LABELS (JLABEL), TEXT AREAS (TEXTFIELD) AND BUTTONS (JBUTTON)
 * 
 */


public class SchermataOperazioniGUI {

	private JFrame frame; //main window
	private String name; //username 
	private Client client; //istance of Client
	private JTextField textFieldAggiungiAmico; //text area where a user will insert a nickname of his friend
	private int i = 0; 
	
	@SuppressWarnings("rawtypes")
	private DefaultListModel dml; //

	
	
	@SuppressWarnings("rawtypes")
	public SchermataOperazioniGUI(Client c,JFrame old,String username) { //builder
		
		this.client = c; 
		frame = new JFrame(); 
		this.name = username; 
		this.dml = new DefaultListModel(); 
		
		//Initialize the frame and remove old components 
		old.getContentPane().removeAll();
		old.getContentPane().revalidate();
		old.getContentPane().repaint();
		
		this.frame = old; 
		initialize(); 
		client.avviaGestore(this,this.name); //start the manager thread of a client 
	}
	
	
	
	/* Method that aims to a challenged client to handles the request with a GUI
	 * 
	 * @param amico ---> user that sends the challenge request
	 * 
	 */
	public void arrivaRichiesta(String amico) {
		SchermataSfidaGUI schermata_sfida = new SchermataSfidaGUI(this.frame,this.name,amico,this.client,false); //new window to handle the request
		schermata_sfida.gestisciRichiesta(amico);
	}
	
	
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	/* Methdo that clears the window 
	 * 
	 * @param punti ---> label that contains the score of a user
	 * @param add_amico ---> label that contains the result of the method to add a friend
	 * @param lista ---> list of friends
	 * 
	 */
	private void clear(JLabel punti,JLabel add_amico,JList lista) {
		punti.setText("");
		add_amico.setText("");
		dml.clear();
		lista.setModel(dml);
	}
	
	
	
	@SuppressWarnings("unchecked")
	//Create ad insert the components in the frame 
	public void initialize() {
		
		frame.setResizable(false);
		frame.getContentPane().setBackground(new Color(135, 206, 250));
		frame.getContentPane().setLayout(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(100, 100, 750, 600);
		frame.getContentPane().setLayout(null);
		
		//Label that contains the title of the main window
		JLabel lblTitolo = new JLabel("WORD QUIZZLE");
		lblTitolo.setForeground(new Color(255, 0, 0));
		lblTitolo.setFont(new Font("Rockwell Extra Bold", Font.BOLD, 40));
		lblTitolo.setBounds(158, 11, 416, 64);
		frame.getContentPane().add(lblTitolo);
		
		//Label that contains the username of the user 
		JLabel lblUtente = new JLabel("BENVENUTO/A " + name.toUpperCase() + ", SELEZIONA UNA TRA LE SEGUENTI OPERAZIONI!");
		lblUtente.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 12));
		lblUtente.setBounds(99, 75, 536, 15);
		frame.getContentPane().add(lblUtente);
		
		//Label that contains the score
		JLabel lblPunti = new JLabel("");
		lblPunti.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 20));
		lblPunti.setHorizontalAlignment(SwingConstants.CENTER);
		lblPunti.setBounds(249, 217, 397, 40);
		frame.getContentPane().add(lblPunti);
		
		//Text area where a user will insert a nickname of a user that want to add as a friend
		textFieldAggiungiAmico = new JTextField();
		textFieldAggiungiAmico.setBackground(new Color(135, 206, 235));
		textFieldAggiungiAmico.setBounds(275, 132, 99, 40);
		frame.getContentPane().add(textFieldAggiungiAmico);
		textFieldAggiungiAmico.setColumns(10);
		textFieldAggiungiAmico.setVisible(false);
		
		//Label that shows the text area where a user will insert the username of his friend
		JLabel lblFreccia = new JLabel("===>");
		lblFreccia.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		lblFreccia.setBounds(230, 143, 46, 14);
		frame.getContentPane().add(lblFreccia);
		lblFreccia.setText("");
		
		//Label that contains the result of adding a friend
		JLabel lblEsitoAggiungiAmico = new JLabel("");
		lblEsitoAggiungiAmico.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 14));
		lblEsitoAggiungiAmico.setHorizontalAlignment(SwingConstants.CENTER);
		lblEsitoAggiungiAmico.setBounds(384, 130, 350, 40);
		frame.getContentPane().add(lblEsitoAggiungiAmico);
		
		@SuppressWarnings("rawtypes")
		//List that contains friends' list or ranking in JSON format
		JList list = new JList();
		list.setVisibleRowCount(20);
		DefaultListCellRenderer dlcr = (DefaultListCellRenderer)list.getCellRenderer();
		dlcr.setHorizontalAlignment(JLabel.CENTER);
		list.setBackground(new Color(135, 206, 250));
		list.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 18));
		list.setBounds(303, 198, 380, 318);
		frame.getContentPane().add(list);
		list.setModel(dml);
		
		//Logout button
		JButton btnLogout = new JButton("LOGOUT");
		btnLogout.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		btnLogout.setForeground(new Color(255, 0, 0));
		btnLogout.setBounds(603, 520, 131, 40);
		frame.getContentPane().add(btnLogout);
		
		//Class that implements ActionListener interface and handles the click on the logout button
		//It calls the logout method and clears the window
		//At the end it calls the builder method to go back to the initial window
		btnLogout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				try {
					
					client.logout(name);
					
					frame.getContentPane().removeAll();
					frame.getContentPane().revalidate();
					frame.getContentPane().repaint();
					
					@SuppressWarnings("unused")
					SchermataInizialeGUI finestra = new SchermataInizialeGUI(client,frame);
					
				} catch (IOException e1) {
					System.out.println("Errore nella logout lato client: " + e1.getMessage());
					e1.printStackTrace();
				}
			}
		});
		
		
		//Button to add a friend 
		JButton btnAggiungiAmico = new JButton("AGGIUNGI AMICO\r\n");
		btnAggiungiAmico.setForeground(new Color(0, 0, 0));
		btnAggiungiAmico.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		btnAggiungiAmico.setBounds(20, 130, 205, 40);
		frame.getContentPane().add(btnAggiungiAmico);
		
		//Class that implements ActionListener interface and handles the click on the button to add a friend
		//If i=0, it will show the text area where a user will be insert the nickname of the friend
		//At the end it analyzes the result and writes it in the specific label next to the text area 
		btnAggiungiAmico.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(i == 0) {
					lblPunti.setText("");
					textFieldAggiungiAmico.setVisible(true);
					lblFreccia.setText("===>");
					i++;
				} else {
					clear(lblEsitoAggiungiAmico,lblPunti,list);
					String amico = textFieldAggiungiAmico.getText().trim();
					textFieldAggiungiAmico.setText("");
					
					try {
						
						String esito = client.aggiungi_amico(name, amico);
						esito = esito.substring(0,esito.length() - 2);
						lblEsitoAggiungiAmico.setText("<html>" + esito + "</html>");
						
					} catch (IOException e1) {
						System.out.println("Errore aggiungi_amico lato client: " + e1.getMessage());
						e1.printStackTrace();
					}
				}
			}
		});
		
		
		//Button to get the list of friends
		JButton btnListaAmici = new JButton("LISTA AMICI");
		btnListaAmici.setForeground(Color.BLACK);
		btnListaAmici.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		btnListaAmici.setBounds(20, 304, 205, 40);
		frame.getContentPane().add(btnListaAmici);
		
		//Class that implements ActionListener interface and handles the click on the button that retrieves the list of friends
		//It calls the specific method, which retrieves a JSON object
		//This object contains an array and if it isn't empty, it can fulfill the support list
		btnListaAmici.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clear(lblPunti,lblEsitoAggiungiAmico,list);
				dlcr.setHorizontalAlignment(JLabel.CENTER);
	
				try {
					
					JSONObject lista_amici = client.lista_amici(name);
					JSONArray array = (JSONArray) lista_amici.get(name);
					if(array == null || array.size() == 0) {
						dml.addElement("Non hai ancora nessun amico!");
					} else {
						dml.addElement("=== LISTA AMICI ===");

						for(int j = 0; j < array.size(); j++) {
							dml.addElement(array.get(j));
						}
						
						dml.addElement("===================");
						list.setModel(dml);
					}
					
				} catch (Exception e1) {
					e1.printStackTrace();
				} 
			}
		});
		
		
		//Score button
		JButton btnPunteggio = new JButton("PUNTEGGIO");
		btnPunteggio.setForeground(Color.BLACK);
		btnPunteggio.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		btnPunteggio.setBounds(20, 217, 205, 40);
		frame.getContentPane().add(btnPunteggio);
		
		//Class that implements ActionListener interface and handles the click on the score button
		//It calls the score method and will insert the result in the specific label
		btnPunteggio.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clear(lblEsitoAggiungiAmico,lblPunti,list);
				
				try {
					
					String punteggio = client.mostra_punteggio(name);
					punteggio = "Punteggio: " + punteggio;
					lblPunti.setText(punteggio);
					
				} catch (IOException e1) {
					System.out.println("Errore nella mostra_punteggio lato client: " + e1.getMessage());					
					e1.printStackTrace();
				}
			}
		});
		
		
		//Ranking button
		JButton btnClassifica = new JButton("CLASSIFICA");
		btnClassifica.setForeground(Color.BLACK);
		btnClassifica.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		btnClassifica.setBounds(20, 391, 205, 40);
		frame.getContentPane().add(btnClassifica);
		
		//Class that implements ActionListener interface and handles the click on the ranking button
		//It calls the ranking method which retrieves a JSON object with key-value pairs
		//If this object isn't null it can fulfill the support list
		btnClassifica.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				clear(lblPunti,lblEsitoAggiungiAmico,list);
				dlcr.setHorizontalAlignment(JLabel.LEFT);
				
				try {
					
					JSONObject classifica = client.mostra_classifica(name);
					JSONArray array = (JSONArray) classifica.get(name);
					
					if(array.size() == 1) {
						list.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 16));
						dml.addElement("Non hai amici con cui poterti sfidare!");
					} else {
						dml.addElement("====== CLASSIFICA =====");
						
						for(int k = 0; k < array.size(); k++) {
							String s = (String)array.get(k).toString();
							s = s.replaceAll("[{}\"]", "");
							dml.addElement(k+1 + ") " + s + " punti");
						}
						
						dml.addElement("========================");
						list.setModel(dml);
					}
					
				} catch (Exception e) {
					System.out.println("Errore mostra classifica lato client: " + e.getMessage());
					e.printStackTrace();
				}
			}
		});
		
		
		//Challenge request button
		JButton btnSfidaUnAmico = new JButton("SFIDA UN AMICO");
		btnSfidaUnAmico.setForeground(Color.BLACK);
		btnSfidaUnAmico.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 15));
		btnSfidaUnAmico.setBounds(20, 483, 205, 40);
		frame.getContentPane().add(btnSfidaUnAmico);
		
		//Class that implements ActionListener interface and handles the click on the challenge request button
		//The user chooses from his friends list the challenged user and it call the builder method of the window that handles the match
		//A user can't start a match if he doesn't choose before a friend from his list
		btnSfidaUnAmico.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				lblPunti.setText("");
				lblEsitoAggiungiAmico.setText("");
				String sfidato = "";
				
				try {
					sfidato = list.getSelectedValue().toString();
					
					@SuppressWarnings("unused")
					SchermataSfidaGUI sfida = new SchermataSfidaGUI(frame,name,sfidato,client,true); 
				
				} catch (Exception e) {
					clear(lblPunti,lblEsitoAggiungiAmico,list);
					dml.addElement("<html> Seleziona un amico da sfidare! </html>");
					list.setModel(dml);
				}				
			}
		});
		
		frame.setVisible(true);
				
	}
}

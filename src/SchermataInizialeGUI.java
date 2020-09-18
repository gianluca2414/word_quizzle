import javax.swing.JFrame;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.NotBoundException;
import java.awt.event.ActionEvent;
import javax.swing.SwingConstants;


/*
 * THIS CLASS IMPLEMENTS THE INITIAL WINDOW, WHERE THERE ARE FIELDS AND BUTTONS TO DO REGISTRATION OR LOGIN
 * I USED SWING FUNCTIONS TO USE LABELS (JLABEL), TEXT AREAS (TEXTFIELD) AND BUTTONS (JBUTTON)
 * 
 */


public class SchermataInizialeGUI {

	public JFrame frame; //initial window
	
	//Text areas where user will insert username and password 
	private JTextField textFieldUsername; 
	private JPasswordField textFieldPassword;
	
	private Client cliente; //istance of Client 
	
	
	
	public SchermataInizialeGUI(Client c,JFrame old) { //builder
		this.cliente = c; 
		frame = new JFrame("WORD QUIZZLE"); //create a frame
		
		if(old != null) {
			old.getContentPane().removeAll();
			old.getContentPane().revalidate();
			old.getContentPane().repaint();
			this.frame = old;
		}
		
		initialize(); 
		frame.setVisible(true);
	}
	

	//Insert the components of a frame
	private void initialize() {
		
		frame.setResizable(false);
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
		
		//Label next to the text area where a user will insert his nickname
		JLabel lblUsername = new JLabel("Username:");
		lblUsername.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 25));
		lblUsername.setBounds(60, 129, 185, 23);
		frame.getContentPane().add(lblUsername);
		
		//Label next to the text area where a user will insert his password
		JLabel lblPassword = new JLabel("Password:");
		lblPassword.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 25));
		lblPassword.setBounds(60, 177, 185, 23);
		frame.getContentPane().add(lblPassword);
		
		//Text area where a user will insert his nickname
		textFieldUsername = new JTextField();
		textFieldUsername.setBounds(234, 132, 249, 20);
		frame.getContentPane().add(textFieldUsername);
		textFieldUsername.setColumns(10);
		
		//Text area where a user will insert his password
		textFieldPassword = new JPasswordField();
		textFieldPassword.setColumns(10);
		textFieldPassword.setBounds(234, 180, 249, 20);
		frame.getContentPane().add(textFieldPassword);
		
		//Label on the registration button
		JLabel lblRegistrazione = new JLabel("Non hai ancora un account WQ ?");
		lblRegistrazione.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 12));
		lblRegistrazione.setBounds(32, 262, 306, 23);
		frame.getContentPane().add(lblRegistrazione);
		
		//Label on the login button
		JLabel lblLogin = new JLabel("Hai gi\u00E0 un account WQ ?");
		lblLogin.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 12));
		lblLogin.setBounds(386, 266, 216, 14);
		frame.getContentPane().add(lblLogin);
		
		//Label where will appear the result of login/registration (at first it's empty)
		JLabel lblEsito = new JLabel("");
		lblEsito.setBackground(new Color(255, 255, 255));
		lblEsito.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 12));
		lblEsito.setHorizontalAlignment(SwingConstants.CENTER);
		lblEsito.setBounds(186, 340, 249, 14);
		frame.getContentPane().add(lblEsito);
		
		//Registration button
		JButton btnRegistrati = new JButton("REGISTRATI "); 
		btnRegistrati.setFont(new Font("Rockwell Extra Bold", Font.PLAIN, 12));
		btnRegistrati.setBounds(64, 291, 158, 38);
		frame.getContentPane().add(btnRegistrati);
		
		//Class that implements ActionListener interface and handles the click on the registration button
		//It retrieves username and password from text areas and calls the registration method
		//At the end it analyzes the result and writes it in the specific label
		btnRegistrati.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String username = textFieldUsername.getText().trim();
				String psw = new String(textFieldPassword.getPassword()).trim();
				
				try {
					
					String esito = cliente.registra_utente(username,psw);
					esito = esito.substring(0,esito.length() - 2); 
					
					if(esito.equals("Registrazione effettuata")) {
						esito = esito.toUpperCase();
						lblEsito.setText(esito);
						lblEsito.setForeground(new Color(0,100,0));
					} else if(esito.equals("Utente già registrato")){
						esito = esito.toUpperCase();
						lblEsito.setText(esito);
						lblEsito.setForeground(Color.BLACK);
					} else if(esito.equals("Registrazione fallita")){
						esito = esito.toUpperCase();
						lblEsito.setText(esito);
						lblEsito.setForeground(Color.RED);
					}
					
				} catch (NotBoundException e) {
					System.out.println("Errore registrazione lato client: " + e.getMessage());
					e.printStackTrace();
				}
				textFieldUsername.setText("");
				textFieldPassword.setText("");
			}
		});
		
		
		//Login button
		JButton btnLogin = new JButton("LOGIN");
		btnLogin.setFont(new Font("Rockwell Nova Extra Bold", Font.PLAIN, 12));
		btnLogin.setBounds(386, 291, 158, 38);
		frame.getContentPane().add(btnLogin);
		
		//Class that implements ActionListener interface and handles the click on the login button
		//It retrieves username and password from text areas and calls the login method
		//At the end it analyzes the result and writes it in the specific label
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String username = textFieldUsername.getText().trim();
				String psw = new String(textFieldPassword.getPassword()).trim();
				
				try {
					
					String esito_login = cliente.login(username,psw);
					System.out.println("LOGIN: " + esito_login);
					if(esito_login.equals("Username e/o password errati .") || esito_login.equals("Utente non registrato .") || esito_login.equals("Errore credenziali .")) {
						esito_login = esito_login.substring(0,esito_login.length() - 2);
						esito_login = esito_login.toUpperCase();
						lblEsito.setText(esito_login);
						lblEsito.setForeground(Color.RED);
						textFieldUsername.setText("");
						textFieldPassword.setText("");
					} else if(esito_login.equals("Utente già connesso .")) {
						esito_login = esito_login.substring(0,esito_login.length() - 2);
						esito_login = esito_login.toUpperCase();
						lblEsito.setText(esito_login);
						lblEsito.setForeground(Color.BLACK);
						textFieldUsername.setText("");
						textFieldPassword.setText("");
					} else {
						
						//If login is ok, create a new object that takes the current window as argument 
						//In this way I can avoid to create a new window, but I will modify the current one 
						@SuppressWarnings("unused")
						SchermataOperazioniGUI schermata = new SchermataOperazioniGUI(cliente,frame,username);
					} 
					
				} catch (IOException e) {
					System.out.println("Errore login lato client: " + e.getMessage());
					e.printStackTrace();
				}
			}
		});
	}
}

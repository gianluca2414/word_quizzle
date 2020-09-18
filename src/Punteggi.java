
/* 
 * CLASS THAT CREATES AN ITEM OF AN ARRAY AND BUILDS THE RANKING
 * A RANKING NEEDS A METHOD THAT COMPARES TWO OBJECT AND THIS METHOD IS IMPLEMENTED IN THIS CLASS
 */


public class Punteggi implements Comparable<Punteggi>{
	
	private String utente; //username
	private int punteggio; //score
	
	
	public Punteggi(String nickUtente, int punti) { //builder
		utente = nickUtente;
		punteggio = punti;
	}
	
	
	
	/* Retrieve the nickname
	 * 
	 */
	public String getUtente() {
		return this.utente;
	}
	
	
	
	/* Retrieve the score
	 * 
	 */
	public int getPunteggio() {
		return this.punteggio;
	}
	
	
	
	/* Set the score 
	 * 
	 */
	public void setPunti (int punti) {
		this.punteggio = punti;
	}
	
	
	
	/* Compare two scores
	 * 
	 * @param pnt ---> object that represents a user and his score
	 * 
	 */
	public int compareTo(Punteggi pnt) {
		
		if(this.punteggio < pnt.punteggio) { //compare two objects and retrieve a value 
			return 1;
		} else if (this.punteggio == pnt.punteggio) {
			return 0;
		} else return -1;
		
	}

}

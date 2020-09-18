
/*
 * CLASS TO REPRESENT A GENERIC ITEM OF AN ARRAY THAT IS MANTAINED FOR EVERY PLAYER OF A MATCH
 * EACH ITEM HAS THREE FIELDS: AN ITALIAN WORD WRITTEN BY THE SERVER, AN ENGLISH WORD WRITTEN BY THE CLIENT AND THE SCORE ACCORDING TO TRANSLATION
 * 
 */


public class ItalianoInglese {
	
	private String parola_ita; //italian word
	private String parola_eng; //english word
	private int tot; //score
	
	
	public ItalianoInglese(String str) { //builder
		
		this.parola_ita = str;
		this.parola_eng = "";
		this.tot = 0;
		
	}

	
	
	public String getParola_ita() { //return the italian word
		return parola_ita;
	}
	
	

	public void setParola_ita(String parola_ita) {  //set a italian word
		this.parola_ita = parola_ita;
	}
	
	

	public String getParola_eng() { //return the english word
		return parola_eng;
	}
	
	

	public void setParola_eng(String parola_eng) { //set a english word
		this.parola_eng = parola_eng;
	}
	
	

	public int getTot() { //return the score of a translation
		return tot;
	}
	
	

	public void setTot(int tot) { //set the score of a translation
		this.tot = tot;
	}
}

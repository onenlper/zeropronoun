package em;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

import model.Mention;
import em.EMUtil.Animacy;
import em.EMUtil.Gender;
import em.EMUtil.Number;
import em.EMUtil.Person;

public class ResolveGroupNAACL implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	String pronoun;
	
	ArrayList<EntryNAACL> entries;
	
	HashMap<String, Double> animacyConf;
	HashMap<String, Double> genderConf;
	HashMap<String, Double> personConf;
	HashMap<String, Double> numberConf;
	
	public ResolveGroupNAACL(String pro) {
		this.pronoun = pro;
		
		animacyConf = new HashMap<String, Double>();
		genderConf = new HashMap<String, Double>();
		personConf = new HashMap<String, Double>();
		numberConf = new HashMap<String, Double>();
		
		if(EMUtil.pronouns.contains(pro)) {
			String animacy = EMUtil.getAnimacy(pro).name();
			String gender = EMUtil.getGender(pro).name();
			String person = EMUtil.getPerson(pro).name();
			String number = EMUtil.getNumber(pro).name();
			
			animacyConf.put(animacy, 1.0);
			genderConf.put(gender, 1.0);
			personConf.put(person, 1.0);
			numberConf.put(number, 1.0);
		} else {
			Common.bangErrorPOS("");
		}
		
		this.entries = new ArrayList<EntryNAACL>();
	}
	
	public ResolveGroupNAACL(HashMap<String, Double> animacyConf, HashMap<String, Double> genderConf,
			HashMap<String, Double> personConf, HashMap<String, Double> numberConf) {
		this.animacyConf = animacyConf;
		this.genderConf = genderConf;
		this.personConf = personConf;
		this.numberConf = numberConf;
		
		this.entries = new ArrayList<EntryNAACL>();
	}

	public static class EntryNAACL implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		ContextNAACL context;
		String head;
		
		Animacy animacy;
		Gender gender;
		Person person;
		Number number;
		
		double p_c;
		
		double p;
		boolean sameSpeaker;
		boolean firstSubj;
		

		public EntryNAACL(Mention ant, ContextNAACL context, boolean sameSpeaker, boolean firstSubj) {
			this.head = ant.head;
			this.context = context;
			this.sameSpeaker = sameSpeaker;
			
			this.animacy = EMUtil.getAntAnimacy(ant);
			this.person = EMUtil.getAntPerson(ant.head);
			
			this.gender = EMUtil.getAntGender(ant);
			this.number = EMUtil.getAntNumber(ant);
			this.firstSubj = firstSubj;
		}
	}
}

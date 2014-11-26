package em;

import java.io.Serializable;
import java.util.ArrayList;

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

//	String pronoun;
	
	short pronoun;

	ArrayList<EntryNAACL> entries;
	
	public ResolveGroupNAACL(short pro) {
		this.pronoun = pro;
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

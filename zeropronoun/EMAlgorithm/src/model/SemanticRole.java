package model;

import java.util.ArrayList;
import java.util.HashMap;

public class SemanticRole implements Comparable<SemanticRole>{
	
	public ArrayList<Mention> arg0;

	public ArrayList<Mention> arg1;
	
	public ArrayList<Mention> tmp;
	
	public HashMap<String, ArrayList<Mention>> args = new HashMap<String, ArrayList<Mention>>();
	
	public Mention predicate;
	
	public SemanticRole() {
		this.arg0 = new ArrayList<Mention>();
		this.arg1 = new ArrayList<Mention>();
		this.tmp = new ArrayList<Mention>();
		this.predicate = new Mention();
	}

	@Override
	public int compareTo(SemanticRole arg0) {
		return this.predicate.start-((SemanticRole)arg0).predicate.start;
	}

}

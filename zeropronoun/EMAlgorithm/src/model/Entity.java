package model;

import java.util.ArrayList;
import java.util.Collections;

public class Entity implements Comparable<Entity>{

	/**
	 * 
	 */
	public boolean singleton = false;

	public ArrayList<Mention> mentions;
	public String type;
	public String subType;
	public int entityIdx;
	
	public double score = 0;

	public ArrayList<Mention> getMentions() {
		return mentions;
	}

	public void addMention(Mention em) {
//		em.entity = this;
		this.mentions.add(em);
	}

	public void setMentions(ArrayList<Mention> mentions) {
		this.mentions = mentions;
	}

	public Mention getFirstMention() {
		Collections.sort(mentions);
		return mentions.get(0);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSubType() {
		return subType;
	}

	public void setSubType(String subType) {
		this.subType = subType;
	}

	public Entity() {
		mentions = new ArrayList<Mention>();
	}

	Mention mostReppresent;

//	public EntityMention getMostRepresent() {
//		Collections.sort(this.mentions);
//		this.mostReppresent = this.mentions.get(0);
//		for (EntityMention m : mentions) {
//			if (m.moreRepresentativeThan(this.mostReppresent)) {
//				this.mostReppresent = m;
//			}
//		}
//		return this.mostReppresent;
//	}

	public int compareTo(Entity emp2) {
		return this.mentions.get(0).compareTo(emp2.mentions.get(0));
		// Collections.sort(mentions);
		// Collections.sort(emp2.mentions);
		// int diff = mentions.get(0).headStart -
		// emp2.mentions.get(0).headStart;
		// if (diff == 0)
		// return mentions.get(0).headEnd - emp2.mentions.get(0).headEnd;
		// return diff;
	}

	public String print() {
		StringBuilder sb = new StringBuilder();
		for (Mention m : this.mentions) {
			sb.append(m.extent).append(" ");
		}
		sb.append(this.mentions.get(0).start).append(":");
		sb.append(this.mentions.get(this.mentions.size() - 1).end);
		return sb.toString().trim();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Type: ").append(type).append(" SubType: ").append(subType).append("\n");
		for (Mention em : mentions) {
			sb.append(em.toString()).append("\n");
		}
		return sb.toString();
	}
}

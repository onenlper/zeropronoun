package model;

import java.io.Serializable;

import model.CoNLL.CoNLLSentence;
import model.syntaxTree.MyTreeNode;
import em.EMUtil;


public class Mention implements Comparable<Mention>, Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int start = -1;
	public int end = -1;
	public String extent = "";

	public Entity entity;
	
	public Mention antecedent;
	
	public String msg;
	
	public double MI;
	
	public boolean notInChainZero;
	
	public int sentenceID;
	
	public CoNLLSentence s;

	public String head = "";

	public int entityIndex;
	
	public int startInS;
	public int endInS;
	
	public int headInS;

	public EMUtil.Grammatic gram;
	public EMUtil.MentionType mType;
	
	public EMUtil.Number number;
	public EMUtil.Gender gender;
	public EMUtil.Person person;
	public EMUtil.Animacy animacy;
	
	public MyTreeNode V;
	
	public MyTreeNode NP;
	
	public String NE = "OTHER";
	
	public boolean isFS = false;
	
	public boolean isBest = false;
	
	//TODO
	public boolean isQuoted = false;
	
	public int getSentenceID() {
		return sentenceID;
	}

	public void setSentenceID(int sentenceID) {
		this.sentenceID = sentenceID;
	}

	public int hashCode() {
		String str = this.start + "," + this.end;
		return str.hashCode();
	}

	public boolean equals(Object em2) {
		if (this.start == ((Mention) em2).start
				&& this.end == ((Mention) em2).end) {
			return true;
		} else {
			return false;
		}
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public String getExtent() {
		return extent;
	}

	public void setExtent(String extent) {
		this.extent = extent;
	}

	public String getHead() {
		return head;
	}

	public void setHead(String head) {
		this.head = head;
	}

	public Mention() {

	}

	public Mention(int start, int end) {
		this.start = start;
		this.end = end;
	}

	// (14, 15) (20, -1) (10, 20)
	public int compareTo(Mention emp2) {
		int diff = this.start - emp2.start;
		if (diff == 0)
			return emp2.end - this.end;
		else
			return diff;
		// if(this.getE()!=-1 && emp2.getE()!=-1) {
		// int diff = this.getE() - emp2.getE();
		// if(diff==0) {
		// return this.getS() - emp2.getS();
		// } else
		// return diff;
		// } else if(this.getE()==-1 && emp2.headEnd!=-1){
		// int diff = this.getS() - emp2.getE();
		// if(diff==0) {
		// return -1;
		// } else
		// return diff;
		// } else if(this.headEnd!=-1 && emp2.headEnd==-1){
		// int diff = this.getE() - emp2.getS();
		// if(diff==0) {
		// return 1;
		// } else
		// return diff;
		// } else {
		// return this.getS()-emp2.getS();
		// }
	}

	public String toName() {
		String str = this.start + "," + this.end;
		return str;
	}

	public String toString() {
		String str = this.start + "," + this.end;
		return str;
	}
}
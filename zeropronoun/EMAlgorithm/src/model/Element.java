package model;

public class Element implements Comparable{
	public int start;
	public int end;
	public String content;
	
	public double confidence;
	
	public Element() {
		
	}
	
	public boolean equals(Object element) {
		if(((Element)element).start == this.start && ((Element)element).end==this.end) {
			return true;
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.start).append(",").append(this.end);
		return sb.toString().hashCode();
	}
	
	// whether contains preposition
	boolean containPP;
	
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

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public boolean isContainPP() {
		return containPP;
	}

	public void setContainPP(boolean containPP) {
		this.containPP = containPP;
	}

	public Element(int start, int end, String content) {
		this.start = start;
		this.end = end;
		this.content = content;
		this.setContainPP(false);
	}
	
	public double getConfidence() {
		return confidence;
	}

	public void setConfidence(double confidence) {
		this.confidence = confidence;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	Object object;
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(start).append(" ").append(end).append(" ").append(content);
		return sb.toString();
	}

	@Override
	public int compareTo(Object arg0) {
		// TODO Auto-generated method stub
		if(this.start==((Element)arg0).start) {
			return this.end - ((Element)arg0).end;
		} else {
			return this.start - ((Element)arg0).start;
		}
	}
}

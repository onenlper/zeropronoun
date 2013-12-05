package model;

import java.util.HashMap;

public class DependSentence {

	public HashMap<Integer, GraphNode> nodes;
	
	public DependSentence() {
		this.nodes = new HashMap<Integer, GraphNode>();
	}
}

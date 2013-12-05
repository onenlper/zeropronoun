package model;

import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

public class GraphNode {

	public ArrayList<GraphNode> nexts;
	public HashMap<Integer, String> edgeName;
	
	public GraphNode backNode;
	
	public String token;
	public String POS;
	public int index;
	public int linkTo;
	public String dep;

	public GraphNode(String token, String POS, int linkTo, String dep, int index) {
		this.token = token;
		this.POS = POS;
		this.linkTo = linkTo;
		this.dep = dep;
		this.index = index;
		this.nexts = new ArrayList<GraphNode>();
		this.edgeName = new HashMap<Integer, String>();
	}

	public void addEdge(GraphNode node, String edgeName) {
		if (node == null || edgeName == null) {
			Common.bangErrorPOS("DONOT insert null");
		}

		if (!this.edgeName.containsKey(node.index)) {
			this.nexts.add(node);
			this.edgeName.put(node.index, edgeName);
		}
	}

	public String getEdgeName(GraphNode n2) {
		return this.edgeName.get(n2.index);
	}

}

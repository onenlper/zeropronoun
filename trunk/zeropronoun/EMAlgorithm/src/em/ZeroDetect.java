package em;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.CoNLL.OntoCorefXMLReader;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import util.Common;

public class ZeroDetect {

	public static ArrayList<Mention> getHeuristicZeros(CoNLLPart part) {
		ArrayList<Mention> mentions = new ArrayList<Mention>();

		for (CoNLLSentence s : part.getCoNLLSentences()) {
			HashSet<Integer> candidates = new HashSet<Integer>();
			MyTree tree = s.syntaxTree;
			MyTreeNode root = tree.root;
			visitTreeNode(root, candidates, s);

			for (Integer can : candidates) {
				Mention m = new Mention();
				m.start = can;
				m.end = -1;
				m.sentenceID = s.getSentenceIdx();

				if (isHeruisZP(m, part))
					mentions.add(m);
			}
		}
		Collections.sort(mentions);
		return mentions;
	}

	private static boolean isHeruisZP(Mention zero, CoNLLPart part) {
//		if (true) {
//			return true;
//		}
		CoNLLWord word = part.getWord(zero.start);
		CoNLLSentence s = word.sentence;
		MyTree tree = word.sentence.syntaxTree;
		MyTreeNode root = tree.root;
		int rightIdx = word.indexInSentence;
		MyTreeNode Wr = tree.leaves.get(rightIdx);
		ArrayList<MyTreeNode> WrAncestors = Wr.getAncestors();
		MyTreeNode Wl = null;
		MyTreeNode temp = null;

		MyTreeNode V = null;
		
		for (MyTreeNode node : WrAncestors) {
			if (node.value.toLowerCase().startsWith("vp")
					&& node.getLeaves().get(0) == Wr
					&& (node.parent.value.equalsIgnoreCase("ip"))) {
				V = node;
			}
		}
		if (V == null) {
			return false;
		}
		
		for(int i=0;i<V.childIndex;i++) {
			if(V.parent.children.get(i).value.equals("VP")) {
				return false;
			}
		}

		MyTreeNode IP = V.parent;

		if (word.index == 0) {
			return false;
		}

		boolean has_Ancestor_NP = false;
		temp = V;
		while (temp != root) {
			if (temp.value.toLowerCase().startsWith("np")
					|| temp.value.toLowerCase().startsWith("qp")
					) {
				has_Ancestor_NP = true;
			}
			temp = temp.parent;
		}
		if (has_Ancestor_NP) {
			return false;
		}
		
		boolean firstGap = word.indexInSentence == 0;
		if (firstGap) {
			return true;
		} else {
			int leftIdx = rightIdx - 1;
			Wl = tree.leaves.get(leftIdx);
			if(Wl.value.equals("，") || Wl.value.equals(",")) {
				Wl = tree.leaves.get(leftIdx - 1);
			}
			ArrayList<MyTreeNode> WlAncestors = Wl.getAncestors();
//			boolean Pl_Is_ObjNP = false;
//			for(int i=WlAncestors.size()-1;i>=0;i--) {
//				MyTreeNode node = WlAncestors.get(i);
//				if((node.value.equals("np") || node.value.equals("qp") || node.value.equals("ip"))
//						&& node.parent!=null && node.parent.equals("VP")) {
//					Pl_Is_ObjNP = true;
//				}
//			}
			
			int m = 0;
			MyTreeNode Pl = WlAncestors.get(m);
			MyTreeNode Pr = WrAncestors.get(m);
			m++;
			while (true) {
				if (Pl == Pr) {
					m++;
					Pl = WlAncestors.get(m);
					Pr = WrAncestors.get(m);
				} else {
					break;
				}
			}
			// Pl_Is_NP
			boolean Pl_Is_ObjNP = (Pl.value.toLowerCase().startsWith("np")
					|| Pl.value.toLowerCase().startsWith("qp") 
					|| Pl.value.toLowerCase().startsWith("ip")
					|| Pl.value.toLowerCase().startsWith("lcp")
					)
					&& (Pl.parent.value.equals("VP"))
					;
			
			boolean hasSubject = false;
			ArrayList<MyTreeNode> leftSisters = V.getLeftSisters();
			for (MyTreeNode n : leftSisters) {
				if (n.value.equalsIgnoreCase("np")
						|| n.value.equalsIgnoreCase("qp")) {
					hasSubject = true;
					break;
				}

				if (n.value.toLowerCase().startsWith("ip")||n.value.toLowerCase().startsWith("lcp")
						||n.value.toLowerCase().startsWith("dp")) {
					hasSubject = true;
					break;
				}
			}
			
			if(IP.childIndex!=0 && IP.parent.value.equals("PP") && IP.parent.children.get(IP.childIndex-1).value.equals("P")
					&& IP.parent.parent.value.equals("VP")
					&& IP.parent.parent.childIndex!=0 && IP.parent.parent.parent.children.get(IP.parent.parent.childIndex-1).value.equals("NP")) {
//				return false;
			}
			
			if (hasSubject) {
				return false;
			}

			if (Pl_Is_ObjNP) {
				return false;
			}
			
			
			// IP as OBJ
			if (IP.parent.value.equals("VP")) {
//				if(IP.childIndex != 0
//					&& (IP.parent.children.get(IP.childIndex - 1).value
//							.equals("VV"))) {
//					return false;
//				}
				
				for(int i=IP.childIndex-1;i>=0;i--) {
					if(IP.parent.children.get(i).value.equals("VV")) {
						return false;
					} else if(IP.parent.children.get(i).value.equals("AS")
//							|| IP.parent.children.get(i).children.get(0).value.equals("，")
							){
						continue;
					} else {
						break;
					}
				}
				
				for(int i=0;i<IP.childIndex-1;i++) {
					if(IP.parent.children.get(i).value.equals("VV") && IP.parent.children.get(i+1).value.equals("NP")) {
						return false;
					}
				}
			}

			if (!goldInts.contains(zero.start)) {
//				System.out.println(part.getDocument().getFilePath());
//				System.out.println(word.getWord() + " " + zero.start);
//				System.out.println(s.getText());
//				System.out.println(Pl_Is_ObjNP + "-" + hasSubject + ":\t"
//						+ (goldInts.contains(zero.start) ? "zero" : "nonzero"));
//				System.out.println("-----");
			}
			return true;
		}
	}

	static HashMap<String, Integer> pus = new HashMap<String, Integer>();

	static HashMap<String, Integer> map = new HashMap<String, Integer>();

	private static void visitTreeNode(MyTreeNode node, HashSet<Integer> zeros,
			CoNLLSentence s) {
		if (node.value.equalsIgnoreCase("VP")) {
			boolean CC = false;
			// if in CC construct
			for (MyTreeNode temp : node.parent.children) {
				if (temp.value.equalsIgnoreCase("CC")) {
					CC = true;
				}
			}

			boolean advp = false;
			ArrayList<MyTreeNode> leftSisters = node.getLeftSisters();
			for (int k = leftSisters.size() - 1; k >= 0; k--) {
				MyTreeNode leftSister = leftSisters.get(k);
				if (leftSister.value.equals("ADVP")) {
					if (leftSister.parent.value.equals("VP")) {
						advp = true;
						break;
					}
				}
			}

			int leafIdx = node.getLeaves().get(0).leafIdx;
			 if (!CC && !advp)
			zeros.add(s.getWord(leafIdx).index);
		}
		for (MyTreeNode child : node.children) {
			visitTreeNode(child, zeros, s);
		}
	}

	static HashSet<Integer> goldInts = new HashSet<Integer>();

	public static void main(String args[]) {
		double gold = 0;
		double sys = 0;
		double hit = 0;
		String folder = "all";
		ArrayList<String> files = Common.getLines("chinese_list_" + folder
				+ "_development");

		for (String file : files) {
			CoNLLDocument doc = new CoNLLDocument(file
					.replace("auto_conll", "gold_conll")
					);
			OntoCorefXMLReader.addGoldZeroPronouns(doc, true);

			for (CoNLLPart part : doc.getParts()) {
				ArrayList<Mention> goldZeros = EMUtil.getAnaphorZeros(part
						.getChains());
//				 ArrayList<Mention> goldZeros = EMUtil.getZeros(part
//				 .getChains());
				goldInts.clear();
				for (Mention z : goldZeros) {
					goldInts.add(z.start);
				}

				ArrayList<Mention> systemZeros = getHeuristicZeros(part);

				gold += goldZeros.size();
				sys += systemZeros.size();
				out: for (Mention g : goldZeros) {
					for (Mention s : systemZeros) {
						if (s.start == g.start) {
							hit++;
							continue out;
						}
					}
//					CoNLLWord word = part.getWord(g.start);
//					CoNLLSentence s = word.sentence;
//					//d
//					System.out.println(part.getDocument().getFilePath());
//					System.out.println(word.getWord());
//					System.out.println(s.getText());
//					System.out.println("-----");
					
				}
			}
		}
		double p = hit / sys;
		double r = hit / gold;
		double f = 2 * p * r / (p + r);
		System.out.println("Hit: " + hit);
		System.out.println("Gold: " + gold);
		System.out.println("Sys: " + sys);
		System.out.println("R: " + r*100);
		System.out.println("P: " + p*100);
		System.out.println("F: " + f*100);

		// for (String key : map.keySet()) {
		// System.out.println(key + "\t:" + map.get(key));
		// }
		// System.out.println("======");
		// System.out.println(map.size());
		// for (String key : pus.keySet()) {
		// System.out.println(key + ":" + pus.get(key));
		// }
	}

}

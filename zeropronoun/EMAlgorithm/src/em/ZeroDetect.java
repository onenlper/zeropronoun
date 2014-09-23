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

	static HashMap<String, Integer> map2 = new HashMap<String, Integer>();
	
	private static boolean isHeruisZP(Mention zero, CoNLLPart part) {
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
		// Rule 3
		if (V == null) {
			return false;
		}
		
		MyTreeNode IP = V.parent;

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
		// Rule 4
		if (has_Ancestor_NP) {
//			if (!goldInts.contains(zero.start)) {
//				System.out.println(part.getDocument().getFilePath());
//				System.out.println(word.getWord() + " " + zero.start);
//				System.out.println(s.getText());
//				System.out.println(goldInts.contains(zero.start) ? "zero" : "nonzero");
//				System.out.println("-----");
//			}
			return false;
		}
		
		
		boolean firstGap = word.indexInSentence == 0;
		
		if (!firstGap) {
			int leftIdx = rightIdx - 1;
			Wl = tree.leaves.get(leftIdx);
			if(Wl.value.equals("，") || Wl.value.equals(",")) {
				Wl = tree.leaves.get(leftIdx - 1);
			}
			ArrayList<MyTreeNode> WlAncestors = Wl.getAncestors();
			
			int m = 0;
			MyTreeNode Pl = WlAncestors.get(m);
//			MyTreeNode Pr = WrAncestors.get(m);
//			m++;
//			while (true) {
//				if (Pl == Pr) {
//					m++;
//					Pl = WlAncestors.get(m);
//					Pr = WrAncestors.get(m);
//				} else {
//					break;
//				}
//			}
			// Pl_Is_NP
			for(MyTreeNode node : WlAncestors) {
				if(node.getLeaves().get(node.getLeaves().size()-1)==Wl) {
					Pl = node;
					break;
				}
			}
			
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
						||n.value.toLowerCase().startsWith("dp")
						||n.value.toLowerCase().startsWith("dnp")
						) {
					hasSubject = true;
					break;
				}
			}
			// Rule 5
			if (hasSubject) {
				return false;
			}
			
			// Rule 6
			if (Pl_Is_ObjNP) {
				return false;
			}
			
			// IP as OBJ
			// Rule 7
			if (IP.parent.value.equals("VP")) {
				
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
		}
		// Rule 8
		if (word.index == 0) {
			return false;
		}
		
//		if(word.sentence.getSentenceIdx()==0) {
//			boolean np = false;
//			for(int i=0;i<word.indexInSentence;i++) {
//				MyTreeNode leaf = tree.leaves.get(i);
//				if(leaf.getXAncestors("NP").size()!=0) {
//					np = true;
//					break;
//				}
//			}
//			if(!np) {
////				return false;
//			}
//		}
		
		
		// Rule 9
//		possessive you3, existential you3
		for(MyTreeNode tn: IP.getLeaves()) {
			if(tn.parent.value.startsWith("V")) {
				if((tn.value.equals("没有") || tn.value.equals("有") || tn.value.equals("无")) && 
						tn.parent.value.equals("VE")) {
//					return false;					 temporary comment out
				}
				break;
			}
		}
		
		// rule 10?
		HashSet<String> set = new HashSet<String>();
		
		MyTreeNode verb = null;
		for(MyTreeNode tn : IP.getLeaves()) {
			if(true 
					&& !tn.parent.value.equals("PU") 
					&& !tn.parent.value.equals("SP")
					&& !tn.parent.value.equals("AS")
					) {
				set.add(tn.value);
			}
			if(tn.parent.value.startsWith("V")) {
				if(verb==null) {
					verb = tn; 
				}
			}
		}
		if(verb==null) {
//			return false; temporary comment out
		}
		
		if(set.size()==1) {
			String key = set.iterator().next();
			if(map2.containsKey(key)) {
				map2.put(key, map2.get(key) + 1);
			} else {
				map2.put(key, 1);
			}
			if(verb!=null && (verb.parent.value.equals("VC") || verb.parent.value.equals("VA"))) {
				if(!goldInts.contains(zero.start)) {
//					System.out.println(set.toString() + " # " + part.getDocument().getDocumentID() + "#" + verb.parent.value
//							+ "$$" + (goldInts.contains(zero.start) ? "zero" : "nonzero"));
//					System.out.println(part.getDocument().getFilePath());
//					System.out.println(word.getWord() + " " + zero.start);
//					System.out.println(s.getText());
//					System.out.println(goldInts.contains(zero.start) ? "zero" : "nonzero");
//					System.out.println("-----");
					}
//				return false; temporary comment out
			}
		}
		
		if (!goldInts.contains(zero.start)) {
//			System.out.println(part.getDocument().getFilePath());
//			System.out.println(word.getWord() + " " + zero.start);
//			System.out.println(s.getText());
//			System.out.println(goldInts.contains(zero.start) ? "zero" : "nonzero");
//			System.out.println("-----");
		}
		
		return true;
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
			// Rule 2
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
		String folder = args[0];
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

		int all = 0;
		for(String key : map2.keySet()) {
//			System.out.println(key + " # " + map2.get(key));
			all += map2.get(key);
		}
		System.out.println(all);
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

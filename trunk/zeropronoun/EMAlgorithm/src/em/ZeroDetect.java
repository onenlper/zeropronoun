package em;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

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
				if(isHeruisZP(m, part))
					mentions.add(m);
			}
		}
		Collections.sort(mentions);
		return mentions;
	}

	private static boolean isHeruisZP(Mention zero, CoNLLPart part) {
		CoNLLWord word = part.getWord(zero.start);
		MyTree tree = word.sentence.syntaxTree;
		MyTreeNode root = tree.root;
		MyTreeNode leaf = root.getLeaves().get(word.indexInSentence);
		int rightIdx = word.indexInSentence;
		MyTreeNode Wr = tree.leaves.get(rightIdx);
		ArrayList<MyTreeNode> WrAncestors = Wr.getAncestors();
		MyTreeNode Wl = null;
		MyTreeNode C = root;
		MyTreeNode temp = null;
		
		MyTreeNode V = null;
		for (MyTreeNode node : WrAncestors) {
			if (node.value.toLowerCase().startsWith("vp") && node.getLeaves().get(0) == Wr) {
				V = node;
			}
		}
		
		boolean firstGap = word.indexInSentence == 0;
		if (firstGap) {
			boolean find_IP_VP = false;
			temp = Wr;
			while (temp != root) {
				if (temp.value.toLowerCase().startsWith("vp")
						&& temp.parent.value.toLowerCase().startsWith("ip")) {
					find_IP_VP = true;
					break;
				}
				temp = temp.parent;
			}
			if (!find_IP_VP) {
				return false;
			}
			
			
			boolean has_Ancestor_NP = false;
			temp = V;
			while (temp != root) {
				// try {
				if (temp.value.toLowerCase().startsWith("np")) {
					has_Ancestor_NP = true;
				}
				temp = temp.parent;
			}
			if(has_Ancestor_NP) {
				return false;
			} else {
				return true;
			}
			
		} else {
			int leftIdx = rightIdx - 1;
			Wl = tree.leaves.get(leftIdx);
			ArrayList<MyTreeNode> WlAncestors = Wl.getAncestors();
			int m = 0;
			MyTreeNode P = root;
			MyTreeNode Pl = WlAncestors.get(m);
			MyTreeNode Pr = WrAncestors.get(m);
			m++;
			while (true) {
				if (Pl == Pr) {
					P = Pl;
					m++;
					Pl = WlAncestors.get(m);
					Pr = WrAncestors.get(m);
				} else {
					break;
				}
			}
			C = P;
			temp = Wr;
			boolean find_IP_VP = false;
			while (temp != C) {
				if (temp.value.toLowerCase().startsWith("vp")
						&& temp.parent.value.toLowerCase().startsWith("ip")) {
					find_IP_VP = true;
					break;
				}
				temp = temp.parent;
			}
			if (!find_IP_VP) {
				return false;
			}
			
			boolean subjectlessIP = false;
			ArrayList<MyTreeNode> ipAns = leaf.getXAncestors("IP");
			outer: for (MyTreeNode ip : ipAns) {
				if (ip.getLeaves().get(0) == leaf) {
					for (MyTreeNode child : ip.children) {
						if (child.value.startsWith("NP")) {
							break;
						}
						if (child.value.startsWith("VP")) {
							subjectlessIP = true;
							break outer;
						}
					}
				}
			}
			int weight = 0;
			if (subjectlessIP) {
				weight++;
			} else {
				weight--;
			}

			boolean nosubject = true;
			for (MyTreeNode tmp : leaf.getXAncestors("VP")) {
				if (tmp.getLeaves().get(0) == leaf) {
					ArrayList<MyTreeNode> leftSisters = tmp.getLeftSisters();
					for (MyTreeNode n : leftSisters) {
						if (n.value.equalsIgnoreCase("np") 
//								&& !n.getLeaves().get(n.getLeaves().size()-1).parent.value.equals("NT")
								) {
							nosubject = false;
						}
					}
				}
			}
			if(nosubject) {
				weight++;
			} else {
				weight--;
			}
			
			// 1. Pl_Is_NP
			boolean Pl_Is_NP = Pl.value.toLowerCase().startsWith("np");
			if (Pl_Is_NP) {
				weight--;
			} else {
				weight++;
			}
			// 2. Pr_Is_VP
			boolean Pr_Is_VP = Pr.value.toLowerCase().startsWith("vp");
			if (Pr_Is_VP) {
				weight++;
			} else {
				weight--;
			}
			// 3. Pl_IS_NP && Pr_IS_VP
			boolean Pl_IS_NP_Pr_IS_VP = Pl.value.toLowerCase().startsWith("np")
					&& Pr.value.toLowerCase().startsWith("vp");
			if (Pl_IS_NP_Pr_IS_VP) {
				weight++;
			} else {
				weight--;			
			}
			
			boolean has_Ancestor_NP = false;
			temp = V;
			while (temp != root) {
				// try {
				if (temp.value.toLowerCase().startsWith("np")) {
					has_Ancestor_NP = true;
				}
				temp = temp.parent;
			}
			if(has_Ancestor_NP) {
				weight--;			
			} else {
				weight++;
			}
			
			if(weight>=2) {
				return true;
			} else {
				return false;
			}
		}

		
		
//		System.out.println(weight);
	}

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

	public static void main(String args[]) {
		double gold = 0;
		double sys = 0;
		double hit = 0;
		String folder = "all";
		ArrayList<String> files = Common.getLines("chinese_list_" + folder
				+ "_development");

		for (String file : files) {
			CoNLLDocument doc = new CoNLLDocument(file.replace("auto_conll",
					"gold_conll"));
			OntoCorefXMLReader.addGoldZeroPronouns(doc, true);

			for (CoNLLPart part : doc.getParts()) {

				ArrayList<Mention> systemZeros = getHeuristicZeros(part);

				ArrayList<Mention> goldZeros = EMUtil.getAnaphorZeros(part
						.getChains());

				gold += goldZeros.size();
				sys += systemZeros.size();
				for (Mention s : systemZeros) {
					for (Mention g : goldZeros) {
						if (s.start == g.start) {
							hit++;
						}
					}
				}
			}
		}
		double p = hit / sys;
		double r = hit / gold;
		double f = 2 * p * r / (p + r);
		System.out.println("Hit: " + hit);
		System.out.println("Gold: " + gold);
		System.out.println("Sys: " + sys);
		System.out.println("R: " + r);
		System.out.println("P: " + p);
		System.out.println("F: " + f);
	}

}

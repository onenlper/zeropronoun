package util;

import java.util.ArrayList;

import model.Mention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.syntaxTree.MyTreeNode;

public class TKUtil {

	public static String getMinExpansion(Mention ant, Mention zero,
			CoNLLPart part, boolean isZP) {
		CoNLLSentence s1 = part.getWord(ant.start).sentence;
		CoNLLSentence s2 = part.getWord(zero.start).sentence;
		MyTreeNode r1 = s1.getSyntaxTree().root;

		int mS = part.getWord(ant.start).indexInSentence;
		int mE = part.getWord(ant.end).indexInSentence;

		int zS = part.getWord(zero.start).indexInSentence;

		MyTreeNode bigRoot = null;

		ArrayList<MyTreeNode> mLeaves = new ArrayList<MyTreeNode>();

		MyTreeNode zST = null;

		if (s1 == s2) {
			bigRoot = r1.copy();
			for (int i = mS; i <= mE; i++) {
				mLeaves.add(bigRoot.getLeaves().get(i));
			}
			zST = bigRoot.getLeaves().get(zS);
		} else {
			bigRoot = new MyTreeNode("SS");
			for (int i = s1.getSentenceIdx(); i <= s2.getSentenceIdx(); i++) {
				MyTreeNode root = part.getCoNLLSentences().get(i)
						.getSyntaxTree().root.copy();
				bigRoot.addChild(root);
			}
			for (int i = mS; i <= mE; i++) {
				mLeaves.add(bigRoot.getChild(0).getLeaf(i));
			}
			zST = bigRoot.getChild(bigRoot.children.size() - 1).getLeaf(zS);
		}
		bigRoot.setAllMark(false);
		MyTreeNode lowest = Common.getLowestCommonAncestor(mLeaves.get(0), zST);
		lowest.mark = true;
		// mark shortest path
		for (MyTreeNode leaf : mLeaves) {
			for (int i = leaf.getAncestors().size() - 1; i >= 0; i--) {
				MyTreeNode node = leaf.getAncestors().get(i);
				if (node == lowest) {
					break;
				}
				node.mark = true;
			}
		}
		for (int i = zST.getAncestors().size() - 1; i >= 0; i--) {
			MyTreeNode node = zST.getAncestors().get(i);
			if (node == lowest) {
				break;
			}
			node.mark = true;
		}

		// TODO
		for (MyTreeNode leaf : mLeaves) {
			leaf.mark = false;
		}
		zST.mark = false;
		mLeaves.get(mLeaves.size() - 1).parent.value = "CANDI-"
				+ mLeaves.get(mLeaves.size() - 1).parent.value;
		zST.parent.value = "PRP-" + zST.parent.value;
		return lowest.getTreeBankStyle(true);
	}

	public static String getSimpleExpansion(Mention ant, Mention zero,
			CoNLLPart part, boolean isZP) {
		CoNLLSentence s1 = part.getWord(ant.start).sentence;
		CoNLLSentence s2 = part.getWord(zero.start).sentence;
		MyTreeNode r1 = s1.getSyntaxTree().root;

		int mS = part.getWord(ant.start).indexInSentence;
		int mE = part.getWord(ant.end).indexInSentence;

		int zS = part.getWord(zero.start).indexInSentence;

		MyTreeNode bigRoot = null;

		ArrayList<MyTreeNode> mLeaves = new ArrayList<MyTreeNode>();

		MyTreeNode zST = null;

		if (s1 == s2) {
			bigRoot = r1.copy();
			for (int i = mS; i <= mE; i++) {
				mLeaves.add(bigRoot.getLeaves().get(i));
			}
			zST = bigRoot.getLeaves().get(zS);
		} else {
			bigRoot = new MyTreeNode("SS");
			for (int i = s1.getSentenceIdx(); i <= s2.getSentenceIdx(); i++) {
				MyTreeNode root = part.getCoNLLSentences().get(i)
						.getSyntaxTree().root.copy();
				bigRoot.addChild(root);
			}
			for (int i = mS; i <= mE; i++) {
				mLeaves.add(bigRoot.getChild(0).getLeaf(i));
			}
			zST = bigRoot.getChild(bigRoot.children.size() - 1).getLeaf(zS);
		}
		bigRoot.setAllMark(false);
		MyTreeNode lowest = Common.getLowestCommonAncestor(mLeaves.get(0), zST);
		lowest.mark = true;
		// mark shortest path
		for (MyTreeNode leaf : mLeaves) {
			for (int i = leaf.getAncestors().size() - 1; i >= 0; i--) {
				MyTreeNode node = leaf.getAncestors().get(i);
				if (node == lowest) {
					break;
				}
				node.mark = true;
			}
		}
		for (int i = zST.getAncestors().size() - 1; i >= 0; i--) {
			MyTreeNode node = zST.getAncestors().get(i);
			if (node == lowest) {
				break;
			}
			node.mark = true;
		}
		// mark the first-level children of nodes in shortest path
		ArrayList<MyTreeNode> nodesInPath = new ArrayList<MyTreeNode>();
		ArrayList<MyTreeNode> fronties = new ArrayList<MyTreeNode>();
		fronties.add(lowest);
		while (fronties.size() != 0) {
			ArrayList<MyTreeNode> tmp = new ArrayList<MyTreeNode>();
			for (MyTreeNode node : fronties) {
				if (node.mark) {
					nodesInPath.add(node);
				}
				tmp.addAll(node.children);
			}
			fronties = tmp;
		}
		for (MyTreeNode node : nodesInPath) {
			for (MyTreeNode child : node.children) {
				child.mark = true;
			}
		}

		// TODO
		for (MyTreeNode leaf : mLeaves) {
			leaf.mark = false;
		}
		zST.mark = false;
		mLeaves.get(mLeaves.size() - 1).parent.value = "CANDI-"
				+ mLeaves.get(mLeaves.size() - 1).parent.value;
		zST.parent.value = "PRP-" + zST.parent.value;
		return lowest.getTreeBankStyle(false);
	}

	public static String getFullExpansion(Mention ant, Mention zero,
			CoNLLPart part, boolean isZP) {
		System.out.println(ant.extent + "\t" + ant.start + "," + ant.end + "\t"
				+ zero.extent + part.getDocument().getDocumentID());
		System.out.println(ant.getExtent() + "\t" + zero.getExtent());
		CoNLLSentence s1 = part.getWord(ant.start).sentence;
		CoNLLSentence s2 = part.getWord(zero.start).sentence;
		MyTreeNode r1 = s1.getSyntaxTree().root;

		int mS = part.getWord(ant.start).indexInSentence;
		int mE = part.getWord(ant.end).indexInSentence;

		int zS = part.getWord(zero.start).indexInSentence;

		MyTreeNode bigRoot = null;

		ArrayList<MyTreeNode> mLeaves = new ArrayList<MyTreeNode>();

		MyTreeNode zST = null;

		if (s1 == s2) {
			bigRoot = r1.copy();
			for (int i = mS; i <= mE; i++) {
				mLeaves.add(bigRoot.getLeaves().get(i));
			}
			zST = bigRoot.getLeaves().get(zS);
		} else {
			bigRoot = new MyTreeNode("SS");
			for (int i = s1.getSentenceIdx(); i <= s2.getSentenceIdx(); i++) {
				MyTreeNode root = part.getCoNLLSentences().get(i)
						.getSyntaxTree().root.copy();
				bigRoot.addChild(root);
			}
			for (int i = mS; i <= mE; i++) {
				mLeaves.add(bigRoot.getChild(0).getLeaf(i));
			}
			zST = bigRoot.getChild(bigRoot.children.size() - 1).getLeaf(zS);
		}
		bigRoot.setAllMark(false);
		MyTreeNode lowest = Common.getLowestCommonAncestor(mLeaves.get(0), zST);
		lowest.mark = true;

		ArrayList<MyTreeNode> newLeaves = lowest.getLeaves();
		boolean start = false;
		for (int j = 0; j < newLeaves.size(); j++) {
			MyTreeNode leaf = newLeaves.get(j);
			if (leaf == mLeaves.get(0)) {
				start = true;
			}
			if (start) {
				for (int i = leaf.getAncestors().size() - 2; i >= 0; i--) {
					MyTreeNode node = leaf.getAncestors().get(i);
					if (node == lowest) {
						break;
					}
					node.mark = true;
				}
			}
			if (leaf == zST) {
				break;
			}
		}
		mLeaves.get(mLeaves.size() - 1).parent.value = "CANDI-"
				+ mLeaves.get(mLeaves.size() - 1).parent.value;
		zST.parent.value = "PRP-" + zST.parent.value;
		return lowest.getTreeBankStyle(false);
	}

	public static String getDynamicExpansion(Mention ant, Mention zero, CoNLLPart part, boolean isZP) {
		CoNLLSentence s1 = part.getWord(ant.start).sentence;
		CoNLLSentence s2 = part.getWord(zero.start).sentence;
		MyTreeNode r1 = s1.getSyntaxTree().root;

		int mS = part.getWord(ant.start).indexInSentence;
		int mE = part.getWord(ant.end).indexInSentence;

		int zS = part.getWord(zero.start).indexInSentence;

		MyTreeNode bigRoot = null;

		// ArrayList<MyTreeNode> compteLeaf = new ArrayList<MyTreeNode>();

		ArrayList<MyTreeNode> mLeaves = new ArrayList<MyTreeNode>();

		MyTreeNode zST = null;

		if (s1 == s2) {
			bigRoot = r1.copy();
			for (int i = mS; i <= mE; i++) {
				mLeaves.add(bigRoot.getLeaves().get(i));
			}
			zST = bigRoot.getLeaves().get(zS);
		} else {
			bigRoot = new MyTreeNode("SS");
			for (int i = s1.getSentenceIdx(); i <= s2.getSentenceIdx(); i++) {
				MyTreeNode root = part.getCoNLLSentences().get(i)
						.getSyntaxTree().root.copy();
				bigRoot.addChild(root);
			}
			for (int i = mS; i <= mE; i++) {
				mLeaves.add(bigRoot.getChild(0).getLeaf(i));
			}
			zST = bigRoot.getChild(bigRoot.children.size() - 1).getLeaf(zS);
		}
		bigRoot.setAllMark(false);
		MyTreeNode lowest = Common.getLowestCommonAncestor(mLeaves.get(0), zST);
		lowest.mark = true;
		// mark shortest path
		for (MyTreeNode leaf : mLeaves) {
			for (int i = leaf.getAncestors().size() - 1; i >= 0; i--) {
				MyTreeNode node = leaf.getAncestors().get(i);
				if (node == lowest) {
					break;
				}
				node.mark = true;
			}
		}
		for (int i = zST.getAncestors().size() - 1; i >= 0; i--) {
			MyTreeNode node = zST.getAncestors().get(i);
			if (node == lowest) {
				break;
			}
			node.mark = true;
		}

		// TODO
		for (MyTreeNode leaf : mLeaves) {
			leaf.mark = false;
		}
		zST.mark = false;
		mLeaves.get(mLeaves.size() - 1).parent.value = "CANDI-"
				+ mLeaves.get(mLeaves.size() - 1).parent.value;
		zST.parent.value = "PRP-" + zST.parent.value;

		int startLeaf = 0;
		int endLeaf = bigRoot.getLeaves().size() - 1;
		for (int i = 0; i < bigRoot.getLeaves().size(); i++) {
			if (bigRoot.getLeaves().get(i) == mLeaves.get(0)) {
				startLeaf = i;
			} else if (bigRoot.getLeaves().get(i) == zST) {
				endLeaf = i;
			}
		}

		// attach competitors
		for (int i = startLeaf; i <= endLeaf; i++) {
			MyTreeNode leaf = bigRoot.getLeaves().get(i);
			// if under np, mark all np
			for (int j = leaf.getAncestors().size() - 1; j >= 0; j--) {
				MyTreeNode node = leaf.getAncestors().get(j);
				if (node == lowest) {
					break;
				}
				if (node.value.equalsIgnoreCase("np")) {
					node.setAllMark(true);
					// find predicate
					for (MyTreeNode sibling : node.parent.children) {
						if (sibling.value.equalsIgnoreCase("VV")) {
							sibling.setAllMark(true);
						}
					}
					break;
				}
			}
		}

		// attach verb
		for (int i = startLeaf; i <= endLeaf; i++) {
			MyTreeNode leaf = bigRoot.getLeaves().get(i);
			if (leaf.parent.value.startsWith("V")) {
				// if predicate, see if there is subject or object
				for (int j = leaf.getAncestors().size() - 1; j >= 0; j--) {
					MyTreeNode node = leaf.getAncestors().get(j);
					if (node == lowest) {
						break;
					}
					node.mark = true;
				}
			}
		}

		// prune it!!! single in and single out , attach to grand
		ArrayList<MyTreeNode> offsprings = lowest.getDepthFirstOffsprings();
		for (MyTreeNode node : offsprings) {
			// skip pos tag
			if (node.children.size() == 1
					&& node.children.get(0).children.size() == 0) {
				continue;
			}
			if (node.children.size() == 0) {
				continue;
			}
			// remove this
			if (node.parent != null && node.parent.numberMarkChildren() == 1
					&& node.numberMarkChildren() == 1) {
				node.parent.children.clear();
				node.parent.children.addAll(node.children);
			}
		}
		return lowest.getTreeBankStyle(false);
	}

	public static String getTree(Mention ant, Mention zero, CoNLLPart part) {

		CoNLLSentence s1 = part.getWord(ant.start).sentence;
		CoNLLSentence s2 = part.getWord(zero.start).sentence;

		int mS = part.getWord(ant.start).indexInSentence;
		int mE = part.getWord(ant.end).indexInSentence;

		int zS = part.getWord(zero.start).indexInSentence;
		int zE = zS;

		MyTreeNode bigRoot = null;
		MyTreeNode mST = null;
		MyTreeNode mET = null;
		MyTreeNode zST = null;
		MyTreeNode zET = null;
		if (s1 == s2) {
			bigRoot = s1.getSyntaxTree().root.copy();
			mST = bigRoot.getLeaves().get(mS);
			mET = bigRoot.getLeaves().get(mE);

			zST = bigRoot.getLeaves().get(zS);
			zET = bigRoot.getLeaves().get(zE);
		} else {
			bigRoot = new MyTreeNode("SS");
			for (int i = s1.getSentenceIdx(); i <= s2.getSentenceIdx(); i++) {
				MyTreeNode root = part.getCoNLLSentences().get(i)
						.getSyntaxTree().root.copy();
				bigRoot.addChild(root);
			}
			mST = bigRoot.children.get(0).getLeaves().get(mS);
			mET = bigRoot.children.get(0).getLeaves().get(mE);

			zST = bigRoot.children.get(bigRoot.children.size() - 1).getLeaves()
					.get(zS);
			zET = zST;
		}
		bigRoot.setAllMark(false);
		MyTreeNode lowest = Common.getLowestCommonAncestor(mST, zST);
		lowest.mark = true;
		// find VP node
		ArrayList<MyTreeNode> vps = zST.getXAncestors("VP");
		for (MyTreeNode vp : vps) {
			if (vp.getLeaves().get(0) == zST) {
				// attach zp
				int vpChildID = vp.childIndex;
				MyTreeNode zeroNP = new MyTreeNode("NP");
				MyTreeNode zeroNode = new MyTreeNode("XXX");
				zeroNP.addChild(zeroNode);
				vp.parent.addChild(vpChildID, zeroNP);
				break;
			}
		}
		// mark shortest path
		for (int i = mST.getAncestors().size() - 1; i >= 0; i--) {
			MyTreeNode node = mST.getAncestors().get(i);
			if (node == lowest) {
				break;
			}
			node.mark = true;
		}
		for (int i = mET.getAncestors().size() - 1; i >= 0; i--) {
			MyTreeNode node = mET.getAncestors().get(i);
			if (node == lowest) {
				break;
			}
			node.mark = true;
		}
		for (int i = zST.getAncestors().size() - 1; i >= 0; i--) {
			MyTreeNode node = zST.getAncestors().get(i);
			if (node == lowest) {
				break;
			}
			node.mark = true;
		}

		int startLeaf = 0;
		int endLeaf = bigRoot.getLeaves().size() - 1;
		for (int i = 0; i < bigRoot.getLeaves().size(); i++) {
			if (bigRoot.getLeaves().get(i) == mST) {
				startLeaf = i;
			} else if (bigRoot.getLeaves().get(i) == zST) {
				endLeaf = i;
			}
		}

		// attach competitors
		for (int i = startLeaf; i <= endLeaf; i++) {
			MyTreeNode leaf = bigRoot.getLeaves().get(i);
			// if under np, mark all np
			for (int j = leaf.getAncestors().size() - 1; j >= 0; j--) {
				MyTreeNode node = leaf.getAncestors().get(j);
				if (node == lowest) {
					break;
				}
				if (node.value.equalsIgnoreCase("np")) {
					node.setAllMark(true);
					// find predicate
					for (MyTreeNode sibling : node.parent.children) {
						if (sibling.value.equalsIgnoreCase("VV")) {
							sibling.setAllMark(true);
						}
					}
					break;
				}
			}
		}

		// attach verb
		for (int i = startLeaf; i <= endLeaf; i++) {
			MyTreeNode leaf = bigRoot.getLeaves().get(i);
			if (leaf.parent.value.startsWith("V")) {
				// if predicate, see if there is subject or object
				for (int j = leaf.getAncestors().size() - 1; j >= 0; j--) {
					MyTreeNode node = leaf.getAncestors().get(j);
					if (node == lowest) {
						break;
					}
					node.mark = true;
				}
			}
		}

		// prune it!!! single in and single out , attach to grand
		ArrayList<MyTreeNode> offsprings = lowest.getDepthFirstOffsprings();
		for (MyTreeNode node : offsprings) {
			// skip pos tag
			if (node.children.size() == 1
					&& node.children.get(0).children.size() == 0) {
				continue;
			}
			if (node.children.size() == 0) {
				continue;
			}
			// remove this
			if (node.parent != null && node.parent.numberMarkChildren() == 1
					&& node.numberMarkChildren() == 1) {
				node.parent.children.clear();
				node.parent.children.addAll(node.children);
			}
		}
		// mark min-expansion
		return lowest.getTreeBankStyle(true);
	}
}

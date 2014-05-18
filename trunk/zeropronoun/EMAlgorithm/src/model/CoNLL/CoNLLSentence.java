package model.CoNLL;

import java.io.Serializable;
import java.util.ArrayList;

import mentionDetect.ParseTreeMention;
import model.Element;
import model.Mention;
import model.SemanticRole;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import util.Common;

public class CoNLLSentence implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ArrayList<SemanticRole> roles = new ArrayList<SemanticRole>();
	
	public CoNLLPart part;
	
	public ArrayList<Element> namedEntities = new ArrayList<Element>();
	
	public ArrayList<Mention> mentions;
	
	public String getText() {
		StringBuilder sb = new StringBuilder();
		for(CoNLLWord word : this.words) {
			sb.append(word.word).append(" ");
		}
		return sb.toString().trim();
	}
	
	public String getText(int k) {
		StringBuilder sb = new StringBuilder();
		for(int i=k;i<this.words.size();i++) {
			sb.append(this.words.get(k).word).append(" ");
		}
		return sb.toString();
	}
	
	private int startWordIdx;

	private int endWordIdx;

	private String sentence = "";

	private int sentenceIdx;
	
	public int getSentenceIdx() {
		return sentenceIdx;
	}

	public void setSentenceIdx(int sentenceIdx) {
		this.sentenceIdx = sentenceIdx;
	}

	public int getStartWordIdx() {
		return startWordIdx;
	}

	public void setStartWordIdx(int startWordIdx) {
		this.startWordIdx = startWordIdx;
	}

	public MyTree getSyntaxTree() {
		return syntaxTree;
	}

	public void setSyntaxTree(MyTree syntaxTree) {
		this.syntaxTree = syntaxTree;
	}

	public ArrayList<String> getDepends() {
		return depends;
	}

	public void setDepends(ArrayList<String> depends) {
		this.depends = depends;
	}

	public ArrayList<int[]> getPositions() {
		return positions;
	}

	public void setPositions(ArrayList<int[]> positions) {
		this.positions = positions;
	}

	public ArrayList<CoNLLWord> getWords() {
		return words;
	}

	public void setWords(ArrayList<CoNLLWord> words) {
		this.words = words;
	}

	public int getEndWordIdx() {
		return endWordIdx;
	}

	public void setEndWordIdx(int endWordIdx) {
		this.endWordIdx = endWordIdx;
	}

	private String speaker;

	private int wordsCount;

	public int getWordsCount() {
		return wordsCount;
	}

	public void setWordsCount(int wordsCount) {
		this.wordsCount = wordsCount;
	}

	public String getSentence() {
		return sentence;
	}

	public void setSentence(String sentence) {
		this.sentence = sentence;
	}

	public String getSpeaker() {
		return speaker;
	}

	public void setSpeaker(String speaker) {
		this.speaker = speaker;
	}

	public MyTree syntaxTree;

	public ArrayList<String> depends;

	public ArrayList<int[]> positions;

	public ArrayList<CoNLLWord> words;

	public CoNLLSentence() {
		words = new ArrayList<CoNLLWord>();
	}

	public void addWord(CoNLLWord word) {
		this.words.add(word);
		word.setSentence(this);
		word.indexInSentence = this.words.size()-1;
	}

	public CoNLLWord getWord(int index) {
		return this.words.get(index);
	}

	public void addSyntaxTree(String syntaxStr) {
		this.syntaxTree = Common.constructTree(syntaxStr);
	}

	public Mention getSpan(int startInS, int endInS) {
		Mention m = new Mention();
		m.startInS = startInS;
		m.endInS = endInS;
		
		m.start = this.getWord(startInS).index;
		m.end = this.getWord(endInS).index;
		m.s = this;
		
		MyTreeNode lNode = this.getSyntaxTree().leaves.get(startInS);
		MyTreeNode rNode = this.getSyntaxTree().leaves.get(endInS);

		ArrayList<MyTreeNode> lAns = lNode.getAncestors();
		ArrayList<MyTreeNode> rAns = rNode.getAncestors();
		MyTreeNode node = null;
		for (int i = 0; i < lAns.size() && i < rAns.size(); i++) {
			if (lAns.get(i) == rAns.get(i)) {
				node = lAns.get(i);
			} else {
				break;
			}
		}
		
		if(node==null || node.leafIdx<lNode.leafIdx || node.leafIdx>lNode.leafIdx) {
			node = this.syntaxTree.leaves.get(endInS);
		}
		m.NP = node;
		
		m.head = node.getHeadLeaf().value;
		m.headInS = node.getHeadLeaf().leafIdx;
		
		ParseTreeMention.calEnAttribute(m, part);
		
		return m;
	}
}

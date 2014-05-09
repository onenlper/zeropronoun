package util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import model.Mention;
import model.CoNLL.CoNLLSentence;
import model.syntaxTree.MyTreeNode;

public class Util {
	
	public static boolean anaphorExtension = false;

	public static String modifyAlign = "/users/yzcchen/chen3/ijcnlp2013/sentenceAlign/senAlignOut_modify/";
	public static String originalAlign = "/users/yzcchen/chen3/ijcnlp2013/sentenceAlign/senAlignOut/";
	
	public static String tokenAlignBase = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/tokenBase/";
	
	public static String headAlignBaseGold = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/headBase_gold/";
	public static String headAlignBaseSys = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/headBase_sys/";
	
	public static String headBAAlignBaseGold = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/headBase_BA_gold/";
	public static String headBAAlignBaseSys = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/headBase_BA_sys/";
	
	public static String tokenBAAlignBaseGold = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/tokenBase_BA_gold/";
	public static String tokenBAAlignBaseSys = "/users/yzcchen/chen3/ijcnlp2013/wordAlign/tokenBase_BA_sys/";
	
	static String annotationsStr = "annotations/";
	
	public static String getID(String file) {
		int start = file.indexOf("annotations/") + annotationsStr.length();
		int dot = file.lastIndexOf(".");
		String id = file.substring(start, dot);
		return id;
	}
	
	private static String getTrainFile(String ID, String lang, boolean gold) {
		String base = "";
		if(lang.equalsIgnoreCase("chi")) {
			base = trainBase + "chinese/annotations/"; 
		} else if(lang.equalsIgnoreCase("eng")) {
			base = trainBase + "english/annotations/";			
		}
		if(gold) {
			return base + ID + ".v4_gold_conll";
		} else {
			return base + ID + ".v4_auto_conll";
		}
	}
	
	private static String getDevFile(String ID, String lang, boolean gold) {
		String base = "";
		if(lang.equalsIgnoreCase("chi")) {
			base = devBase + "chinese/annotations/"; 
		} else if(lang.equalsIgnoreCase("eng")) {
			base = devBase + "english/annotations/";			
		}
		if(gold) {
			return base + ID + ".v4_gold_conll";
		} else {
			return base + ID + ".v4_auto_conll";
		}
	}
	
	private static String getTestFile(String ID, String lang, boolean gold) {
		String base = "";
		if(lang.equalsIgnoreCase("chi")) {
			base = testBase + "chinese/annotations/"; 
		} else if(lang.equalsIgnoreCase("eng")) {
			base = testBase + "english/annotations/";			
		}
		if(gold) {
			return base + ID + ".v4_gold_conll";
		} else {
			if(lang.equalsIgnoreCase("chi")) {
				return base + ID + ".v5_auto_conll";
			} else {
				return base + ID + ".v4_auto_conll";
			}
		}
	}
		
	static String devBase = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/development/data/";
	static String trainBase = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/train/data/";
	static String testBase = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/test/data/";
	
	public static String getFullPath(String ID, String lang, boolean gold) {
		String trainFn = getTrainFile(ID, lang, gold);
//		System.out.println(trainFn);
		if((new File(trainFn)).exists()) {
			return trainFn;
		}
		String devFn = getDevFile(ID, lang, gold);
//		System.out.println(devFn);
		if((new File(devFn)).exists()) {
			return devFn;
		}
		String testFn = getTestFile(ID, lang, gold);
//		System.out.println(testFn);
		if((new File(testFn)).exists()) {
			return testFn;
		}
		return null;
	}
	
	public static String getMTPath(int ID, String lang) {
		String base = "/users/yzcchen/chen3/ijcnlp2013/googleMTRED/" + lang + "_MT/conll/";
		return base + ID + ".conll";
	}
	
	public static Mention formPhrase(MyTreeNode treeNode, CoNLLSentence sentence) {
		ArrayList<MyTreeNode> leaves = treeNode.getLeaves();
		int startIdx = leaves.get(0).leafIdx;
		int endIdx = leaves.get(leaves.size() - 1).leafIdx;
		int start = sentence.getWord(startIdx).index;
		int end = sentence.getWord(endIdx).index;
		StringBuilder sb = new StringBuilder();
		for (int i = startIdx; i <= endIdx; i++) {
			sb.append(sentence.getWord(i).word).append(" ");
		}
		Mention em = new Mention();
		em.start = start;
		em.end = end;
		em.extent = sb.toString().trim();
		return em;
	}
	
	public static Set<String> reportVerb = new HashSet<String>(Arrays.asList("accuse", "acknowledge", "add", "admit",
			"advise", "agree", "alert", "allege", "announce", "answer", "apologize", "argue", "ask", "assert",
			"assure", "beg", "blame", "boast", "caution", "charge", "cite", "claim", "clarify", "command", "comment",
			"compare", "complain", "concede", "conclude", "confirm", "confront", "congratulate", "contend",
			"contradict", "convey", "counter", "criticize", "debate", "decide", "declare", "defend", "demand",
			"demonstrate", "deny", "describe", "determine", "disagree", "disclose", "discount", "discover", "discuss",
			"dismiss", "dispute", "disregard", "doubt", "emphasize", "encourage", "endorse", "equate", "estimate",
			"expect", "explain", "express", "extoll", "fear", "feel", "find", "forbid", "forecast", "foretell",
			"forget", "gather", "guarantee", "guess", "hear", "hint", "hope", "illustrate", "imagine", "imply",
			"indicate", "inform", "insert", "insist", "instruct", "interpret", "interview", "invite", "issue",
			"justify", "learn", "maintain", "mean", "mention", "negotiate", "note", "observe", "offer", "oppose",
			"order", "persuade", "pledge", "point", "point out", "praise", "pray", "predict", "prefer", "present",
			"promise", "prompt", "propose", "protest", "prove", "provoke", "question", "quote", "raise", "rally",
			"read", "reaffirm", "realise", "realize", "rebut", "recall", "reckon", "recommend", "refer", "reflect",
			"refuse", "refute", "reiterate", "reject", "relate", "remark", "remember", "remind", "repeat", "reply",
			"report", "request", "respond", "restate", "reveal", "rule", "say", "see", "show", "signal", "sing",
			"slam", "speculate", "spoke", "spread", "state", "stipulate", "stress", "suggest", "support", "suppose",
			"surmise", "suspect", "swear", "teach", "tell", "testify", "think", "threaten", "told", "uncover",
			"underline", "underscore", "urge", "voice", "vow", "warn", "welcome", "wish", "wonder", "worry", "write", 
			"表示", "讲起", "说话", "说", "指出", "介绍", "认为", "密布", "觉得", "汇给", "低吟", "想", "介绍", "以为", "惊叫",
			"回忆", "宣告", "报道", "透露", "谈", "感慨", "反映", "宣布", "指"));
}

package em;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import util.Common;
import util.Common.Feature;
import util.YYFeature;

public class GuessPronounFea extends YYFeature {
	// global gap
	int next;
	int previous;

	CoNLLSentence s;
	CoNLLPart part;

	HashMap<String, Integer> trans;
	HashMap<String, Integer> intrans;

	public GuessPronounFea(boolean train, String name) {
		super(train, name);
		this.trans = Common.readFile2Map("trans.verb");
		this.intrans = Common.readFile2Map("intrans.verb");

	}

	public int get(HashMap<String, Integer> map, String key) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		return 0;
	}

	public void configure(int previous, int next, CoNLLSentence s, CoNLLPart part) {
		this.next = next;
		this.previous = previous;
		this.s = s;
		this.part = part;
	}

	@Override
	public ArrayList<Feature> getCategoryFeatures() {
		ArrayList<Feature> features = new ArrayList<Feature>();
		return features;
	}

	@Override
	public ArrayList<HashSet<String>> getStrFeatures() {
		ArrayList<HashSet<String>> strs = new ArrayList<HashSet<String>>();
		strs.addAll(getXueLexicalFea());
		strs.addAll(this.getLexicalFea());
		return strs;
	}
	
	public ArrayList<HashSet<String>> getLexicalFea() {
		ArrayList<HashSet<String>> feas = new ArrayList<HashSet<String>>();
		
		HashSet<String> before = new HashSet<String>();
		for(int i=s.getStartWordIdx();i<=this.previous;i++) {
			before.add(this.part.getWord(i).word);
		}
		feas.add(before);
		
		HashSet<String> beforeBigram = new HashSet<String>();
		for(int i=s.getStartWordIdx();i<this.previous;i++) {
			before.add(this.part.getWord(i).word + "#" + this.part.getWord(i+1).word);
		}
		feas.add(beforeBigram);
		
		HashSet<String> after = new HashSet<String>();
		for(int i=this.next;i<=this.s.getEndWordIdx();i++) {
			after.add(this.part.getWord(i).word);
		}
		feas.add(after);
		HashSet<String> afterBigram = new HashSet<String>();
		for(int i=this.next;i<this.s.getEndWordIdx();i++) {
			afterBigram.add(this.part.getWord(i).word + "#" + this.part.getWord(i).word);
		}
		feas.add(afterBigram);
		
		
		return feas;
	}
	

	private ArrayList<HashSet<String>> getXueLexicalFea() {
		ArrayList<String> strs = new ArrayList<String>();
		// word(1)
		strs.add(part.getWord(this.next).word);
		// word(-1)
		if (this.previous >= 0) {
			strs.add(part.getWord(this.previous).word);
		} else {
			strs.add("-");
		}

		ArrayList<HashSet<String>> ret = new ArrayList<HashSet<String>>();
		for (String str : strs) {
			HashSet<String> set = new HashSet<String>();
			set.add(str);
			ret.add(set);
		}

		return ret;
	}
}

package em;

import model.Mention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;

public class RuleUtil {

	
	public static boolean zeroStartsSentence(Mention zero, CoNLLPart part) {
		CoNLLWord w = part.getWord(zero.getStart());
		if(w.indexInSentence==0) {
			return true;
		}
		return false;
	}
	
	public static boolean zeroAfterComma(Mention zero, CoNLLPart part) {
		CoNLLWord w = part.getWord(zero.getStart());
		CoNLLSentence s = w.sentence;
		
		if(w.indexInSentence!=0) {
			CoNLLWord pw = s.getWords().get(w.indexInSentence-1);
			String pwStr = pw.word;
			if(pwStr.equals(",") || pwStr.equals("，")) {
				return true;
			}
			if(w.indexInSentence>1) {
				CoNLLWord pw2 = s.getWords().get(w.indexInSentence-2);
				String pw2Str = pw2.word;
				if(pw.posTag.equals("AD") && (pw2Str.equals(",") || pw2Str.equals("，"))) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static boolean zeroAfterVerb(Mention zero, CoNLLPart part) {
		CoNLLWord w = part.getWord(zero.getStart());
		CoNLLSentence s = w.sentence;
		
		if(w.indexInSentence!=0) {
			String pw = s.getWords().get(w.indexInSentence-1).posTag;
			if(pw.startsWith("V")) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean zeroAfterNoun(Mention zero, CoNLLPart part) {
		CoNLLWord w = part.getWord(zero.getStart());
		CoNLLSentence s = w.sentence;
		
		if(w.indexInSentence!=0) {
			String pw = s.getWords().get(w.indexInSentence-1).posTag;
			if(pw.startsWith("N") || pw.equals("PN")) {
				return true;
			}
		}
		return false;
	}

	public static boolean zeroAfterCC(Mention zero, CoNLLPart part) {
		CoNLLWord w = part.getWord(zero.getStart());
		CoNLLSentence s = w.sentence;
		
		if(w.indexInSentence!=0) {
			String pw = s.getWords().get(w.indexInSentence-1).posTag;
			if(pw.startsWith("CC")) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean zeroAfterNN_P(Mention zero, CoNLLPart part) {
		CoNLLWord w = part.getWord(zero.getStart());
		CoNLLSentence s = w.sentence;
		
		if(w.indexInSentence!=0) {
			CoNLLWord pw = s.getWords().get(w.indexInSentence-1);
			if(pw.posTag.equals("P")) {
				if(w.indexInSentence>1) {
					CoNLLWord pw2 = s.getWords().get(w.indexInSentence-2);
					if(pw2.posTag.startsWith("N") || pw2.posTag.equals("PN")) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static boolean zeroOnlyAfterAD_P_CS(Mention zero, CoNLLPart part) {
		CoNLLWord w = part.getWord(zero.getStart());
		CoNLLSentence s = w.sentence;
		if(w.indexInSentence==1) {
			String pw = s.getWords().get(w.indexInSentence-1).posTag;
			if(pw.equals("AD") || pw.equals("P") || pw.equals("CS")) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean zeroAfterQuota(Mention zero, CoNLLPart part) {
		CoNLLWord w = part.getWord(zero.getStart());
		CoNLLSentence s = w.sentence;
		if(w.indexInSentence==1) {
			String pw = s.getWords().get(w.indexInSentence-1).word;
			if(pw.equals("“") || pw.equals("「")) {
				return true;
			}
		}
		return false;
	}
	
	public static void printZero(Mention zero, CoNLLPart part) {
		StringBuilder sb = new StringBuilder();
		CoNLLWord w = part.getWord(zero.getStart());
		CoNLLSentence s = w.sentence;
		int idInS = w.indexInSentence;
		for(int i=-10;i<=10;i++) {
			int id = idInS + i;
			if(id>=0 && id<s.words.size()) {
				if(i==0) {
					sb.append("*pro*");
				}
				sb.append(s.getWords().get(id).word);
			}
		}
		System.out.println(sb.toString());
		String pw = s.getWords().get(w.indexInSentence-1).posTag;
		System.out.println(pw);
	}
}

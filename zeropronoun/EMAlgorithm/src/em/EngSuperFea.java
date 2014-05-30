package em;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import model.Mention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.Common.Feature;
import util.YYFeature;
import em.EMUtil.Grammatic;
import em.EMUtil.Person;

public class EngSuperFea extends YYFeature {

	Mention p;
	CoNLLPart part;
	Mention c;

	int disSent;
	int disWord;

	public EngSuperFea(boolean train, String name) {
		super(train, name);
	}

	public void configure(Mention c, Mention p, CoNLLPart part, int disSent,
			int disWord) {
		this.c = c;
		this.p = p;
		this.part = part;
		this.disSent = disSent;
		this.disWord = (int) Math.pow(disWord, .5);
	}

	@Override
	public ArrayList<Feature> getCategoryFeatures() {
		ArrayList<Feature> features = new ArrayList<Feature>();
		return features;
		
//		CoNLLSentence ps = p.s;
//		CoNLLSentence cs = p.s;
//
//		String pSpeaker = part.getWord(p.getStart()).speaker;
//		String cSpeaker = part.getWord(c.getStart()).speaker;
//
//		int distanceS = cs.getSentenceIdx() - ps.getSentenceIdx();
//		if (distanceS > 9) {
//			distanceS = 9;
//		}
//		features.add(new Feature(distanceS, 1, 10));
//
//		int distanceW = (int) Math.pow(c.start
//				- (p.end == -1 ? p.start : p.end), 0.5);
//		if (distanceW > 9) {
//			distanceW = 9;
//		}
//		features.add(new Feature(distanceW, 1, 10));
//
//		if (ps.getSentenceIdx() == cs.getSentenceIdx()) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//
//		if (p.gender == c.gender) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//
//		if (p.number == c.number) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//
//		if (p.animacy == c.animacy) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//
//		if ((p.person == c.person && pSpeaker.equals(cSpeaker))
//				|| !pSpeaker.equals(cSpeaker)
//				&& p.number == c.number
//				&& ((p.person == Person.first && c.person == Person.second) || (p.person == Person.second && c.person == Person.first))) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//
//		if (!c.NE.equals("OTHER")) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//
//		if (c.extent.toLowerCase().startsWith("the ")) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//
//		if (c.gram == Grammatic.subject) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//
//		if (c.gram == Grammatic.object) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//
//		if (c.isNNP) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
//
//		if (p.generic) {
//			features.add(new Feature(0, 1, 2));
//		} else {
//			features.add(new Feature(1, 1, 2));
//		}
	}

	private boolean isPronoun(Mention m) {
		if (m.start == m.end
				&& this.part.getWord(m.start).posTag.startsWith("PRP")) {
			return true;
		} else {
			return false;
		}
	}

	private String getPronounForm(Mention m) {
		if (isPronoun(m)) {
			return this.part.getWord(m.start).word.toLowerCase();
		} else {
			return "**";
		}
	}

	public String getSyntaxPath() {
		if (p.s != c.s) {
			return "diffSentence";
		}
		MyTreeNode n1 = c.NP;
		MyTreeNode n2 = p.NP;
		ArrayList<MyTreeNode> n1s = n1.getAncestors();
		ArrayList<MyTreeNode> n2s = n2.getAncestors();
		MyTreeNode common = new MyTreeNode();
		int i = 0;
		for (; i < n1s.size() && i < n2s.size(); i++) {
			if (n1s.get(i) == n2s.get(i)) {
				common = n1s.get(i);
			} else {
				break;
			}
		}
		StringBuilder sb = new StringBuilder();
		for(int j=n1s.size()-1;j>=0;j--) {
			MyTreeNode n = n1s.get(j);
			if(n==common) {
				break;
			}
			sb.append(n.value).append("#");
		}
		sb.append(common.value).append("#");
		
		for(int j=i+1;j<n2s.size();j++) {
			MyTreeNode n = n2s.get(j);
			sb.append(n.value).append("#");
		}
		return sb.toString();
	}
	
	private String getSubCate(MyTreeNode np) {
		StringBuilder sb = new StringBuilder();
		sb.append(np.value + "->");
		for(MyTreeNode child : np.children) {
			if(child.children.size()==0) {
				continue;
			} else {
				sb.append(child.value).append(",");
			}
		}
		return sb.toString();
	}

	private String getSPr(Mention m) {
		CoNLLWord w = part.getWord(m.start);
		CoNLLSentence s = w.sentence;
		if(w.indexInSentence==0) {
			return "##";
		} else {
			return s.getWords().get(w.indexInSentence-1).word.toLowerCase();
		}
	}
	
	private String getSFo(Mention m) {
		CoNLLWord w = part.getWord(m.start);
		CoNLLSentence s = w.sentence;
		if(w.indexInSentence==s.getWords().size()-1) {
			return "##";
		} else {
			return s.getWords().get(w.indexInSentence+1).word.toLowerCase();
		}
	}
	
	private String getSFoPos(Mention m) {
		CoNLLWord w = part.getWord(m.start);
		CoNLLSentence s = w.sentence;
		if(w.indexInSentence==s.getWords().size()-1) {
			return "##";
		} else {
			return s.getWords().get(w.indexInSentence+1).posTag;
		}
	}
	
	private String getSFPos(Mention m) {
		if(m.headID==m.start) {
			return "";
		} else {
			return part.getWord(m.start).posTag;
		}
	}
	
	private String getSFForm(Mention m) {
		if(m.headID==m.start) {
			return "";
		} else {
			return part.getWord(m.start).word.toLowerCase();
		}
	}
	
	private String getHDInForm(Mention m) {
		if(m.headID+1<=m.end) {
			return this.part.getWord(m.headID+1).word.toLowerCase();
		} else {
			return "**";
		}
	}
	
	@Override
	public ArrayList<HashSet<String>> getStrFeatures() {
		ArrayList<String> feas = new ArrayList<String>();

		CoNLLSentence ps = p.s;
		CoNLLSentence cs = c.s;
		String antHead = part.getWord(this.c.headID).word.toLowerCase();
		String anaHead = part.getWord(this.p.headID).word.toLowerCase();
		String antExtent = c.extent.toLowerCase();
		String anaExtent = p.extent.toLowerCase();
		String pSpeaker = part.getWord(p.getStart()).speaker;
		String cSpeaker = part.getWord(c.getStart()).speaker;
		boolean sameSpeaker = pSpeaker.equalsIgnoreCase(cSpeaker);

		feas.add(antHead + "##" + anaHead);

		feas.add(Boolean.toString(antExtent.startsWith("the ")));
		feas.add(Boolean.toString(anaExtent.startsWith("a ")
				|| anaExtent.startsWith("an ")));

		feas.add(Integer.toString(this.disSent));
		feas.add(Integer.toString(this.disSent));

		feas.add(sameSpeaker + "#" + this.getPronounForm(c) + "#" + anaHead);
		feas.add(antExtent);
		feas.add(getSyntaxPath() + anaHead);
		feas.add(this.c.NP.value);
		feas.add(this.getSubCate(c.NP));
		feas.add(this.part.folder);
		feas.add(this.getSPr(c));
		feas.add(antExtent + "##" + anaExtent);
		feas.add(this.getSFo(c));
		feas.add(this.getSFForm(c));
		feas.add(part.getWord(c.headID).posTag + "#" + anaHead);
		feas.add(part.getWord(c.headID).word + "#" + anaHead);
		feas.add(this.getSubCate(this.c.NP.parent));
		feas.add(this.getSPr(c) + "#" + anaHead);
		feas.add(this.getSubCate(this.c.NP.parent) + "#" + anaHead);
		feas.add(this.c.mentionType.name());
		feas.add(this.disWord + "#" + anaHead);
		feas.add(this.disSent + "#" + anaHead);
		feas.add(this.part.folder + "#" + this.getPronounForm(p) + "#" + anaHead);
		feas.add(this.getHDInForm(c) + "#" + this.part.getWord(c.headID).posTag);
		feas.add(this.getSFoPos(c) + "#" + anaHead);
		feas.add(c.gender.name() + "#" + anaHead);
		feas.add(c.number.name() + "#" + anaHead);
		feas.add(c.animacy.name() + "#" + anaHead);
		feas.add(this.c.NE);

		// String pred1N = EMUtil.getPredicateNode( );

		ArrayList<HashSet<String>> ret = new ArrayList<HashSet<String>>();
		for(String f : feas) {
			HashSet<String> set = new HashSet<String>();
			set.add(f);
			ret.add(set);
		}

		return ret;
	}

}

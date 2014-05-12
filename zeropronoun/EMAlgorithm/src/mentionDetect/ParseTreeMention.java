package mentionDetect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import align.DocumentMap.Unit;

import model.Element;
import model.Mention;
import model.CoNLL.CoNLLDocument.DocType;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import util.Common;
import dict.EnDictionary;
import em.EMUtil.Animacy;
import em.EMUtil.Gender;
import em.EMUtil.MentionType;
import em.EMUtil.Number;
import em.EMUtil.Person;
import em.EMUtil.PersonEng;

public class ParseTreeMention extends MentionDetect {

	@Override
	public ArrayList<Mention> getMentions(CoNLLPart part) {
		if (part.getDocument().getLanguage().equalsIgnoreCase("chinese")) {
			ChineseMention ch = new ChineseMention();
			return ch.getChineseMention(part);
		} else if (part.getDocument().getLanguage().equalsIgnoreCase("english")) {
			return getEnglishMention(part);
		} 
		return null;
	}

	private ArrayList<Mention> getEnglishMention(CoNLLPart part) {
		ArrayList<Mention> mentions = new ArrayList<Mention>();
		mentions.addAll(this.getNamedMention(part));
		mentions.addAll(this.getNPorPRPMention(part));
		removeDuplicateMentions(mentions);
		this.pruneMentions(mentions, part);
		this.setBarePlural(mentions, part);
		
		for(Mention m : mentions) {
			CoNLLSentence s = m.s;
			if (s.part.itself != null) {
				for (int i = m.start; i <= m.end; i++) {
					Unit unit = s.part.itself.getUnit(i);
					if (unit != null) {
						unit.addMention(m);
//						System.out.println("add");
					}
				}
			}
		}
		
		return mentions;
	}
	
	private ArrayList<Mention> getNamedMention(CoNLLPart part) {
		ArrayList<Mention> mentions = new ArrayList<Mention>();
		ArrayList<Element> namedEntities = part.getNameEntities();
		for (Element element : namedEntities) {
			if (element.content.equalsIgnoreCase("QUANTITY") || element.content.equalsIgnoreCase("CARDINAL")
					|| element.content.equalsIgnoreCase("PERCENT")) {
				continue;
			}
			// Mr. Mandela
			if (element.start > 0
					&& EnDictionary.getInstance().titles.contains(part.getWord(element.start - 1).word)) {
				continue;
			}
			int end = element.end;
			int start = element.start;
			if (element.end + 1 < part.getWordCount()) {
				String lastWord = part.getWord(element.end + 1).word;
				if (lastWord.equalsIgnoreCase("'s")) {
					end++;
				}
			}
			Mention mention = new Mention(start, end);
			
			StringBuilder sb = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (int i = start; i <= end; i++) {
				sb.append(part.getWord(i).word).append(" ");
				sb2.append(part.getWord(i).orig).append(" ");
			}
			mention.extent = sb.toString().trim().toLowerCase();
			
			mention.s = part.getWord(start).sentence;

			MyTreeNode t1 = mention.s.syntaxTree.leaves.get(part.getWord(start).indexInSentence);
			MyTreeNode t2 = mention.s.syntaxTree.leaves.get(part.getWord(end).indexInSentence);
			ArrayList<MyTreeNode> anc1 = t1.getAncestors();
			ArrayList<MyTreeNode> anc2 = t2.getAncestors();
			MyTreeNode node = null;
			for(int i=0;i<anc1.size()&&i<anc2.size();i++) {
				if(anc1.get(i)==anc2.get(i)) {
					node = anc1.get(i);
				} else {
					break;
				}
			}
			mention.NP = node;
			if (!mentions.contains(mention)) {
				mentions.add(mention);
			}
		}
		return mentions;
	}
	
	public Mention formPhrase(MyTreeNode treeNode, CoNLLSentence sentence) {
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
		em.NP = treeNode;
		return em;
	}
	
	public ArrayList<Mention> getAllNounPhrase(CoNLLPart part) {
		ArrayList<Mention> nounPhrases = new ArrayList<Mention>();
		for (CoNLLSentence sentence : part.getCoNLLSentences()) {
			MyTree tree = sentence.getSyntaxTree();
			MyTreeNode root = tree.root;
			ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
			frontie.add(root);
			while (frontie.size() > 0) {
				MyTreeNode tn = frontie.remove(0);
				String value = tn.value.toUpperCase();
				if ((part.lang.equalsIgnoreCase("chi") && (tn.value.toUpperCase().startsWith("NP") || tn.value.toUpperCase().startsWith("QP")))
						|| (part.lang.equalsIgnoreCase("eng") && (value.startsWith("PRP") || value
								.startsWith("NP")))) {
//					if (!value.equalsIgnoreCase("NP") && !value.equalsIgnoreCase("PRP")
//							&& !value.equalsIgnoreCase("PRP$")) {
//						System.out.println(value);
//					}
					Mention element = formPhrase(tn, sentence);
					if (element != null) {
						if (element.start == -1) {
//							System.out.println();
						}
						nounPhrases.add(element);
					}
				}
				ArrayList<MyTreeNode> tns = tn.children;
				frontie.addAll(tns);
			}

		}
		return nounPhrases;
	}
	
	private ArrayList<Mention> getNPorPRPMention(CoNLLPart part) {
		ArrayList<Mention> npMentions = new ArrayList<Mention>();
		npMentions = getAllNounPhrase(part);

		// MentionDetect md = new GoldBoundaryMentionTest();
		// npMentions = md.getMentions(part);
		for (int g = 0; g < npMentions.size(); g++) {
			Mention npMention = npMentions.get(g);
			int end = npMention.end;
			int start = npMention.start;
			StringBuilder sb = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (int i = start; i <= end; i++) {
				sb.append(part.getWord(i).word).append(" ");
				sb2.append(part.getWord(i).orig).append(" ");
			}
			npMention.extent = sb2.toString().trim();
			npMention.s = part.getWord(start).sentence;
			// System.out.println(start + " " + end + " " + npMention.source);

			// for (Element NE : part.getNameEntities()) {
			// if (NE.content.equalsIgnoreCase("QUANTITY") ||
			// NE.content.equalsIgnoreCase("QUANTITY")
			// || NE.content.equalsIgnoreCase("PERCENT")) {
			// continue;
			// }
			// if (npMention.start >= NE.start && npMention.end <= NE.end) {
			// npMentions.remove(g);
			// g--;
			// break;
			// }
			// }
		}
		return npMentions;
	}
	
	private void assignNE(ArrayList<Mention> mentions, CoNLLPart part) {
		for (Mention mention : mentions) {
			int headStart = mention.headID;
			for (Element element : part.getNameEntities()) {
				if (element.start <= headStart && headStart <= element.end) {
					mention.NE = element.content;
				}
			}
		}
	}
	
//	public boolean isEnglishAppositive2(Mention ant, Mention mention, CoNLLPart part) {
//		if (ant.s != mention.s) {
//			return false;
//		}
//		Tree tree = ant.s.stdTree;
//		String pattens[] = new String[4];
//		pattens[0] = "NP=m1 < (NP=m2 $.. (/,/ $.. NP=m3))";
//		pattens[1] = "NP=m1 < (NP=m2 $.. (/,/ $.. (SBAR < (WHNP < WP|WDT=m3))))";
//		pattens[2] = "/^NP(?:-TMP|-ADV)?$/=m1 < (NP=m2 $- /^,$/ $-- NP=m3 !$ CC|CONJP)";
//		pattens[3] = "/^NP(?:-TMP|-ADV)?$/=m2 < (PRN=m3 < (NP < /^NNS?|CD$/ $-- /^-LRB-$/ $+ /^-RRB-$/))";
//		for (String pattern : pattens) {
//			try {
//				TregexPattern tgrepPattern = TregexPattern.compile(pattern);
//				TregexMatcher m = tgrepPattern.matcher(tree);
//				while (m.find()) {
//					Tree np2 = m.getNode("m2");
//					Tree np3 = m.getNode("m3");
//					int start1 = (int) ((CoreLabel) np2.label()).get(BeginIndexAnnotation.class);
//					int end1 = (int) ((CoreLabel) np2.label()).get(EndIndexAnnotation.class) - 1;
//					int start2 = (int) ((CoreLabel) np3.label()).get(BeginIndexAnnotation.class);
//					int end2 = (int) ((CoreLabel) np3.label()).get(EndIndexAnnotation.class) - 1;
//					if (start1 == ant.start && end1 == ant.end && start2 == mention.start && end2 == mention.end) {
////						System.out.println(ant.original + " # " + mention.original);
//						return true;
//					}
//				}
//			} catch (Exception e) {
//				e.printStackTrace();
//				System.exit(0);
//			}
//		}
//		return false;
//	}
	
//	private static boolean isPleonastic(Mention m, Tree tree) {
//		if (!m.extent.equalsIgnoreCase("it"))
//			return false;
//		final String[] patterns = {
//				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (VP < (VBN $.. /S|SBAR/))))",
//				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (ADJP $.. (/S|SBAR/))))",
//				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (ADJP < (/S|SBAR/))))",
//				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (NP < /S|SBAR/)))",
//				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:is|was|become|became)/) $.. (NP $.. ADVP $.. /S|SBAR/)))",
//				"NP < (PRP=m1) $.. (VP < (MD $ .. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (VP < (VBN $.. /S|SBAR/))))))",
//				"NP < (PRP=m1) $.. (VP < (MD $ .. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (ADJP $.. (/S|SBAR/))))))",
//				"NP < (PRP=m1) $.. (VP < (MD $ .. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (ADJP < (/S|SBAR/))))))",
//				"NP < (PRP=m1) $.. (VP < (MD $ .. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (NP < /S|SBAR/)))))",
//				"NP < (PRP=m1) $.. (VP < (MD $ .. (VP < ((/^V.*/ < /^(?:be|become)/) $.. (NP $.. ADVP $.. /S|SBAR/)))))",
//				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:seems|appears|means|follows)/) $.. /S|SBAR/))",
//				"NP < (PRP=m1) $.. (VP < ((/^V.*/ < /^(?:turns|turned)/) $.. PRT $.. /S|SBAR/))" };
//		for (String p : patterns) {
//			if (checkPleonastic(m, tree, p)) {
//				return true;
//			}
//		}
//		return false;
//	}
	
	private void pruneMentions(ArrayList<Mention> mentions, CoNLLPart part) {
		assignNE(mentions, part);
		for (Mention mention : mentions) {
			calEnAttribute(mention, part);
		}
		ArrayList<Mention> removes = new ArrayList<Mention>();

		Collections.sort(mentions);
//		for (int i = 0; i < mentions.size(); i++) {
//			for (int j = 0; j < i; j++) {
//				if (isEnglishAppositive2(mentions.get(j), mentions.get(i), part)) {
//					removes.add(mentions.get(i));
//					removes.add(mentions.get(j));
//				}
//			}
//		}
		// remove smaller mention with the same head as the longer mention
		ArrayList<Mention> copyMentions = new ArrayList<Mention>(mentions.size());
		copyMentions.addAll(mentions);
		for (int i = 0; i < mentions.size(); i++) {
			Mention em = mentions.get(i);
			for (int j = 0; j < copyMentions.size(); j++) {
				Mention em2 = copyMentions.get(j);
				if (em.headID == em2.headID
						&& (em.end - em.start < em2.end - em2.start)) {
					if (em.end + 1 < part.getWordCount()
							&& (part.getWord(em.end + 1).posTag.equalsIgnoreCase("CC") || part.getWord(em.end + 1).word
									.equals(","))) {
						continue;
					}
					removes.add(em);
					break;
				}
			}
		}

		mentions.removeAll(removes);
		removes.clear();

		for (int i = 0; i < mentions.size(); i++) {
			Mention mention = mentions.get(i);
//			if (isPleonastic(mention, mention.s.syntaxTree)) {
//				removes.add(mention);
//				continue;
//			}

			if (mention.head.equalsIgnoreCase("%")) {
				removes.add(mention);
				continue;
			}
			if (mention.NE.equalsIgnoreCase("QUANTITY") || mention.NE.equalsIgnoreCase("CARDINAL")
					|| mention.NE.equalsIgnoreCase("PERCENT") || mention.NE.equalsIgnoreCase("MONEY")) {
				removes.add(mention);
				continue;
			}
			// non word such as 'hmm'
			if (EnDictionary.getInstance().nonWords.contains(mention.head.toLowerCase())) {
				removes.add(mention);
				continue;
			}
			// quantRule : not starts with 'any', 'all' etc
			if (EnDictionary.getInstance().quantifiers.contains(part.getWord(mention.start).word)) {
				removes.add(mention);
				continue;
			}
			// partitiveRule
			if (mention.start > 2 && part.getWord(mention.start - 1).word.equals("of")
					&& EnDictionary.getInstance().parts.contains(part.getWord(mention.start - 2).word)) {
				removes.add(mention);
				continue;
			}
			// bareNPRule
			if (part.getWord(mention.headID).posTag.equals("NN")
					&& !EnDictionary.getInstance().temporals.contains(mention.head)
					&& (mention.end - mention.start == 0 || part.getWord(mention.start).posTag.equalsIgnoreCase("JJ"))) {
				removes.add(mention);
				continue;
			}
			// adjective form of nations
			if (EnDictionary.getInstance().adjectiveNation.contains(mention.extent.toLowerCase())) {
				removes.add(mention);
				continue;
			}
			// stop list (e.g., U.S., there)
			if (inStopList(mention)) {
				removes.add(mention);
				continue;
			}
		}
		mentions.removeAll(removes);
		HashSet<Mention> mentionsHash = new HashSet<Mention>();
		mentionsHash.addAll(mentions);
		mentions.clear();
		mentions.addAll(mentionsHash);
	}
	
	private void removeDuplicateMentions(ArrayList<Mention> mentions) {
		HashSet<Mention> mentionsHash = new HashSet<Mention>();
		mentionsHash.addAll(mentions);
		mentions.clear();
		mentions.addAll(mentionsHash);
	}
	
	private void setBarePlural(List<Mention> mentions, CoNLLPart part) {
		Collections.sort(mentions);
		for (Mention m : mentions) {
			String pos = part.getWord(m.start).posTag;
			if (m.start == m.end && pos.equals("NNS")) {
				m.generic = true;
			}
			// set generic 'you' : e.g., you know in conversation
			if (part.getDocument().getType() != DocType.Article && m.person == Person.second
					&& m.end + 1 < part.getWordCount() && part.getWord(m.end + 1).orig.equals("know")) {
				m.generic = true;
			}
		}
	}
	
	private static boolean inStopList(Mention m) {
		String mentionSpan = m.extent.toLowerCase();
		if (mentionSpan.equals("u.s.") || mentionSpan.equals("u.k.") || mentionSpan.equals("u.s.s.r"))
			return true;
		if (mentionSpan.equals("there") || mentionSpan.startsWith("etc.") || mentionSpan.equals("ltd."))
			return true;
		if (mentionSpan.startsWith("'s "))
			return true;
		if (mentionSpan.endsWith("etc."))
			return true;
		return false;
	}
	
	public void assignHeadExtent(Mention em, CoNLLPart part) {
		ArrayList<CoNLLSentence> sentences = part.getCoNLLSentences();
		if (part.lang.equalsIgnoreCase("chi")) {
			em.headID = em.end;
			em.head = part.getWord(em.headID).orig;
		} else if (part.lang.equalsIgnoreCase("eng")) {
			MyTreeNode node = em.NP;
			// find English mention's head
			// mention ends with 's
			MyTreeNode headLeaf = node.getHeadLeaf();
			int headStart = em.s.getWord(headLeaf.leafIdx).index;
			if (headStart < em.start || headStart > em.end) {
				headStart = em.end;
			}
			String head = part.getWord(headStart).orig;
			em.headID = headStart;
			em.head = head;
		}
	}
	
	public void setMentionType(Mention mention, CoNLLPart part) {
		if (part.getWord(mention.headID).posTag.startsWith("PRP")
				|| (mention.start == mention.end && mention.NE.equalsIgnoreCase("OTHER") && (EnDictionary.getInstance().allPronouns
						.contains(mention.head) || EnDictionary.getInstance().relativePronouns.contains(mention.head)))) {
			mention.mentionType = MentionType.pronoun;
		} else if (!mention.NE.equalsIgnoreCase("OTHER") || part.getWord(mention.headID).posTag.startsWith("NNP")) {
			mention.mentionType = MentionType.proper;
		} else {
			mention.mentionType = MentionType.common;
		}
	}
	
	public void calEnAttribute(Mention em, CoNLLPart part) {
		assignHeadExtent(em, part);
		setMentionType(em, part);
		ArrayList<CoNLLSentence> sentences = part.getCoNLLSentences();
		ArrayList<Element> nerElements = part.getNameEntities();

		em.sentenceID = em.s.getSentenceIdx();
		if (part.getWord(em.headID).posTag.startsWith("PRP")
				|| (em.start == em.end && em.NE.equalsIgnoreCase("OTHER") && (EnDictionary.getInstance().allPronouns
						.contains(em.head.toLowerCase()) || EnDictionary.getInstance().relativePronouns.contains(em.head
						.toLowerCase())))) {
			em.isPronoun = true;
		} else {
			em.isPronoun = false;
		}

		// get Pronoun type
		em.PRONOUN_TYPE = Common.getPronounType(em.getHead());
		em.head = em.head.replace("\n", "").replaceAll("\\s+", "");
		em.extent = em.extent.replace("\n", "").replaceAll("\\s+", "");
		for (int i = em.start; i <= em.end; i++) {
			if (i != em.headID) {
				CoNLLWord word = part.getWord(i);
				String posTag = word.getPosTag();
				if (posTag.equals("NN") || posTag.equals("NR") || posTag.equals("OD") || posTag.equals("JJ")
						|| posTag.equals("NT") || posTag.equals("CD") || posTag.equalsIgnoreCase("RB")) {
					if (!EnDictionary.getInstance().stopWords.contains(word.getWord())) {
						em.modifyList.add(word.getOrig().toLowerCase());
					}
				}
			}
		}
		em.isNNP = false;

		for (Element nerEle : nerElements) {
			if (em.headID >= nerEle.start && em.headID <= nerEle.end) {
				em.NE = nerEle.getContent();
			}
		}

		// determine whether it is a proper noun
		if (em.NE.equalsIgnoreCase("person") || em.NE.equalsIgnoreCase("gpe") || em.NE.equalsIgnoreCase("loc")
				|| em.NE.equalsIgnoreCase("org") || em.NE.equalsIgnoreCase("fac") || em.NE.equalsIgnoreCase("law")) {
			em.isProperNoun = true;
		} else {
			em.isProperNoun = false;
		}
		setGender(em, part);
		setNumber(em, part);
		setAnimacy(em, part);
		setPerson(em, part);
	}

	private void setAnimacy(Mention mention, CoNLLPart part) {
		String headString = mention.head.toLowerCase();
		String nerString = mention.NE;
		if (mention.isPronoun) {
			if (EnDictionary.getInstance().animatePronouns.contains(headString)) {
				mention.animacy = Animacy.animate;
			} else if (EnDictionary.getInstance().inanimatePronouns.contains(headString)) {
				mention.animacy = Animacy.unanimate;
			} else {
				mention.animacy = Animacy.unknown;
			}
		} else if (nerString.equals("PERSON")) {
			mention.animacy = Animacy.animate;
		} else if (nerString.equals("CARDINAL")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.equals("DATE")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.equals("EVENT")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.equals("FAC")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.equals("GPE")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.equals("LAW")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.equals("LOC")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.startsWith("MONEY")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.startsWith("NORP")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.startsWith("ORDINAL")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.startsWith("ORG")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.startsWith("PERCENT")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.startsWith("PRODUCT")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.startsWith("QUANTITY")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.startsWith("TIME")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.startsWith("WORK_OF_ART")) {
			mention.animacy = Animacy.unanimate;
		} else if (nerString.startsWith("LANGUAGE")) {
			mention.animacy = Animacy.unanimate;
		} else {
			mention.animacy = Animacy.unknown;
		}
		if (!mention.isPronoun) {
			if (mention.animacy == Animacy.unknown) {
				if (EnDictionary.getInstance().animateWords.contains(headString)) {
					mention.animacy = Animacy.animate;
				} else if (EnDictionary.getInstance().inanimateWords.contains(headString)) {
					mention.animacy = Animacy.unanimate;
				}
			}
		}
	}

	private void setPerson(Mention mention, CoNLLPart part) {
		// only do for pronoun
		if (!mention.isPronoun) {
			mention.personEng = PersonEng.UNKNOWN;
			return;
		}
		if (EnDictionary.getInstance().firstPersonPronouns.contains(mention.extent.toLowerCase())) {
			if (mention.number == Number.single) {
				mention.personEng = PersonEng.I;
			} else if (mention.number == Number.plural) {
				mention.personEng = PersonEng.WE;
			} else {
				mention.personEng = PersonEng.UNKNOWN;
			}
		} else if (EnDictionary.getInstance().secondPersonPronouns.contains(mention.extent.toLowerCase())) {
			mention.personEng = PersonEng.YOU;
		} else if (EnDictionary.getInstance().thirdPersonPronouns.contains(mention.extent.toLowerCase())) {
			if (mention.gender == Gender.male && mention.number == Number.single) {
				mention.personEng = PersonEng.HE;
			} else if (mention.gender == Gender.female && mention.number == Number.single) {
				mention.personEng = PersonEng.SHE;
			} else if ((mention.gender == Gender.neuter || mention.animacy == Animacy.unanimate)
					&& mention.number == Number.single) {
				mention.personEng = PersonEng.IT;
			} else if (mention.number == Number.plural)
				mention.personEng = PersonEng.THEY;
			else {
				mention.personEng = PersonEng.UNKNOWN;
			}
		} else {
			mention.personEng = PersonEng.UNKNOWN;
		}
	}

	private void setGender(Mention mention, CoNLLPart part) {
		String headString = mention.head.toLowerCase();
		mention.gender = Gender.unknown;
		if (mention.isPronoun) {
			if (EnDictionary.getInstance().malePronouns.contains(mention.extent.toLowerCase())) {
				mention.gender = Gender.male;
			} else if (EnDictionary.getInstance().femalePronouns.contains(mention.extent.toLowerCase())) {
				mention.gender = Gender.female;
			}
		} else {
			// Bergsma list
			if (mention.gender == Gender.unknown) {
				if (EnDictionary.getInstance().maleWords.contains(headString)) {
					mention.gender = Gender.male;
				} else if (EnDictionary.getInstance().femaleWords.contains(headString)) {
					mention.gender = Gender.female;
				} else if (EnDictionary.getInstance().neutralWords.contains(headString)) {
					mention.gender = Gender.neuter;
				}
			}
			int genderNumberCount[] = this.getNumberCount(mention, part);
			if (genderNumberCount != null && mention.number != Number.plural) {
				double male = genderNumberCount[0];
				double female = genderNumberCount[1];
				double neutral = genderNumberCount[2];

				if (male * 0.5 > female + neutral && male > 2)
					mention.gender = Gender.male;
				else if (female * 0.5 > male + neutral && female > 2)
					mention.gender = Gender.female;
				else if (neutral * 0.5 > male + female && neutral > 2)
					mention.gender = Gender.neuter;
			}
		}
	}

	private int[] getNumberCount(Mention mention, CoNLLPart part) {
		int headIndex = mention.headID;
		if(part.getWord(headIndex).rawNamedEntity.startsWith("PER")) {
			ArrayList<String> words = new ArrayList<String>();
			for(int i=mention.start;i<=headIndex;i++) {
				if(part.getWord(i).rawNamedEntity.startsWith("PER")) {
					words.add(part.getWord(i).word);
				}
			}
			for(int i=words.size();i>0;i--) {
				if (EnDictionary.getInstance().bigGenderNumber.containsKey(words.subList(0, i)))
					return EnDictionary.getInstance().bigGenderNumber.get(words.subList(0, i));
			}
		}
		return null;
	}

	public void setNumber(Mention mention, CoNLLPart part) {
		String headString = part.getWord(mention.headID).orig.toLowerCase();
		if (mention.isPronoun) {
			if (EnDictionary.getInstance().pluralPronouns.contains(headString)) {
				mention.number = Number.plural;
			} else if (EnDictionary.getInstance().singularPronouns.contains(headString)) {
				mention.number = Number.single;
			} else {
				mention.number = Number.single;
			}
		} else if (!mention.NE.equalsIgnoreCase("OTHER")) {
			if (!mention.NE.startsWith("ORG")) {
				mention.number = Number.single;
			} else {
				mention.number = Number.single;
			}
		} else {
			String posTag = part.getWord(mention.headID).posTag;
			if (posTag.startsWith("N") && posTag.endsWith("S")) {
				mention.number = Number.plural;
			} else if (posTag.startsWith("N")) {
				mention.number = Number.single;
			} else {
				mention.number = Number.single;
			}
		}
		if (!mention.isPronoun) {
			if (mention.number == Number.single) {
				if (EnDictionary.getInstance().singularWords.contains(part.getWord(mention.headID).orig.toLowerCase())) {
					mention.number = Number.single;
				} else if (EnDictionary.getInstance().pluralPronouns.contains(part.getWord(mention.headID).orig.toLowerCase())) {
					mention.number = Number.plural;
				}
			}
		}
		for(int i=mention.start;i<=mention.end;i++) {
			if(part.getWord(i).posTag.equalsIgnoreCase("CC")) {
				mention.number = Number.plural;
			}
		}
	}
}

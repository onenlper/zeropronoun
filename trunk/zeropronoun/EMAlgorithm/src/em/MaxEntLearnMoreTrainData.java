package em;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import model.Element;
import model.Entity;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.OntoCorefXMLReader;
import model.syntaxTree.MyTreeNode;
import util.Common;
import edu.stanford.nlp.ling.Datum;
import em.ResolveGroup.Entry;

public class MaxEntLearnMoreTrainData {

	static ArrayList<CoNLLPart> parts = new ArrayList<CoNLLPart>();

	// static HashMap<Context, Double> p_context_ = new HashMap<Context,
	// Double>();

	static Parameter numberP = new Parameter(
			1.0 / ((double) EMUtil.Number.values().length));
	static Parameter genderP = new Parameter(
			1.0 / ((double) EMUtil.Gender.values().length));
	static Parameter personP = new Parameter(
			1.0 / ((double) EMUtil.Person.values().length));
	static Parameter personQP = new Parameter(
			1.0 / ((double) EMUtil.Person.values().length));
	static Parameter animacyP = new Parameter(
			1.0 / ((double) EMUtil.Animacy.values().length));

	static HashMap<String, Double> contextPrior = new HashMap<String, Double>();

	static HashMap<String, Double> contextOverall = new HashMap<String, Double>();

	// static Parameter contextP = new Parameter(1.0 / 2.0);

	static HashMap<String, Double> fracContextCount = new HashMap<String, Double>();

	static SuperviseFea superFea;

	static List<Datum<String, String>> trainingData = new ArrayList<Datum<String, String>>();

	static ArrayList<String> yasmet = new ArrayList<String>();
	static int maxAnts = 0;

	static ArrayList<String> svmRanks = new ArrayList<String>();
	static int qid = 0;

	private static ArrayList<Element> getChGoldNE(CoNLLPart part) {
		String documentID = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/train/data/chinese/annotations/"
				+ part.docName + ".v4_gold_skel";
		// System.out.println(documentID);
		CoNLLDocument document = new CoNLLDocument(documentID);
		CoNLLPart goldPart = document.getParts().get(part.getPartID());
		// for (Element ner : goldPart.getNameEntities()) {
		// int start = ner.start;
		// int end = ner.end;
		// String ne = ner.content;
		//
		// StringBuilder sb = new StringBuilder();
		// for (int k = start; k <= end; k++) {
		// sb.append(part.getWord(k).word).append(" ");
		// }
		// // System.out.println(sb.toString() + " # " + ne);
		// // System.out.println(goldPart.);
		// }
		return goldPart.getNameEntities();
	}

	static HashMap<String, Integer> map = new HashMap<String, Integer>();

	public static void extractGroups(CoNLLPart part) {
		// ArrayList<Element> goldNE = getChGoldNE(part);
		HashMap<String, Integer> chainMap = EMUtil.formChainMap(part
				.getChains());
		ArrayList<Mention> allMentions = EMUtil.extractMention(part);
		Collections.sort(allMentions);
		EMUtil.assignNE(allMentions, part.getNameEntities());

		ArrayList<Mention> goldInChainZeroses = EMUtil.getAnaphorZeros(part
				.getChains());
		for (Entity e : part.getChains()) {
			for (Mention m : e.mentions) {
				m.entity = e;
				if (m.end == -1) {
					continue;
				}
				m.head = part.getWord(m.start).word;
				StringBuilder sb = new StringBuilder();
				for (int i = m.start; i <= m.end; i++) {
					sb.append(part.getWord(i).word);
				}
				m.extent = sb.toString().trim();
				MyTreeNode root = part.getWord(m.start).sentence
						.getSyntaxTree().root;
				MyTreeNode left = root.getLeaves().get(
						part.getWord(m.start).indexInSentence);
				MyTreeNode right = root.getLeaves().get(
						part.getWord(m.end).indexInSentence);

				ArrayList<MyTreeNode> leftAncestors = left.getAncestors();
				ArrayList<MyTreeNode> rightAncestors = right.getAncestors();
				for (int i = 0; i < leftAncestors.size()
						&& i < rightAncestors.size(); i++) {
					if(leftAncestors.get(i)==rightAncestors.get(i)) {
						m.NP = leftAncestors.get(i);
					} else {
						break;
					}
				}
			}
		}
		for (Mention z : goldInChainZeroses) {
			if (z.end == -1) {
				z.isAZP = true;
				// find V
				// TODO
				EMUtil.assignVNode(z, part);
				z.s = part.getWord(z.start).sentence;
			} else {
				Common.bangErrorPOS("!!!");
			}
		}
		allMentions.addAll(goldInChainZeroses);
		Collections.sort(allMentions);

		for (int i = 0; i < allMentions.size(); i++) {
			Mention m = allMentions.get(i);

			if (m.gram == EMUtil.Grammatic.subject && m.start == m.end
					&& part.getWord(m.end).posTag.equals("PN")) {
				String ext = m.extent;
				if (map.containsKey(ext)) {
					map.put(ext, map.get(ext) + 1);
				} else {
					map.put(ext, 1);
				}
			}

			// TODO assign number, gender, person, animacy for AZPs

			if (
//					m.isAZP
//			 ||
			 (m.gram == EMUtil.Grammatic.subject
			 && EMUtil.pronouns.contains(m.extent))
			) {
				// String goldPro = m.extent;
				qid++;
				// for (String pronoun : EMUtil.pronounList) {
				// m.extent = pronoun;
				ArrayList<Mention> ants = new ArrayList<Mention>();
				int corefCount = 0;
				for (int j = i - 1; j >= 0; j--) {
					Mention ant = allMentions.get(j);
					if (ant.end == -1) {
						continue;
					}
					ants.add(ant);
					ant.MI = Context.calMI(ant, m);
					ant.isBest = false;
					boolean coref = isCoref(chainMap, m, ant);
					if (coref) {
						corefCount++;
					}
					if (m.s.getSentenceIdx() - ant.s.getSentenceIdx() > 2) {
						break;
					}
				}

				EMUtil.setPronounAttri(m, part);
				String proSpeaker = part.getWord(m.start).speaker;

				Collections.sort(ants);
				Collections.reverse(ants);
				ApplyEM.findBest(m, ants);

				StringBuilder ysb = new StringBuilder();
				if (ants.size() > maxAnts) {
					maxAnts = ants.size();
				}

				boolean findFirstSubj = false;
				boolean findCoref = false;
				for (int k = 0; k < ants.size(); k++) {
					Mention ant = ants.get(k);

					boolean fs = false;
					if (!findFirstSubj && ant.gram == EMUtil.Grammatic.subject) {
						findFirstSubj = true;
						fs = true;
					}
					String antSpeaker = part.getWord(ant.start).speaker;

					Context context = Context.buildContext(ant, m, part, fs);

					boolean sameSpeaker = proSpeaker.equals(antSpeaker);

					Entry entry = new Entry(ant, context, sameSpeaker, fs);
					boolean coref = isCoref(chainMap, m, ant);
					if (coref) {
						if (!findCoref) {
							ysb.insert(0, k + " @ ");
							findCoref = true;
						}
					}
					int rank = 0;
					if (coref) {
						// if(m.extent.equals(goldPro)) {
						// rank = 3;
						// } else {
						// rank = 2;
						// }
						rank = 1;
					} else {
						rank = -1;
					}
					// coref = coref && m.extent.equals(goldPro);

					// //
					// if (sameSpeaker) {
					// pStr = entry.person.name() + "=" +
					// EMUtil.getPerson(pronoun).name();
					// } else {
					// pStr = entry.person.name() + "!="
					// + EMUtil.getPerson(pronoun).name();
					// }
					// String nStr = entry.number.name() + "="
					// + EMUtil.getNumber(pronoun).name();
					// String gStr = entry.gender.name() + "="
					// + EMUtil.getGender(pronoun).name();
					// String aStr = entry.animacy.name() + "="
					// + EMUtil.getAnimacy(pronoun).name();

					ysb.append(getYamset(coref, ant, m, context, sameSpeaker,
							entry, superFea, corefCount, part));
					svmRanks.add(getSVMRank(rank, ant, m, context, sameSpeaker,
							entry, superFea, part));
				}
				for (int k = ants.size(); k < 84; k++) {
					ysb.append("@ 0 NOCLASS 1 # ");
				}
				if (corefCount > 0) {
					yasmet.add(ysb.toString().trim());
				}
				// }
				// m.extent = goldPro;
				CoNLLSentence s = part.getWord(m.start).sentence;
				// try {
				extractGuessPronounFea(m, s, part);
				// } catch (Exception e) {
				// System.out.println(s.getText());
				// System.out.println(m.extent);
				// Common.bangErrorPOS("");
				// }
			}
		}
	}

	static FileWriter genderWriter;
	static FileWriter numberWriter;
	static FileWriter personWriter;
	static FileWriter animacyWriter;
	// static FileWriter nbFeaWriter;
	static GuessPronounFea guessFea;

	public static void extractGuessPronounFea(Mention pronoun, CoNLLSentence s,
			CoNLLPart part) {
		try {
			guessFea.configure(pronoun.start - 1, pronoun.start + 1, s, part);
			int label = EMUtil.pronounList.indexOf(pronoun.extent) + 1;

			// label = pronoun.animacy.ordinal() + 1;

			String feaStr = guessFea.getSVMFormatString();
			String v = EMUtil.getFirstVerb(pronoun.V);

			// TODO Yasmet format
			// NUMBER, GENDER, PERSON, ANIMACY
			String tks[] = feaStr.split("\\s+");
			int all = EMUtil.Number.values().length;
			int y = pronoun.number.ordinal();
			double[] numberProbs = ApplyMaxEnt.selectRestriction("number", all,
					v);
			String nYSB = transform(tks, all, y, numberProbs);

			all = EMUtil.Gender.values().length - 1;
			y = pronoun.gender.ordinal();
			double[] genderProbs = ApplyMaxEnt.selectRestriction("gender", all,
					v);
			String gYSB = transform(tks, all, y, genderProbs);

			all = EMUtil.Person.values().length;
			y = pronoun.person.ordinal();
			double[] personProbs = ApplyMaxEnt.selectRestriction("person", all,
					v);
			String pYSB = transform(tks, all, y, personProbs);

			all = EMUtil.Animacy.values().length - 1;
			y = pronoun.animacy.ordinal();
			double[] animacyProbs = ApplyMaxEnt.selectRestriction("animacy",
					all, v);
			String aYSB = transform(tks, all, y, animacyProbs);

			numberWriter.write(nYSB + "\n");
			genderWriter.write(gYSB + "\n");
			personWriter.write(pYSB + "\n");
			animacyWriter.write(aYSB + "\n");

			// String nbFeaStr = guessFea.getMyBNFormatString();
			// nbFeaWriter.write((label - 1) + " " + nbFeaStr + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String transform(String[] tks, int all, int y,
			double probs[]) {
		StringBuilder ysb = new StringBuilder();
		ysb.append(y);
		ysb.append(" @ ");
		for (int i = 0; i < all; i++) {
			ysb.append("@ ");
			if (i == y) {
				ysb.append("1 ");
			} else {
				ysb.append("0 ");
			}
			for (String tk : tks) {
				if (tk.isEmpty()) {
					continue;
				}
				int k = tk.indexOf(":");
				String f = tk.substring(0, k);
				String v = tk.substring(k + 1);
				ysb.append(f + "_" + i).append(" ").append(v).append(" ");
			}

			for (int j = 0; j < probs.length; j++) {
				ysb.append(j + "_" + i).append(" ").append(probs[j])
						.append(" ");
			}
			ysb.append("# ");
		}
		return ysb.toString();
	}

	public static double all = 0;
	public static double match = 0;

	private static boolean isCoref(HashMap<String, Integer> chainMap,
			Mention m, Mention ant) {
		boolean coref = chainMap.containsKey(m.toName())
				&& chainMap.containsKey(ant.toName())
				&& chainMap.get(m.toName()).intValue() == chainMap.get(
						ant.toName()).intValue();
		return coref;
	}

	public static String getSVMRank(int rank, Mention ant, Mention pro,
			Context context, boolean sameSpeaker, Entry entry,
			SuperviseFea superFea, CoNLLPart part) {
		String pronoun = pro.extent;
		String pStr = "";
		if (sameSpeaker) {
			pStr = entry.person.name() + "=" + pro.person.name();
		} else {
			pStr = entry.person.name() + "!=" + pro.person.name();
		}
		String nStr = entry.number.name() + "=" + pro.number.name();
		String gStr = entry.gender.name() + "=" + pro.gender.name();
		String aStr = entry.animacy.name() + "=" + pro.animacy.name();

		superFea.configure(pStr, nStr, gStr, aStr, context, ant, pro, part);

		String svm = superFea.getSVMFormatString();
		String label = Integer.toString(rank);
		return label + " qid:" + qid + " " + svm;
		// return ysb.toString();
	}

	public static String getYamset(boolean coref, Mention ant, Mention pro,
			Context context, boolean sameSpeaker, Entry entry,
			SuperviseFea superFea, double corefCount, CoNLLPart part) {
		String pStr = "";
		if (sameSpeaker) {
			pStr = entry.person.name() + "=" + pro.person.name();
		} else {
			pStr = entry.person.name() + "!=" + pro.person.name();
		}
		String nStr = entry.number.name() + "=" + pro.number.name();
		String gStr = entry.gender.name() + "=" + pro.gender.name();
		String aStr = entry.animacy.name() + "=" + pro.animacy.name();

		superFea.configure(pStr, nStr, gStr, aStr, context, ant, pro, part);

		String fea = superFea.getSVMFormatString();
		String tks[] = fea.split("\\s+");
		StringBuilder ysb = new StringBuilder();
		ysb.append("@ ");
		if (coref) {
			ysb.append(1.0 / corefCount + " ");
		} else {
			ysb.append("0 ");
		}
		for (String tk : tks) {
			int k = tk.indexOf(":");
			String f = tk.substring(0, k);
			String v = tk.substring(k + 1);
			ysb.append(f).append(" ").append(v).append(" ");
			// System.out.println(part.folder);
		}
		ysb.append("# ");
		return ysb.toString();
	}

	private static void extractCoNLL(ArrayList<ResolveGroup> groups) {
		// CoNLLDocument d = new CoNLLDocument("train_auto_conll");

		ArrayList<String> lines = Common.getLines("chinese_list_all_train");
		for (String line : lines) {
			System.out.println(line);
			CoNLLDocument d = new CoNLLDocument(line);

			OntoCorefXMLReader.addGoldZeroPronouns(d, false);

			for (CoNLLPart part : d.getParts()) {
				extractGroups(part);
			}
		}
//		CoNLLDocument d = new CoNLLDocument("train_gold_conll");
//		parts.addAll(d.getParts());
//		int i = d.getParts().size();
//		for (CoNLLPart part : parts) {
//			extractGroups(part);
//			// System.out.println(i--);
//		}
	}

	public static void main(String args[]) throws Exception {
		EMUtil.train = true;

		genderWriter = new FileWriter("gender.train");
		genderWriter.write(EMUtil.Gender.values().length + "\n");
		numberWriter = new FileWriter("number.train");
		numberWriter.write(EMUtil.Number.values().length + "\n");
		personWriter = new FileWriter("person.train");
		personWriter.write(EMUtil.Person.values().length + "\n");
		animacyWriter = new FileWriter("animacy.train");
		animacyWriter.write(EMUtil.Animacy.values().length - 1 + "\n");
		// nbFeaWriter = new FileWriter("guessPronoun.train.nb");
		guessFea = new GuessPronounFea(true, "guessPronoun");

		superFea = new SuperviseFea(true, "supervise");

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();

		extractCoNLL(groups);

		superFea.freeze();
		yasmet.add(0, Integer.toString(maxAnts));
		Common.outputLines(yasmet, "yasmet.train");

		System.out.println(":" + match / all);

		Common.outputLines(svmRanks, "svmRank.train");

		System.out.println("Qid: " + qid);

		guessFea.freeze();
		guessFea.clear();
		// nbFeaWriter.close();
		genderWriter.close();
		numberWriter.close();
		personWriter.close();
		animacyWriter.close();

		for (String k : map.keySet()) {
			System.out.println(k + ":" + map.get(k));
		}
	}

}

package em;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import model.Element;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.Common;
import edu.stanford.nlp.ling.Datum;
import em.ResolveGroup.Entry;

public class MaxEntLearn10b {

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

	static ArrayList<String> yasmet10 = new ArrayList<String>();
	static int maxAnts = 0;

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
		for (int i = 0; i < allMentions.size(); i++) {
			Mention m = allMentions.get(i);

			if (m.gram == EMUtil.Grammatic.subject && m.start == m.end
					&& part.getWord(m.end).posTag.equals("PN")) {
			}

			if (m.gram == EMUtil.Grammatic.subject
					&& EMUtil.pronouns.contains(m.extent)
			// && chainMap.containsKey(m.toName())
			) {
				ArrayList<Mention> ants = new ArrayList<Mention>();
				int corefCount = 0;
				for (int j = i - 1; j >= 0; j--) {
					Mention ant = allMentions.get(j);
					ants.add(ant);
					ant.MI = Context.calMI(ant, m);
					boolean coref = isCoref(chainMap, m, ant);
					if (coref) {
						corefCount++;
					}
					if (m.s.getSentenceIdx() - ant.s.getSentenceIdx() > 2) {
						break;
					}
				}
				Collections.sort(ants);
				Collections.reverse(ants);

				if (corefCount == 0) {
					continue;
				}

				for (Mention ant : ants) {
					ant.isBest = false;
				}

				ApplyEM.findBest(m, ants);

				if (ants.size() > maxAnts) {
					maxAnts = ants.size();
				}
				String origPro = m.extent;

				String ext = m.extent;
				if (map.containsKey(ext)) {
					map.put(ext, map.get(ext) + 1);
				} else {
					map.put(ext, 1);
				}
				StringBuilder ysb = new StringBuilder();
				for (int h = 0; h < EMUtil.pronounList.size(); h++) {
					m.extent = EMUtil.pronounList.get(h);
					generateInstance(part, chainMap, m, ants,
							corefCount, origPro, h, ysb);
				}
				for (int k = ants.size() * EMUtil.pronounList.size(); k < 840; k++) {
					ysb.append("@ 0 NOCLASS 1 # ");
				}
				yasmet10.add(ysb.toString());
				m.extent = origPro;
			}
		}
	}

	private static void generateInstance(CoNLLPart part,
			HashMap<String, Integer> chainMap, Mention m,
			ArrayList<Mention> ants, int corefCount, String origPro, int index, StringBuilder ysb) {
		EMUtil.setPronounAttri(m, part);
		String proSpeaker = part.getWord(m.start).speaker;
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
			if (coref && origPro.equals(m.extent)) {
				if (!findCoref) {
					ysb.insert(0, Integer.toString(k + index * EMUtil.pronounList.size()) + " @ ");
					findCoref = true;
				}
			}
			if (origPro.equals(m.extent)) {
				ysb.append(getYamset(coref, ant, m, context, sameSpeaker,
						entry, superFea, corefCount, part));
			} else {
				ysb.append(getYamset(false, ant, m, context, sameSpeaker, entry,
						superFea, 1.0 * ants.size(), part));
			}
		}
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

	public static String getYamset2(boolean coref, Mention ant, Mention pro,
			String proPerson, String proNumber, String proGender,
			String proAnimacy, Context context, boolean sameSpeaker,
			Entry entry, SuperviseFea superFea, double corefCount,
			CoNLLPart part) {
		String pStr = "";
		if (sameSpeaker) {
			pStr = entry.person.name() + "=" + proPerson;
		} else {
			pStr = entry.person.name() + "!=" + proPerson;
		}
		String nStr = entry.number.name() + "=" + proNumber;
		String gStr = entry.gender.name() + "=" + proGender;
		String aStr = entry.animacy.name() + "=" + proAnimacy;

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

	public static String getYamset(boolean coref, Mention ant, Mention pro,
			Context context, boolean sameSpeaker, Entry entry,
			SuperviseFea superFea, double corefCount, CoNLLPart part) {
		String pronoun = pro.extent;
		String pStr = "";
		String nStr = "";
		String gStr = "";
		String aStr = "";

		if (entry != null) {
//			if (sameSpeaker) {
//				pStr = entry.person.name() + "="
//						+ EMUtil.getPerson(pronoun).name();
//			} else {
//				pStr = entry.person.name() + "!="
//						+ EMUtil.getPerson(pronoun).name();
//			}
//			nStr = entry.number.name() + "=" + EMUtil.getNumber(pronoun).name();
//			gStr = entry.gender.name() + "=" + EMUtil.getGender(pronoun).name();
//			aStr = entry.animacy.name() + "="
//					+ EMUtil.getAnimacy(pronoun).name();
			
			if (sameSpeaker) {
				pStr = entry.person.name().equals(EMUtil.getPerson(pronoun).name()) + "sameS";
			} else {
				pStr = entry.person.name() + "#" + EMUtil.getPerson(pronoun).name() + "diffS";
			}
			nStr = entry.number.name().equals(EMUtil.getNumber(pronoun).name()) + "number";
			gStr = entry.gender.name().equals(EMUtil.getGender(pronoun).name()) + "gender";
			aStr = entry.animacy.name().equals(EMUtil.getAnimacy(pronoun).name()) + "animacy";
		}

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
		CoNLLDocument d = new CoNLLDocument("train_gold_conll");
		parts.addAll(d.getParts());
		int i = d.getParts().size();
		for (CoNLLPart part : parts) {
			extractGroups(part);
			System.out.println(i--);
		}
	}

	public static void main(String args[]) throws Exception {
		EMUtil.train = true;
		// nbFeaWriter = new FileWriter("guessPronoun.train.nb");

		superFea = new SuperviseFea(true, "supervise");

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();

		extractCoNLL(groups);

		superFea.freeze();
		yasmet10.add(0, Integer.toString(maxAnts  * EMUtil.pronounList.size()));
		Common.outputLines(yasmet10, "yasmet10.train");
		System.out.println(":" + match / all);
	}

}

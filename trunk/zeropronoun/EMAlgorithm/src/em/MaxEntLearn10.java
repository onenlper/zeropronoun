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

public class MaxEntLearn10 {

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

	static ArrayList<ArrayList<String>> yasmet10 = new ArrayList<ArrayList<String>>();
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
				String ext = m.extent;
				if (map.containsKey(ext)) {
					map.put(ext, map.get(ext) + 1);
				} else {
					map.put(ext, 1);
				}
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

				if(corefCount==0) {
					continue;
				}
				
				for (Mention ant : ants) {
					ant.isBest = false;
				}

				ApplyEM.findBest(m, ants);

				Mention fake = new Mention();
				fake.fake = true;
				ants.add(fake);
				
				if (ants.size() > maxAnts) {
					maxAnts = ants.size();
				}
				String origPro = m.extent;

				for (int h = 0; h < EMUtil.pronounList.size(); h++) {
					m.extent = EMUtil.pronounList.get(h);
					String str = generateInstance(part, chainMap, m, ants,
							corefCount, origPro);
					yasmet10.get(h).add(str);
				}
				m.extent = origPro;
			}
		}
	}

	private static String generateInstance(CoNLLPart part,
			HashMap<String, Integer> chainMap, Mention m,
			ArrayList<Mention> ants, int corefCount, String origPro) {
		EMUtil.setPronounAttri(m, part);
		StringBuilder ysb = new StringBuilder();
		String proSpeaker = part.getWord(m.start).speaker;
		boolean findFirstSubj = false;
		boolean findCoref = false;
		for (int k = 0; k < ants.size(); k++) {
			Mention ant = ants.get(k);

			if (ant.fake) {
				if(!origPro.equals(m.extent)) {
					ysb.insert(0, k + " @ ");
				}
				ysb.append(getYamset(!origPro.equals(m.extent), ant, m, null,
						false, null, superFea, 1, part));

			} else {
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
					if (!findCoref && origPro.equals(m.extent)) {
						ysb.insert(0, k + " @ ");
						findCoref = true;
					}
				}
				ysb.append(getYamset(coref && origPro.equals(m.extent), ant, m,
						context, sameSpeaker, entry, superFea, corefCount, part));
			}
		}
		for (int k = ants.size(); k < 85; k++) {
			ysb.append("@ 0 NOCLASS 1 # ");
		}
		return ysb.toString().trim();
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
			if (sameSpeaker) {
				pStr = entry.person.name() + "="
						+ EMUtil.getPerson(pronoun).name();
			} else {
				pStr = entry.person.name() + "!="
						+ EMUtil.getPerson(pronoun).name();
			}
			nStr = entry.number.name() + "=" + EMUtil.getNumber(pronoun).name();
			gStr = entry.gender.name() + "=" + EMUtil.getGender(pronoun).name();
			aStr = entry.animacy.name() + "="
					+ EMUtil.getAnimacy(pronoun).name();
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
		for (int i = 0; i < EMUtil.pronounList.size(); i++) {
			yasmet10.add(new ArrayList<String>());
		}

		superFea = new SuperviseFea(true, "supervise");

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();

		extractCoNLL(groups);

		superFea.freeze();

		for (int i = 0; i < EMUtil.pronounList.size(); i++) {
			ArrayList<String> yasmet = yasmet10.get(i);
			yasmet.add(0, Integer.toString(maxAnts));
			Common.outputLines(yasmet, "yasmet10.train" + i);
		}
		System.out.println(":" + match / all);

		for (String k : map.keySet()) {
			System.out.println(k + ":" + map.get(k));
		}
	}

}

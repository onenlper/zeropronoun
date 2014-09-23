package em;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import mentionDetect.ParseTreeMention;
import model.Element;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import util.Common;
import edu.stanford.nlp.ling.Datum;
import em.ResolveGroup.Entry;

public class TrainEngPronoun {

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

	// static SuperviseFea superFea;

	static EngSuperFea engSuperFea;

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
		HashMap<String, Integer> chainMap = EMUtil.formChainMap(part
				.getChains());

		ParseTreeMention ptm = new ParseTreeMention();
		ArrayList<Mention> allMentions = ptm.getMentions(part);

		Collections.sort(allMentions);
		EMUtil.assignNE(allMentions, part.getNameEntities());
		for (int i = 0; i < allMentions.size(); i++) {
			Mention m = allMentions.get(i);

			if (m.start == m.end
					&& part.getWord(m.start).posTag.startsWith("PRP")) {
				ArrayList<Mention> ants = new ArrayList<Mention>();
				int corefCount = 0;
				for (int j = i - 1; j >= 0; j--) {
					Mention ant = allMentions.get(j);
					ants.add(ant);
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
				maxAnts = Math.max(maxAnts, ants.size());

				StringBuilder ysb = new StringBuilder();
				generateInstance(part, chainMap, m, ants, corefCount, ysb);
				for (int k = ants.size(); k < 87; k++) {
					ysb.append("@ 0 NOCLASS 1 # ");
				}
				yasmet10.add(ysb.toString());
			}
		}
	}

	private static void generateInstance(CoNLLPart part,
			HashMap<String, Integer> chainMap, Mention m,
			ArrayList<Mention> ants, int corefCount, StringBuilder ysb) {

		boolean findCoref = false;

		for (int k = 0; k < ants.size(); k++) {
			Mention ant = ants.get(k);

			boolean coref = isCoref(chainMap, m, ant);
			if (coref) {
				if (!findCoref) {
					ysb.insert(0, Integer.toString(k) + " @ ");
					findCoref = true;
				}
			}
			ysb.append(getYamset(coref, ant, m, engSuperFea, corefCount, part,
					m.s.getSentenceIdx() - ant.s.getSentenceIdx(), m.start
							- ant.end));
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
			EngSuperFea superFea, double corefCount, CoNLLPart part,
			int disSent, int disWord) {
		superFea.configure(ant, pro, part, disSent, disWord);

		String fea = superFea.getSVMFormatString().trim();
		// System.out.println(fea);
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
		CoNLLDocument d = new CoNLLDocument("train_gold_conll.eng");
		d.language = "english";
		parts.addAll(d.getParts());
		for (CoNLLPart part : parts) {
			part.lang = "eng";
			extractGroups(part);
		}
	}

	public static void main(String args[]) throws Exception {
		EMUtil.train = true;
		// nbFeaWriter = new FileWriter("guessPronoun.train.nb");

		engSuperFea = new EngSuperFea(true, "superEng");

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();

		extractCoNLL(groups);

		engSuperFea.freeze();
		yasmet10.add(0, Integer.toString(maxAnts));
		Common.outputLines(yasmet10, "yasmetEng.train");
		System.out.println(":" + match / all);
	}

}

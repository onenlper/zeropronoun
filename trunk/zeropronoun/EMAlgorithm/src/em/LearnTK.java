package em;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import model.Element;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import util.Common;
import util.TKUtil;
import edu.stanford.nlp.ling.Datum;
import em.ResolveGroup.Entry;

public class LearnTK {

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

	static List<Datum<String, String>> trainingData = new ArrayList<Datum<String, String>>();

	static ArrayList<String> treeKernels = new ArrayList<String>();
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
				// String goldPro = m.extent;
				qid++;
				// for (String pronoun : EMUtil.pronounList) {
				// m.extent = pronoun;
				ArrayList<Mention> ants = new ArrayList<Mention>();
				int corefCount = 0;
				for (int j = i - 1; j >= 0; j--) {
					Mention ant = allMentions.get(j);
					// TODO
					// 他们 爱国 的 标准#228,231
					// 他们#228,228
					if (ant.end < m.start) {
						ants.add(ant);
						// ant.MI = Context.calMI(ant, m);
						// ant.isBest = false;
						boolean coref = isCoref(chainMap, m, ant);
						if (coref) {
							corefCount++;
						}
						if (m.s.getSentenceIdx() - ant.s.getSentenceIdx() > 2) {
							break;
						}
					}
				}
				EMUtil.setPronounAttri(m);
				String proSpeaker = part.getWord(m.start).speaker;

				Collections.sort(ants);
				Collections.reverse(ants);
				// ApplyEM.findBest(m, ants);

				StringBuilder ysb = new StringBuilder();
				if (ants.size() > maxAnts) {
					maxAnts = ants.size();
				}

				boolean findFirstSubj = false;
				boolean findCoref = false;
				for (int k = 0; k < ants.size(); k++) {

//					for (int j = k + 1; j < ants.size(); j++) {
						Mention ant1 = ants.get(k);
//						Mention ant2 = ants.get(j);

						boolean coref1 = isCoref(chainMap, m, ant1);
//						boolean coref2 = isCoref(chainMap, m, ant2);
//						if (coref1 == coref2) {
//							continue;
//						}

						String tk1 = getTKStr(ant1, m, false, part, false);
//						String tk2 = getTKStr(ant2, m, false, part, false);
						StringBuilder sb = new StringBuilder();
//						if (!coref1) {
//							sb.append("-1 ").append("|BT| ").append(tk2)
//									.append(" |BT| ").append(tk1)
//									.append(" |ET|");
//						} else {
//							sb.append("1 ").append("|BT| ").append(tk1)
//									.append(" |BT| ").append(tk2)
//									.append(" |ET|");
//						}
						if(coref1) {
							sb.append("+1 ").append("|BT| ").append(tk1).append(" |ET|");
						} else {
							sb.append("-1 ").append("|BT| ").append(tk1).append(" |ET|");
						}
						
						
						treeKernels.add(sb.toString());

						// StringBuilder sb2 = new StringBuilder();
						// sb2.append("-1 ");
						// if (coref1 && !coref2) {
						// sb2.append("|BT| ").append(tk2).append(" |BT| ")
						// .append(tk1).append(" |ET|");
						// } else {
						// sb2.append("|BT| ").append(tk1).append(" |BT| ")
						// .append(tk2).append(" |ET|");
						// }
						// treeKernels.add(sb2.toString());

//					}
				}
				CoNLLSentence s = part.getWord(m.start).sentence;
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

	public static String getSVMRank(int rank, Mention ant, Mention pro,
			Context context, boolean sameSpeaker, Entry entry,
			SuperviseFea superFea, CoNLLPart part) {
		String pronoun = pro.extent;
		String pStr = "";
		if (sameSpeaker) {
			pStr = entry.person.name() + "=" + EMUtil.getPerson(pronoun).name();
		} else {
			pStr = entry.person.name() + "!="
					+ EMUtil.getPerson(pronoun).name();
		}
		String nStr = entry.number.name() + "="
				+ EMUtil.getNumber(pronoun).name();
		String gStr = entry.gender.name() + "="
				+ EMUtil.getGender(pronoun).name();
		String aStr = entry.animacy.name() + "="
				+ EMUtil.getAnimacy(pronoun).name();

		superFea.configure(pStr, nStr, gStr, aStr, context, ant, pro, part);

		String svm = superFea.getSVMFormatString();
		String label = Integer.toString(rank);
		return label + " qid:" + qid + " " + svm;
		// return ysb.toString();
	}

	private static String getTKStr(Mention ant, Mention pro,
			boolean sameSpeaker, CoNLLPart part, boolean isZP) {
		String tk = TKUtil.getMinExpansion(ant, pro, part, isZP, "");
//		 String tk = TKUtil.getSimpleExpansion(ant, pro, part, false, "");
//		 String tk = TKUtil.getFullExpansion(ant, pro, part, false, "");
//		 String tk = TKUtil.getDynamicExpansion(ant, pro, part, false, "");
		// System.out.println("###");
		// System.out.println(tk);
		// System.out.println("###");
		return tk;
	}

	public static String getYamset(boolean coref, Mention ant, Mention pro,
			Context context, boolean sameSpeaker, Entry entry,
			SuperviseFea superFea, double corefCount, CoNLLPart part) {
		String pronoun = pro.extent;
		String pStr = "";
		if (sameSpeaker) {
			pStr = entry.person.name() + "=" + EMUtil.getPerson(pronoun).name();
		} else {
			pStr = entry.person.name() + "!="
					+ EMUtil.getPerson(pronoun).name();
		}
		String nStr = entry.number.name() + "="
				+ EMUtil.getNumber(pronoun).name();
		String gStr = entry.gender.name() + "="
				+ EMUtil.getGender(pronoun).name();
		String aStr = entry.animacy.name() + "="
				+ EMUtil.getAnimacy(pronoun).name();

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
		// CoNLLDocument d = new CoNLLDocument("train_gold_conll");
		ArrayList<String> lines = Common.getLines("chinese_list_" + folder
				+ "_train");
		for (String line : lines) {
			CoNLLDocument d = new CoNLLDocument(line);
			parts.addAll(d.getParts());
		}
		int i = parts.size();		
		for (CoNLLPart part : parts) {
			extractGroups(part);
//			System.out.println(i--);
		}
	}

	public static String folder;

	public static void main(String args[]) throws Exception {
		if (args.length < 1) {
			Common.bangErrorPOS("java ~ folder");
		}
		EMUtil.train = true;
		folder = args[0];

		// superFea = new SuperviseFea(true, "supervise");

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();

		extractCoNLL(groups);

		// superFea.freeze();
		Common.outputLines(treeKernels, "treeKernel." + folder);

		System.out.println(":" + match / all);

		Common.outputLines(svmRanks, "svmRank." + folder);

		// System.out.println("Qid: " + qid);
		// for(String k : map.keySet()) {
		// System.out.println(k + ":" + map.get(k));
		// }
	}

}

package em;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
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
import edu.stanford.nlp.ling.Datum;
import em.ResolveGroup.Entry;

public class LearnSuperProb {

	static ArrayList<CoNLLPart> parts = new ArrayList<CoNLLPart>();

	// static HashMap<Context, Double> p_context_ = new HashMap<Context,
	// Double>();

	static Parameter numberP = new Parameter();
	static Parameter genderP = new Parameter();
	static Parameter personP = new Parameter();
	static Parameter personQP = new Parameter();
	static Parameter animacyP = new Parameter();
	
	static Parameter positionP = new Parameter();
	static Parameter miP = new Parameter();
	static Parameter bestP = new Parameter();

	static HashMap<String, Double> contextPrior = new HashMap<String, Double>();

	static HashMap<String, Double> contextOverall = new HashMap<String, Double>();

	// static Parameter contextP = new Parameter(1.0 / 2.0);

	static HashMap<String, Double> fracContextCount = new HashMap<String, Double>();

	static SuperviseFea superFea;

	static List<Datum<String, String>> trainingData = new ArrayList<Datum<String, String>>();

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

				generateInstance(part, chainMap, m, ants,
						corefCount);

			}
		}
	}

	private static String generateInstance(CoNLLPart part,
			HashMap<String, Integer> chainMap, Mention m,
			ArrayList<Mention> ants, double corefCount) {
		EMUtil.setPronounAttri(m, part);
		StringBuilder ysb = new StringBuilder();
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
			
			
//			static Parameter numberP = new Parameter();
//			static Parameter genderP = new Parameter();
//			static Parameter personP = new Parameter();
//			static Parameter personQP = new Parameter();
//			static Parameter animacyP = new Parameter();
			
			if (coref) {
				// TODO
				numberP.addFracCount(EMUtil.getNumber(m.extent).name(), entry.number.name(), 1.0/corefCount);
				genderP.addFracCount(EMUtil.getGender(m.extent).name(), entry.gender.name(), 1.0/corefCount);
				if(sameSpeaker) {
					personP.addFracCount(EMUtil.getPerson(m.extent).name(), entry.person.name(), 1.0/corefCount);					
				} else {
					personQP.addFracCount(EMUtil.getPerson(m.extent).name(), entry.person.name(), 1.0/corefCount);
				}
				animacyP.addFracCount(EMUtil.getAnimacy(m.extent).name(), entry.animacy.name(), 1.0/corefCount);
				CoNLLSentence pronounS = part.getWord(m.start).sentence;
				CoNLLSentence antS = part.getWord(ant.start).sentence;
				int antSID = antS.getSentenceIdx();
				int proSID = pronounS.getSentenceIdx();

				short senDis = (short) (proSID - antSID);
				positionP.addFracCount("position", Short.toString(senDis), 1.0/corefCount);
				
				
				
				miP.addFracCount("context", Boolean.toString(Context.MI>0) + "#" + "", 1.0/corefCount);
				
				bestP.addFracCount("bestP", Boolean.toString(ant.isBest), 1.0/corefCount);
			}
			ysb.append(getYamset(coref, ant, m, context, sameSpeaker, entry,
					superFea, corefCount, part));
		}
		for (int k = ants.size(); k < 84; k++) {
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

		superFea = new SuperviseFea(true, "supervise");

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();

		extractCoNLL(groups);

		superFea.freeze();

		System.out.println(":" + match / all);
		double all = 0;
		
		bestP.setVals();
		numberP.setVals();
		genderP.setVals();
		personP.setVals();
		personQP.setVals();
		animacyP.setVals();
		positionP.setVals();
		miP.setVals();
		
		bestP.printParameter("bestP");
		numberP.printParameter("numberP");
		genderP.printParameter("genderP");
		personP.printParameter("personP");
		personQP.printParameter("personQP");
		animacyP.printParameter("animacyP");
		positionP.printParameter("positionP");
		miP.printParameter("miP");
		
		ObjectOutputStream modelOut = new ObjectOutputStream(
				new FileOutputStream("superProbModel"));
		modelOut.writeObject(numberP);
		modelOut.writeObject(genderP);
		modelOut.writeObject(animacyP);
		modelOut.writeObject(personP);
		modelOut.writeObject(personQP);

		modelOut.writeObject(positionP);
		modelOut.writeObject(miP);
		modelOut.writeObject(bestP);

		modelOut.close();
	}

}

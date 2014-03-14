package em;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
	static ArrayList<String> yasmetAZP = new ArrayList<String>();

	static ArrayList<String> yasmetCR = new ArrayList<String>();
	static ArrayList<String> yasmetAZPCR = new ArrayList<String>();

	
	static int maxAnts = 0;

	static ArrayList<String> svmRanks = new ArrayList<String>();
	static ArrayList<String> svmRanksAZP = new ArrayList<String>();
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
					if (leftAncestors.get(i) == rightAncestors.get(i)) {
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

		HashMap<String, Integer> goldMentionToClusterIDMap = new HashMap<String, Integer>();
		for (int i = 0; i < part.getChains().size(); i++) {
			Entity e = part.getChains().get(i);
			for (Mention m : e.mentions) {
				goldMentionToClusterIDMap.put(m.toName(), i);
			}
		}

		HashMap<String, ArrayList<Mention>> clusterMap = new HashMap<String, ArrayList<Mention>>();
		for (int i = 0; i < allMentions.size(); i++) {
			Mention m = allMentions.get(i);
			if (m.end == -1) {
				continue;
			}
			ArrayList<Mention> ms = new ArrayList<Mention>();
			clusterMap.put(m.toName(), ms);
			if (goldMentionToClusterIDMap.containsKey(m.toName())) {
				int clusterID = goldMentionToClusterIDMap.get(m.toName());
				for (int j = 0; j < allMentions.size(); j++) {
					Mention m2 = allMentions.get(j);
					if (m2.end == -1) {
						continue;
					}
					if (goldMentionToClusterIDMap.containsKey(m2.toName())
							&& goldMentionToClusterIDMap.get(m2.toName()).intValue() == clusterID) {
						ms.add(m2);
					}
				}
			} else {
				ms.add(m);
			}
		}

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
			if (m.isAZP
					|| (m.gram == EMUtil.Grammatic.subject && EMUtil.pronouns
							.contains(m.extent))) {
				qid++;
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

				StringBuilder mrYSB = new StringBuilder();
				StringBuilder crYSB = new StringBuilder();
				if (ants.size() > maxAnts) {
					maxAnts = ants.size();
				}

				boolean findFirstSubj = false;
				boolean findCoref = false;

				HashSet<Integer> processedClusters = new HashSet<Integer>();
				int clusterID = 0;
				for (int k = 0; k < ants.size(); k++) {

					Mention ant = ants.get(k);

					// System.out.println(ant.entity.entityIdx + " # " +
					// ant.entity.mentions.size());
					boolean fs = false;
					if (!findFirstSubj && ant.gram == EMUtil.Grammatic.subject) {
						findFirstSubj = true;
						fs = true;
					}
					
					Context context = Context.buildContext(ant, m, part, fs);
					superFea.configure("", "", "", "", context, ant, m, part);
					String fea = superFea.getSVMFormatString();
					
					boolean coref = isCoref(chainMap, m, ant);
					int rank = -1;
					if (coref) {
						if (!findCoref) {
							mrYSB.insert(0, k + " @ ");
							findCoref = true;
						}
						rank = 1;
					}
					mrYSB.append(getYamset(coref, fea, corefCount));
					if (corefCount > 0) {
						if (m.isAZP) {
							svmRanksAZP.add(getSVMRank(rank, fea));
						} else {
							svmRanks.add(getSVMRank(rank, fea));
						}
					}
					
//					if(goldMentionToClusterIDMap.containsKey(ant.toName()) && 
//							processedClusters.contains(goldMentionToClusterIDMap.get(ant.toName()))) {
//					} else {
//						if(coref) {
//							crYSB.insert(0, clusterID + " @ ");
//						}
//						ArrayList<Mention> wholeCluster = clusterMap.get(ant
//								.toName());
//						ArrayList<Mention> cluster = new ArrayList<Mention>();
//						for (Mention a : wholeCluster) {
//							cluster.add(a);
//							if (a.toName().equals(ant.toName())) {
//								break;
//							}
//						}
//						HashMap<Integer, Integer> feaMap = new HashMap<Integer, Integer>();
//						for(int c=0;c<cluster.size();c++) {
//							Mention cant = cluster.get(c);
//							if (m.s.getSentenceIdx() - cant.s.getSentenceIdx() > 2) {
//								cant.isBest = false;
//							}
//							context = Context.buildContext(cant, m, part, cant==ant?fs:false);
//							superFea.configure("", "", "", "", context, cant, m, part);
//							fea = superFea.getSVMFormatString();
//							
//							String tks[] = fea.split("\\s+");
//							for(String tk : tks) {
//								int comma = tk.indexOf(":");
//								int feaIdx = Integer.parseInt(tk.substring(0, comma));
//								if(feaMap.containsKey(feaIdx)) {
//									feaMap.put(feaIdx, feaMap.get(feaIdx).intValue() + 1);
//								} else {
//									feaMap.put(feaIdx, 1);
//								}
//							}
//						}
//						ArrayList<Integer> feaIdxes = new ArrayList<Integer>(feaMap.keySet());
//						Collections.sort(feaIdxes);
//						StringBuilder crunitSb = new StringBuilder();
//						for(int feaIdx : feaIdxes) {
//							int amount = feaMap.get(feaIdx);
//							int newFea = feaIdx * 3;
//							if(amount==cluster.size()) {
//								newFea += 0;
//							} else if(amount>=cluster.size()/2) {
//								newFea += 1;
//							} else if(amount>0) {
//								newFea += 2;
//							}
//							crunitSb.append(newFea).append(":1 ");
//						}
//						crYSB.append(getYamset(coref, crunitSb.toString().trim(), 1));
//						if(goldMentionToClusterIDMap.containsKey(ant.toName())) {
//							processedClusters.add(goldMentionToClusterIDMap.get(ant.toName()));
//						}
//						clusterID++;
//					}
				}
				for (int k = ants.size(); k < 100; k++) {
					mrYSB.append("@ 0 NOCLASS 1 # ");
				}
				for(int k=clusterID;k<100;k++) {
					crYSB.append("@ 0 NOCLASS 1 # ");
				}
				if (corefCount > 0) {
					if (m.isAZP) {
						yasmetAZP.add(mrYSB.toString().trim());
						yasmetAZPCR.add(crYSB.toString().trim());
					} else {
						yasmet.add(mrYSB.toString().trim());
						yasmetCR.add(crYSB.toString().trim());
					}
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

	public static String getSVMRank(int rank, String svm) {
//		String pronoun = pro.extent;
//		String pStr = "";
//		if (sameSpeaker) {
//			pStr = entry.person.name() + "=" + pro.person.name();
//		} else {
//			pStr = entry.person.name() + "!=" + pro.person.name();
//		}
//		String nStr = entry.number.name() + "=" + pro.number.name();
//		String gStr = entry.gender.name() + "=" + pro.gender.name();
//		String aStr = entry.animacy.name() + "=" + pro.animacy.name();

//		superFea.configure(pStr, nStr, gStr, aStr, context, ant, pro, part);
//		String svm = superFea.getSVMFormatString();
		String label = Integer.toString(rank);
		return label + " qid:" + qid + " " + svm;
		// return ysb.toString();
	}

	public static String getYamset(boolean coref, String fea,
			double corefCount) {
		// String pStr = "";
		// if (sameSpeaker) {
		// pStr = entry.person.name() + "=" + pro.person.name();
		// } else {
		// pStr = entry.person.name() + "!=" + pro.person.name();
		// }
		// String nStr = entry.number.name() + "=" + pro.number.name();
		// String gStr = entry.gender.name() + "=" + pro.gender.name();
		// String aStr = entry.animacy.name() + "=" + pro.animacy.name();
		StringBuilder ysb = new StringBuilder();
		String tks[] = fea.split("\\s+");
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
			// System.out.println(line);
			CoNLLDocument d = new CoNLLDocument(line);

			OntoCorefXMLReader.addGoldZeroPronouns(d, false);

			for (CoNLLPart part : d.getParts()) {
				extractGroups(part);
			}
		}
		// CoNLLDocument d = new CoNLLDocument("train_gold_conll");
		// parts.addAll(d.getParts());
		// int i = d.getParts().size();
		// for (CoNLLPart part : parts) {
		// extractGroups(part);
		// // System.out.println(i--);
		// }
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
		if (maxAnts > 100) {
			Common.pause("MaxAnts:" + maxAnts);
		}
		yasmet.add(0, "100");
		Common.outputLines(yasmet, "yasmet.train");
		yasmetAZP.add(0, "100");
		Common.outputLines(yasmetAZP, "yasmetAZP.train");
		
		yasmetCR.add(0, "100");
		Common.outputLines(yasmetCR, "yasmetCR.train");
		yasmetAZPCR.add(0, "100");
		Common.outputLines(yasmetAZPCR, "yasmetAZPCR.train");

		System.out.println(":" + match / all);

		Common.outputLines(svmRanks, "svmRank.train");
		Common.outputLines(svmRanksAZP, "svmRankAZP.train");

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

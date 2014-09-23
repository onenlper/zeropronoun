package em;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import align.DocumentMap;
import align.DocumentMap.SentForAlign;
import align.DocumentMap.Unit;

import mentionDetect.ParseTreeMention;
import model.Entity;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.CoNLL.OntoCorefXMLReader;
import model.syntaxTree.MyTreeNode;
import util.Common;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Distribution;
import em.EMUtil.Animacy;
import em.EMUtil.Gender;
import em.EMUtil.Grammatic;
import em.EMUtil.Number;
import em.EMUtil.Person;
import em.ResolveGroup.Entry;

public class ApplyMaxEnt10Eng {

	String folder;

	Parameter numberP;
	Parameter genderP;
	Parameter animacyP;
	Parameter personP;
	Parameter personQP;

	double contextOverall;

	HashMap<String, Double> contextPrior;

	int overallGuessPronoun;

	static int mode;

	static final int classify = 0;
	static final int load = 1;
	static final int prepare = 2;

	static HashMap<String, ArrayList<SentForAlign[]>> alignMap = new HashMap<String, ArrayList<SentForAlign[]>>();
	static HashMap<String, ArrayList<CoNLLSentence>> engSMap = new HashMap<String, ArrayList<CoNLLSentence>>();

	HashMap<Short, Double> pronounPrior;
	HashMap<Integer, HashMap<Short, Integer>> counts;
	HashMap<Integer, Integer> denomCounts;
	HashMap<Integer, HashSet<Integer>> subSpace;

	HashMap<String, Double> fracContextCount;

	GuessPronounFea guessFea;

	LinearClassifier<String, String> classifier;

	SuperviseFea superFea;
	SuperviseFea superFeaZ;

	EngSuperFea engSuperFea;

	static int zpID;

	static ArrayList<String> numberTest = new ArrayList<String>();
	static ArrayList<String> genderTest = new ArrayList<String>();
	static ArrayList<String> personTest = new ArrayList<String>();
	static ArrayList<String> animacyTest = new ArrayList<String>();

	static HashMap<String, ArrayList<String>> anteTest = new HashMap<String, ArrayList<String>>();

	static ArrayList<String> numberRS;
	static ArrayList<String> genderRS;
	static ArrayList<String> personRS;
	static ArrayList<String> animacyRS;
	static HashMap<String, ArrayList<String>> anteRS;

	public static ArrayList<String> goods = new ArrayList<String>();
	public static ArrayList<String> bads = new ArrayList<String>();

	double good = 0;
	double bad = 0;

	@SuppressWarnings("unchecked")
	public ApplyMaxEnt10Eng(String folder) {
		this.folder = folder;
		try {
			ObjectInputStream modelInput = new ObjectInputStream(
					new FileInputStream("EMModel"));
			numberP = (Parameter) modelInput.readObject();
			genderP = (Parameter) modelInput.readObject();
			animacyP = (Parameter) modelInput.readObject();
			personP = (Parameter) modelInput.readObject();
			personQP = (Parameter) modelInput.readObject();
			fracContextCount = (HashMap<String, Double>) modelInput
					.readObject();
			contextPrior = (HashMap<String, Double>) modelInput.readObject();

			Context.ss = (HashSet<String>) modelInput.readObject();
			Context.vs = (HashSet<String>) modelInput.readObject();
			// Context.svoStat = (SVOStat)modelInput.readObject();
			modelInput.close();

			// classifier = LinearClassifier
			// .readClassifier("stanfordClassifier.gz");
			// ObjectInputStream modelInput2 = new ObjectInputStream(
			// new FileInputStream("giga2/EMModel"));
			// numberP = (Parameter) modelInput2.readObject();
			// genderP = (Parameter) modelInput2.readObject();
			// animacyP = (Parameter) modelInput2.readObject();
			// personP = (Parameter) modelInput2.readObject();
			// personQP = (Parameter) modelInput2.readObject();
			// fracContextCount = (HashMap<String, Double>) modelInput2
			// .readObject();
			// contextPrior = (HashMap<String, Double>)
			// modelInput2.readObject();

			// modelInput2.close();
			// loadGuessProb();
			guessFea = new GuessPronounFea(false, "guessPronoun");

			superFea = new SuperviseFea(false, "supervise");
			superFeaZ = new SuperviseFea(false, "superviseZ");
			superFeaZ.plusNumberGenderPersonAnimacy = false;
			engSuperFea = new EngSuperFea(false, "superEng");
			EMUtil.loadPredictNE(folder, "dev");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadGuessProb() throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(
				"guessPronoun.train.nb"));
		String line;
		pronounPrior = new HashMap<Short, Double>();
		counts = new HashMap<Integer, HashMap<Short, Integer>>();
		denomCounts = new HashMap<Integer, Integer>();
		subSpace = new HashMap<Integer, HashSet<Integer>>();
		overallGuessPronoun = 0;
		while ((line = reader.readLine()) != null) {
			int k = line.indexOf(' ');
			short label = (short) Integer.parseInt(line.substring(0, k));
			overallGuessPronoun++;

			Double priorPro = pronounPrior.get(label);
			if (priorPro == null) {
				pronounPrior.put(label, 1.0);
			} else {
				pronounPrior.put(label, 1.0 + priorPro.doubleValue());
			}

			String feas[] = line.substring(k + 1).split("#");
			for (int i = 0; i < feas.length; i++) {
				String fea = feas[i];
				String tks[] = fea.trim().split("\\s+");
				for (String tk : tks) {
					int idx = Integer.parseInt(tk);
					HashSet<Integer> subS = subSpace.get(i);
					if (subS == null) {
						subS = new HashSet<Integer>();
						subSpace.put(i, subS);
					}

					HashMap<Short, Integer> count = counts.get(idx);
					if (count == null) {
						count = new HashMap<Short, Integer>();
						counts.put(idx, count);
					}
					Integer c = count.get(label);
					if (c == null) {
						count.put(label, 1);
					} else {
						count.put(label, c.intValue() + 1);
					}

					Integer dc = denomCounts.get(idx);
					if (dc == null) {
						denomCounts.put(idx, 1);
					} else {
						denomCounts.put(idx, 1 + dc.intValue());
					}

				}
			}

		}
	}

	static int sigID = 0;

	public void test() {
		ArrayList<String> files = Common.getLines("chinese_list_" + folder
				+ "_development");

		ArrayList<ArrayList<Mention>> corefResults = new ArrayList<ArrayList<Mention>>();
		ArrayList<ArrayList<Entity>> goldEntities = new ArrayList<ArrayList<Entity>>();
		int pID = 0;

		HashMap<String, int[]> positionsMap = new HashMap<String, int[]>();

		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file.replace(
					"auto_conll", "gold_conll"));
			OntoCorefXMLReader.addGoldZeroPronouns(document, false);

			for (int k = 0; k < document.getParts().size(); k++) {
				pID++;
				// if(pID%5!=sigID) {
				// continue;
				// }
				CoNLLPart part = document.getParts().get(k);

				String folder = part.folder;
				int[] positions = positionsMap.get(folder);
				if (positions == null) {
					positions = new int[2];
					positionsMap.put(folder, positions);
					positions[0] = corefResults.size();
				}
				positions[1] = corefResults.size() + 1;

				ArrayList<Entity> goldChains = part.getChains();

				for (Entity e : goldChains) {
					for (Mention m : e.mentions) {
						StringBuilder s = new StringBuilder();
						if (m.end == -1) {
							continue;
						}
						for (int i = m.start; i <= m.end; i++) {
							s.append(part.getWord(i).word).append(" ");
						}
						m.extent = s.toString().trim();
						m.head = part.getWord(m.end).word;
					}
				}

				goldEntities.add(goldChains);

				HashMap<String, Integer> chainMap = EMUtil
						.formChainMap(goldChains);

				HashMap<String, ArrayList<Mention>> chainMap2 = EMUtil
						.formChainMap2(goldChains);

				ArrayList<Mention> corefResult = new ArrayList<Mention>();
				corefResults.add(corefResult);

				ParseTreeMention ptm = new ParseTreeMention();
				ArrayList<Mention> goldBoundaryNPMentions = ptm
						.getMentions(part);

				Collections.sort(goldBoundaryNPMentions);

				ArrayList<Mention> anaphorZeros = EMUtil.getAnaphorZeros(part
						.getChains());
				// anaphorZeros = zeroDetectTest.detectZeros(part, null);

				ArrayList<Mention> candidates = new ArrayList<Mention>();
				candidates.addAll(goldBoundaryNPMentions);

				if (!file.contains("/nw/") && !file.contains("/mz/")
						&& !file.contains("/bn/") && !file.contains("/mz/")) {
					candidates.addAll(anaphorZeros);
				}
				Collections.sort(candidates);
				Collections.sort(anaphorZeros);

				for (int i = 0; i < part.getCoNLLSentences().size(); i++) {
					CoNLLSentence chiS = part.getCoNLLSentences().get(i);
					int start = chiS.getWord(0).index;
					int end = chiS.getWord(chiS.getWords().size() - 1).index;

					ArrayList<Mention> zerosInS = EMUtil.getInBetweenMention(
							anaphorZeros, start, end);
					ArrayList<ArrayList<CoNLLWord>> wordsArr = EMUtil.split(
							chiS, zerosInS);

					for (int sid = 0; sid < wordsArr.size(); sid++) {
						ArrayList<CoNLLWord> segWords = wordsArr.get(sid);
						int chiSegStart = segWords.get(0).index;
						int chiSegEnd = segWords.get(segWords.size() - 1).index;
						ArrayList<Mention> zeros = EMUtil.getInBetweenMention(
								zerosInS, chiSegStart, chiSegEnd);
						ArrayList<Mention> nps = EMUtil.getInBetweenMention(
								candidates, chiSegStart, chiSegEnd);

						// fill all gaps with ta
						for (int z = 0; z < zeros.size(); z++) {
							if (z != 0
									&& zeros.get(z).start == zeros.get(z - 1).start) {
								continue;
							}
							Mention zero = zeros.get(z);
							int w = -1;
							for (; w < segWords.size(); w++) {
								if (w != -1
										&& segWords.get(w).index == zero.start) {
									break;
								}
							}
							CoNLLWord newW = new CoNLLWord();
							newW.isZeroWord = true;
							newW.index = zeros.get(z).start;
							newW.word = EMUtil.pronounList.get(0);
							segWords.add(w, newW);
						}

						for (int z = 0; z < zeros.size(); z++) {
							if (z != 0
									&& zeros.get(z).start == zeros.get(z - 1).start) {
								zeros.get(z).antecedent = zeros.get(z - 1).antecedent;
							} else {
								findAntecedent(file, part, chainMap,
										corefResult, zeros.get(z), candidates,
										chainMap2, segWords);
							}
						}
						alignMentions(chiS, segWords, nps);
					}
				}
			}
		}
		System.out.println("Good: " + good);
		System.out.println("Bad: " + bad);
		System.out.println("Precission: " + good / (good + bad) * 100);

		bad = 0;
		good = 0;
		evaluate(corefResults, goldEntities);

		System.out.println(corefResults.size() + "###");
		System.out.println(goldEntities.size() + "$$$");

		if (this.folder.equals("all")) {
			for (String key : positionsMap.keySet()) {
				int ps[] = positionsMap.get(key);
				int s = ps[0];
				int e = ps[1];
				System.out.println(key + "::::::");
				evaluate(corefResults.subList(s, e), goldEntities.subList(s, e));
			}
		}
	}

	ParseTreeMention ptm = new ParseTreeMention();

	private void alignMentions(CoNLLSentence chiS,
			ArrayList<CoNLLWord> segWords, ArrayList<Mention> chiNPs) {
		int chiSegStart = segWords.get(0).index;
		HashMap<Integer, Integer> offsetMap = new HashMap<Integer, Integer>();
		int offset = 0;
		for (CoNLLWord w : segWords) {
			if (w.isZeroWord) {
				offset++;
			} else {
				offsetMap.put(w.index, offset);
			}
		}

		String chiStr = EMUtil.listToString(segWords);
		SentForAlign[] align = alignMap.get(chiStr).get(0);
		String engStr = align[1].getText();
		CoNLLSentence engCoNLLS = engSMap.get(engStr).get(0);

		// construct mention map between two s
		for (Mention cm : chiNPs) {
			cm.units.clear();
			Mention.chiSpanMaps.remove(cm.getReadName());
		}
		for (Mention em : chiNPs) {
			int from = em.start - chiSegStart + offsetMap.get(em.start);
			int to = from;
			if (em.end != -1) {
				to = em.end - chiSegStart + offsetMap.get(em.end);
			} else {
				from--;
				to = from;
			}
			StringBuilder sb = new StringBuilder();
			for (int g = from; g <= to || g==from; g++) {
				Unit unit = align[0].units.get(g);
				unit.sentence = chiS;
				unit.addMention(em);
				em.units.add(unit);
				sb.append(unit.getToken()).append(" ");
			}
			if (!sb.toString().trim().equalsIgnoreCase(em.extent)) {
				// System.out.println("#" + sb.toString().trim()
				// + "#" + em.extent.trim() + "#");
				// System.out.println(em.start + "," + em.end);
				// Common.pause("");
			} else if (em.end == -1) {
				// System.out.println("#" + sb.toString().trim()
				// + "#" + em.extent.trim() + "#");
				// System.out.println(em.start + "," + em.end);
				// Common.pause("");
			}
		}
		ArrayList<Mention> engMentions = ptm.getMentions(engCoNLLS);
		int engStart = engCoNLLS.getWords().get(0).index;
		for (Mention em : engMentions) {
			StringBuilder sb = new StringBuilder();
			for (int g = em.start - engStart; g <= em.end - engStart; g++) {
				Unit unit = align[1].units.get(g);
				unit.sentence = engCoNLLS;
				unit.addMention(em);
				em.units.add(unit);
				sb.append(unit.getToken()).append(" ");
			}
			if (!em.extent.trim().equalsIgnoreCase(sb.toString().trim())) {
				System.out.println("#" + sb.toString().trim() + "#"
						+ em.extent.trim() + "#");
				Common.bangErrorPOS("");
			}
		}

		for (int g = 1; g <= 4; g++) {
			Mention.assignMode = g;
			// for (Mention m : engMentions) {
			// m.getXSpan();
			// }
			for (Mention m : chiNPs) {
				m.getXSpan();
			}
		}

		for (Mention m : chiNPs) {
			Mention xm = m.getXSpan();
			// if (xm != null
			// && xm.extent.isEmpty()
			// ) {
			// System.out.println(m.extent + "#" + xm.extent);
			// System.out.println(m.start + "," + m.end + "#" + xm.start + "," +
			// xm.end);
			// System.out.println(EMUtil.listToString(words));
			// System.out.println(xm.s.getText());
			// System.out.println(m.xSpanType);
			// Common.pause("GOOD!!!");
			// }
			if (xm != null && m.end != -1) {
				// System.out.println(m.extent + "#" + xm.extent);
				// System.out.println(m.start + "," + m.end + "#" + xm.start +
				// "," + xm.end);
				// Common.pause("");
			}

			if (xm != null) {
				alignn++;
			}
			all++;
		}
	}

	static double all = 0;
	static double alignn = 0;

	private void setParas(CoNLLPart part) {
		if (part.folder.equalsIgnoreCase("BN")) {
			alpha = 0.3;
			beta = 0;
			theta = 0;
			delta = 0.1;
		} else if (part.folder.equalsIgnoreCase("TC")) {
			alpha = 0.2;
			beta = 0;
			theta = 0;
			delta = 0.3;
		} else if (part.folder.equalsIgnoreCase("NW")) {
			alpha = 0;
			beta = 0.1;
			theta = 0;
			delta = 0;
		} else if (part.folder.equalsIgnoreCase("BC")) {
			alpha = 0.4;
			beta = 0;
			theta = 0;
			delta = 0;
		} else if (part.folder.equalsIgnoreCase("WB")) {
			alpha = 0.45;
			beta = 0;
			theta = 0.15;
			delta = 0;
		} else if (part.folder.equalsIgnoreCase("MZ")) {
			alpha = 0.8;
			beta = 0;
			theta = 0.4;
			delta = 0;
		} else {
			Common.bangErrorPOS("Wrong Folder!!!" + part.folder);
		}
	}

	private void setParas2(CoNLLPart part) {
		if (part.folder.equalsIgnoreCase("BN")) {
			p1 = 0;
			p2 = 0;
			p3 = 0;
			p4 = 0;
		} else if (part.folder.equalsIgnoreCase("TC")) {
			p1 = 0;
			p2 = 0.8;
			p3 = 0;
			p4 = 0;
		} else if (part.folder.equalsIgnoreCase("NW")) {
			p1 = 0;
			p2 = 0;
			p3 = 0;
			p4 = 0;
		} else if (part.folder.equalsIgnoreCase("BC")) {
			p1 = 1.2;
			p2 = 0;
			p3 = 0.1;
			p4 = 0.1;
		} else if (part.folder.equalsIgnoreCase("WB")) {
			p1 = 1.9;
			p2 = 0.4;
			p3 = 0;
			p4 = 0;
		} else if (part.folder.equalsIgnoreCase("MZ")) {
			p1 = 0;
			p2 = 0;
			p3 = 0.6;
			p4 = 0;
		} else {
			Common.bangErrorPOS("Wrong Folder!!!" + part.folder);
		}
	}

	private void inferGoldAttri(Mention zero, CoNLLPart part,
			HashMap<String, ArrayList<Mention>> chainMap2) {
		ArrayList<Mention> cluster = new ArrayList<Mention>(chainMap2.get(zero
				.toName()));
		Person proPer = null;
		Number proNum = null;
		Gender proGen = null;
		Animacy proAni = null;
		proPer = EMUtil.inferPerson(cluster, zero, part);
		proNum = EMUtil.inferNumber(cluster);
		proGen = EMUtil.inferGender(cluster);
		proAni = EMUtil.inferAnimacy(cluster);
		StringBuilder sb = new StringBuilder();
		sb.append("Chains: ");
		for (Mention m : cluster) {
			if (m.end == -1) {
				continue;
			}
			sb.append(m.extent).append(" # ");
		}
		sb.append("\nInfer: ");
	}

	private void findAntecedent(String file, CoNLLPart part,
			HashMap<String, Integer> chainMap, ArrayList<Mention> corefResult,
			Mention zero, ArrayList<Mention> allCandidates,
			HashMap<String, ArrayList<Mention>> chainMap2,
			ArrayList<CoNLLWord> segChiWords) {
		// for (Mention zero : anaphorZeros) {
		zero.sentenceID = part.getWord(zero.start).sentence.getSentenceIdx();
		zero.s = part.getWord(zero.start).sentence;
		EMUtil.assignVNode(zero, part);

		Mention antecedent = null;
		Collections.sort(allCandidates);

		ArrayList<Mention> cands = new ArrayList<Mention>();
		boolean findFS = false;

		for (int h = allCandidates.size() - 1; h >= 0; h--) {
			Mention cand = allCandidates.get(h);
			cand.sentenceID = part.getWord(cand.start).sentence
					.getSentenceIdx();
			cand.s = part.getWord(cand.start).sentence;
			cand.isFS = false;
			cand.isBest = false;
			cand.MI = Context.calMI(cand, zero);
			if (cand.start < zero.start
					&& zero.sentenceID - cand.sentenceID <= 2) {
				if (!findFS && cand.gram == EMUtil.Grammatic.subject) {
					cand.isFS = true;
					findFS = true;
				}
				cands.add(cand);
			}
		}
		findBest(zero, cands);

		guessFea.configure(zero.start - 1, zero.start,
				part.getWord(zero.start).sentence, part);

		// label = pronoun.animacy.ordinal() + 1;
		String pro = EMUtil.pronounList.get(0);

		String feaStr = guessFea.getSVMFormatString();
		String v = EMUtil.getFirstVerb(zero.V);
		// Yasmet format
		// NUMBER, GENDER, PERSON, ANIMACY
		String tks[] = feaStr.split("\\s+");

		int all = EMUtil.Person.values().length;
		double[] probPer1 = ApplyMaxEnt10Eng
				.selectRestriction("person", all, v);

		String pYSB = transform(tks, all, 0, probPer1);
		double probPer2[] = runAttri("person", pYSB, all, v);

		all = EMUtil.Number.values().length;
		double[] probNum1 = ApplyMaxEnt10Eng
				.selectRestriction("number", all, v);
		String nYSB = transform(tks, all, 0, probNum1);
		double probNum2[] = runAttri("number", nYSB, all, v);

		all = EMUtil.Gender.values().length - 1;
		double[] probGen1 = ApplyMaxEnt10Eng
				.selectRestriction("gender", all, v);
		String gYSB = transform(tks, all, 0, probGen1);
		double probGen2[] = runAttri("gender", gYSB, all, v);

		all = EMUtil.Animacy.values().length - 1;
		double[] probAni1 = ApplyMaxEnt10Eng.selectRestriction("animacy", all,
				v);
		String aYSB = transform(tks, all, 0, probAni1);
		double probAni2[] = runAttri("animacy", aYSB, all, v);
		// TODO
		ArrayList<double[]> opProbs = getProb_C(cands, zero, part, superFeaZ,
				"WTZ");
		setParas(part);
		setParas2(part);
		tuneOpProbs(opProbs, probPer1, probNum1, probGen1, probAni1);
		tuneOpProbs2(opProbs, probPer2, probNum2, probGen2, probAni2);
		pro = EMUtil.decideOP(opProbs.get(0), opProbs.get(1), opProbs.get(2),
				opProbs.get(3));

		// TODO Warning
		antecedent = givenOPFindC(cands, zero, part, pro, superFea, "WT",
				segChiWords, chainMap);
		zpID++;

		// boolean coref = false;
		// if (antecedent != null) {
		// coref = chainMap.containsKey(zero.toName())
		// && chainMap.containsKey(antecedent.toName())
		// && chainMap.get(zero.toName()).intValue() == chainMap
		// .get(antecedent.toName()).intValue();
		// }
		// antecedent = null;
		// antecedent = doWorkFill10Times(cands, zero, part, chainMap,
		// superFea, "WT", coref, sb.toString().trim());

		if (antecedent != null) {
			if (antecedent.end != -1) {
				zero.antecedent = antecedent;
			} else {
				zero.antecedent = antecedent.antecedent;
			}
			zero.extent = antecedent.extent;
			zero.head = antecedent.head;
			zero.gram = Grammatic.subject;
			zero.mType = antecedent.mType;
			zero.NE = antecedent.NE;
			this.addEmptyCategoryNode(zero);
			// System.out.println(zero.start);
			// System.out.println(antecedent.extent);
		}
		if (zero.antecedent != null
				&& zero.antecedent.end != -1
				&& chainMap.containsKey(zero.toName())
				&& chainMap.containsKey(zero.antecedent.toName())
				&& chainMap.get(zero.toName()).intValue() == chainMap.get(
						zero.antecedent.toName()).intValue()) {
			good++;
			// if(antecedent.mType==MentionType.tmporal) {
			// System.out.println(antecedent.extent + "GOOD!");
			// }
			// System.out.println(overtPro + "  " + zero.antecedent.extent);
			// System.out.println("+++");
			// printResult(zero, zero.antecedent, part);
			// System.out.println("Predicate: " +
			// this.getPredicate(zero.V));
			// System.out.println("Object NP: " +
			// this.getObjectNP(zero));
			// System.out.println("===");
			// if (zero.antecedent.MI < 0) {
			// System.out.println("Right!!! " + good + "/" + bad);
			// System.out.println(zero.antecedent.msg);
			// }
		} else {
			// if(antecedent!=null && antecedent.mType==MentionType.tmporal)
			// {
			// System.out.println(antecedent.extent + "BAD !");
			// }
			bad++;
			System.out.println("Error??? " + good + "/" + bad);
			if (zero.antecedent != null) {
				System.out.println(zero.antecedent.msg);
			}
		}
		String conllPath = file;
		int aa = conllPath.indexOf(anno);
		int bb = conllPath.indexOf(".");
		String middle = conllPath.substring(aa + anno.length(), bb);
		String path = prefix + middle + suffix;
		System.out.println(path);
		// System.out.println("=== " + file);
		EMUtil.addEmptyCategoryNode(zero);

		// if (antecedent != null) {
		// CoNLLWord candWord = part.getWord(antecedent.start);
		// CoNLLWord zeroWord = part.getWord(zero.start);
		//
		// String zeroSpeaker = part.getWord(zero.start).speaker;
		// String candSpeaker = part.getWord(antecedent.start).speaker;
		// // if (!zeroSpeaker.equals(candSpeaker)) {
		// // if (antecedent.source.equals("我") &&
		// // zeroWord.toSpeaker.contains(candSpeaker)) {
		// // zero.head = "你";
		// // zero.source = "你";
		// // } else if (antecedent.source.equals("你") &&
		// // candWord.toSpeaker.contains(zeroSpeaker)) {
		// // zero.head = "我";
		// // zero.source = "我";
		// // }
		// // } else {
		// zero.extent = antecedent.extent;
		// zero.head = antecedent.head;
		// // }
		//
		// }
		// }
		// for (Mention zero : anaphorZeros) {
		if (zero.antecedent != null) {
			corefResult.add(zero);
		}
		// }
	}

	static double alpha = 0;
	static double beta = 0;
	static double theta = 0;
	static double delta = 0;

	static double p1 = 0;
	static double p2 = 0;
	static double p3 = 0;
	static double p4 = 0;

	private void tuneOpProbs2(ArrayList<double[]> opProbs, double[] probPer,
			double[] probNum, double[] probGen, double[] probAni) {
		double[] arr1 = opProbs.get(0);
		for (int i = 0; i < arr1.length && i < probPer.length; i++) {
			arr1[i] += probPer[i] * p1;
		}
		double[] arr2 = opProbs.get(1);
		for (int i = 0; i < arr2.length && i < probNum.length; i++) {
			arr2[i] += probNum[i] * p2;
		}
		double[] arr3 = opProbs.get(2);
		for (int i = 0; i < arr3.length && i < probGen.length; i++) {
			arr3[i] += probGen[i] * p3;
		}
		double[] arr4 = opProbs.get(3);
		for (int i = 0; i < arr4.length && i < probAni.length; i++) {
			arr4[i] += probAni[i] * p4;
		}

		// normalize(opProbs.get(0));
		// normalize(opProbs.get(1));
		// normalize(opProbs.get(2));
		// normalize(opProbs.get(3));
	}

	private void tuneOpProbs(ArrayList<double[]> opProbs, double[] probPer,
			double[] probNum, double[] probGen, double[] probAni) {
		double[] arr1 = opProbs.get(0);
		for (int i = 0; i < arr1.length && i < probPer.length; i++) {
			arr1[i] += probPer[i] * alpha;
		}
		double[] arr2 = opProbs.get(1);
		for (int i = 0; i < arr2.length && i < probNum.length; i++) {
			arr2[i] += probNum[i] * beta;
		}
		double[] arr3 = opProbs.get(2);
		for (int i = 0; i < arr3.length && i < probGen.length; i++) {
			arr3[i] += probGen[i] * theta;
		}
		double[] arr4 = opProbs.get(3);
		for (int i = 0; i < arr4.length && i < probAni.length; i++) {
			arr4[i] += probAni[i] * delta;
		}

		// normalize(opProbs.get(0));
		// normalize(opProbs.get(1));
		// normalize(opProbs.get(2));
		// normalize(opProbs.get(3));
	}

	private Mention doWorkFill10Times(ArrayList<Mention> cands, Mention zero,
			CoNLLPart part, HashMap<String, Integer> chainMap,
			SuperviseFea superFea, String model, boolean upboundCorrect,
			String message) {
		int antCount = 0;
		String proSpeaker = part.getWord(zero.start).speaker;
		HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();
		StringBuilder ysb = new StringBuilder();
		ysb.append("0 @ ");
		Mention antecedent = null;
		HashSet<Integer> corefs = new HashSet<Integer>();
		for (int p = 0; p < EMUtil.pronounList.size(); p++) {
			zero.extent = EMUtil.pronounList.get(p);
			for (int i = 0; i < cands.size(); i++) {
				Mention cand = cands.get(i);
				if (cand.extent.isEmpty()) {
					continue;
				}

				boolean coref = chainMap.containsKey(zero.toName())
						&& chainMap.containsKey(cand.toName())
						&& chainMap.get(zero.toName()).intValue() == chainMap
								.get(cand.toName()).intValue();
				if (coref) {
					corefs.add(antCount);
				}
				String antSpeaker = part.getWord(cand.start).speaker;
				cand.sentenceID = part.getWord(cand.start).sentence
						.getSentenceIdx();

				Context context = Context.buildContext(cand, zero, part,
						cand.isFS);
				cand.msg = Context.message;
				cand.MI = Context.MI;

				boolean sameSpeaker = proSpeaker.equals(antSpeaker);
				Entry entry = new Entry(cand, context, sameSpeaker, cand.isFS);

				String unit = MaxEntLearn.getYamset(false, cand, zero, context,
						sameSpeaker, entry, superFea, 1, part);

				ysb.append(unit);

				idMap.put(antCount, i);
				antCount++;
			}
		}

		double probAnt[] = runYasmet(ysb.toString(), antCount, model);
		// System.err.println(cands.size());
		if (antCount != 0 && (mode == classify || mode == load)) {
			int antIdx = -1;
			double maxProb = -1;
			int antNumber = probAnt.length / EMUtil.pronounList.size();
			for (int i = 0; i < probAnt.length; i++) {
				if (probAnt[i] > maxProb) {
					maxProb = probAnt[i];
					antIdx = i;
				}
			}
			boolean find = false;
			for (int p = 0; p < EMUtil.pronounList.size(); p++) {
				double localMax = 0;
				int localBest = -1;
				for (int g = 0; g < antNumber; g++) {
					int id = p * antNumber + g;
					double pr = probAnt[id];
					if (pr > localMax) {
						localMax = pr;
						localBest = id;
					}
				}
				if (corefs.contains(localBest)) {
					antecedent = cands.get(idMap.get(localBest));
					String pro = EMUtil.pronounList.get(p);

					if (!upboundCorrect) {
						System.out.println(message);
						System.out.println("Good Pronoun: " + pro + "\t"
								+ EMUtil.getNumber(pro) + "\t"
								+ EMUtil.getGender(pro) + "\t"
								+ EMUtil.getPerson(pro) + "\t"
								+ EMUtil.getAnimacy(pro));
						Common.pause("");
					}
					find = true;
				}
			}
			if (find) {
				return antecedent;
			}
			if (antIdx != -1) {
				antecedent = cands.get(idMap.get(antIdx));
			}
		}
		return antecedent;
	}

	private Mention givenOPFindC(ArrayList<Mention> cands, Mention zero,
			CoNLLPart part, String givenOP, SuperviseFea superFea,
			String model, ArrayList<CoNLLWord> segChiWords, HashMap<String, Integer> chainMap) {
		int antCount = 0;
		String proSpeaker = part.getWord(zero.start).speaker;
		HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();
		StringBuilder ysb = new StringBuilder();
		ysb.append("0 @ ");
		Mention antecedent = null;

		for (int p = 0; p < EMUtil.pronounList.size(); p++) {
			String op = EMUtil.pronounList.get(p);
			zero.extent = op;
			zero.head = op;

			for (int i = 0; i < cands.size(); i++) {
				Mention cand = cands.get(i);
				if (cand.extent.isEmpty()) {
					continue;
				}
				// if(!cand.isFS)
				// continue;
				String antSpeaker = part.getWord(cand.start).speaker;
				cand.sentenceID = part.getWord(cand.start).sentence
						.getSentenceIdx();

				Context context = Context.buildContext(cand, zero, part,
						cand.isFS);
				cand.msg = Context.message;
				cand.MI = Context.MI;

				boolean sameSpeaker = proSpeaker.equals(antSpeaker);
				Entry entry = new Entry(cand, context, sameSpeaker, cand.isFS);

				String unit = getYamset(false, cand, zero, context,
						sameSpeaker, entry, superFea, 1, part,
						EMUtil.getPerson(op), EMUtil.getNumber(op),
						EMUtil.getGender(op), EMUtil.getAnimacy(op));

				ysb.append(unit);

				idMap.put(antCount, i);
				antCount++;
			}
		}
		double probAnt[] = runYasmet(ysb.toString(), antCount, model);
		// run english
		double prob2[] = runEnglish(cands, zero, givenOP, segChiWords);
		
		double prob1[] = new double[cands.size()];
		// System.err.println(cands.size());
		if (antCount != 0 && (mode == classify || mode == load)) {
			int antIdx = -1;
			double maxProb = -1;

			int numOfAnt = probAnt.length / EMUtil.pronounList.size();

			int proIdx = EMUtil.pronounList.indexOf(givenOP);
			for (int i = 0; i < probAnt.length; i++) {
				if (i < numOfAnt * proIdx || i >= numOfAnt * (proIdx + 1)) {
					continue;
				}
				if (probAnt[i] > maxProb) {
					maxProb = probAnt[i];
					antIdx = i;
				}
				prob1[idMap.get(i)] = probAnt[i];
			}
			
			if(prob2[idMap.get(antIdx)]!=0) {
				for(int i=0;i<cands.size();i++) {
					if(prob1[i]==0) {
						prob2[i] = 0;
					}
					if(prob2[i]==0) {
						prob1[i] = 0;
					}
				}
				normalize(prob1);
				normalize(prob2);
				maxProb = 0;
				int candID = 0;
				for(int i=0;i<cands.size();i++) {
					double p = prob1[i] + pp * prob2[i];
					if(p>maxProb) {
						maxProb = p;
						candID = i;
					}
				}
				antecedent = cands.get(candID);
			} else {
				if (antIdx != -1) {
					antecedent = cands.get(idMap.get(antIdx));
				}
			}
		}
		return antecedent;
	}
	static int buhao = 0;
	static int great = 0;
	
	static double pp = 0.1;

	private double[] runEnglish(ArrayList<Mention> cands, Mention zero,
			String givenOP, ArrayList<CoNLLWord> segChiWords) {
		
		double prob2[] = new double[cands.size()];
		
		HashMap<Integer, Integer> idMap;
		Mention antecedent;
		int chiSegStart = segChiWords.get(0).index;
		int chiSegEnd = segChiWords.get(segChiWords.size() - 1).index;
		ArrayList<Mention> nps = EMUtil.getInBetweenMention(cands, chiSegStart,
				chiSegEnd);
		nps.add(zero);
		int w = -1;
		for (; w < segChiWords.size(); w++) {
			if (w != -1 && segChiWords.get(w).index == zero.start) {
				break;
			}
		}
		segChiWords.get(w).word = givenOP;
		alignMentions(zero.s, segChiWords, nps);
		
		Mention xZero = zero.getXSpan();
		if (xZero != null) {
			System.out.println(givenOP + "#####" + zero.getXSpan().extent);
			StringBuilder xYSB = new StringBuilder();
			xYSB.append("0 @ ");
			int xAntCount = 0;
			idMap = new HashMap<Integer, Integer>();
			for (int k = 0; k < cands.size(); k++) {
				Mention ant = cands.get(k);
				Mention xAnt = ant.getXSpan();
				if (xAnt == null) {
					continue;
				}
				xYSB.append(TrainEngPronoun.getYamset(false, xAnt, xZero,
						engSuperFea, 1, engPart, zero.s.getSentenceIdx()
								- ant.s.getSentenceIdx(), zero.start - ant.end));
				idMap.put(xAntCount, k);
				xAntCount++;
			}
			double probAntEng[] = runYasmet(xYSB.toString(), xAntCount, "WTEng");
			int antIdx = -1;
			double maxProb = 0;
			for (int i = 0; i < probAntEng.length; i++) {
				if (probAntEng[i] > maxProb) {
					maxProb = probAntEng[i];
					antIdx = i;
				}
				prob2[idMap.get(i)] = probAntEng[i];
			}
			if (antIdx != -1) {
				antecedent = cands.get(idMap.get(antIdx));
				if(antecedent.getXSpan()==null) {
					Common.pause("");
				}
			} else {
				antecedent = null;
			}
			System.out.println("Percent: " + xAntCount*1.0/(cands.size()*1.0));
		} else {
			antecedent = null;
		}
		String msg = "null";
		if(antecedent!=null) {
			msg = antecedent.extent + " " + antecedent.start + "," + antecedent.end +"###" + antecedent.getXSpan().extent;
		}
		System.out.println("Pick::::: " + msg);
		System.out.println("--------------------");
		return prob2;
	}

	private ArrayList<double[]> getProb_C(ArrayList<Mention> cands,
			Mention zero, CoNLLPart part, SuperviseFea superFea, String model) {
		int antCount = 0;
		String proSpeaker = part.getWord(zero.start).speaker;
		HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();
		StringBuilder ysb = new StringBuilder();
		ysb.append("0 @ ");
		for (int i = 0; i < cands.size(); i++) {
			Mention cand = cands.get(i);
			if (cand.extent.isEmpty()) {
				continue;
			}
			// if(!cand.isFS)
			// continue;
			String antSpeaker = part.getWord(cand.start).speaker;
			cand.sentenceID = part.getWord(cand.start).sentence
					.getSentenceIdx();

			Context context = Context.buildContext(cand, zero, part, cand.isFS);
			cand.msg = Context.message;
			cand.MI = Context.MI;

			boolean sameSpeaker = proSpeaker.equals(antSpeaker);
			Entry entry = new Entry(cand, context, sameSpeaker, cand.isFS);

			String unit = getYamset(false, cand, zero, context, sameSpeaker,
					entry, superFea, 1, part, null, null, null, null);

			ysb.append(unit);

			idMap.put(antCount, i);
			antCount++;
		}

		double probAnt[] = runYasmet(ysb.toString(), antCount, model);

		// System.err.println(cands.size());
		int id = 0;
		double[] anis = new double[Animacy.values().length];
		double[] pers = new double[Person.values().length];
		double[] gens = new double[Gender.values().length];
		double[] nums = new double[Number.values().length];
		for (int c = 0; c < cands.size(); c++) {
			Mention ant = cands.get(c);
			if (ant.extent.isEmpty()) {
				continue;
			}

			Animacy animacy = EMUtil.getAntAnimacy(ant);

			String antSpeaker = part.getWord(ant.start).speaker;
			boolean sameSpeaker = proSpeaker.equals(antSpeaker);

			Person person = EMUtil.getAntPerson(ant.head);

			person = EMUtil.flipPerson(person, sameSpeaker, ant.extent);

			Gender gender = EMUtil.getAntGender(ant);
			Number number = EMUtil.getAntNumber(ant);
			double prob = probAnt[id++];

			anis[animacy.ordinal()] = Math.max(prob, anis[animacy.ordinal()]);
			pers[person.ordinal()] = Math.max(prob, pers[person.ordinal()]);
			gens[gender.ordinal()] = Math.max(prob, gens[gender.ordinal()]);
			nums[number.ordinal()] = Math.max(prob, nums[number.ordinal()]);
		}
		if (id != probAnt.length) {
			Common.bangErrorPOS("!!!");
		}

		normalize(pers);
		normalize(nums);
		normalize(gens);
		normalize(anis);
		ArrayList<double[]> ret = new ArrayList<double[]>();
		ret.add(pers);
		ret.add(nums);
		ret.add(gens);
		ret.add(anis);
		return ret;
	}

	private static void normalize(double[] vals) {
		double all = 0;
		for (double v : vals) {
			all += v;
		}
		if(all==0) {
			return;
		}
		for (int i = 0; i < vals.length; i++) {
			vals[i] = vals[i] / all;
		}
	}

	public static String getYamset(boolean coref, Mention ant, Mention pro,
			Context context, boolean sameSpeaker, Entry entry,
			SuperviseFea superFea, double corefCount, CoNLLPart part,
			Person proPerson, Number proNumber, Gender proGender, Animacy proAni) {
		// String pronoun = pro.extent;
		String pStr = "";
		String nStr = "";
		String gStr = "";
		String aStr = "";

		if (proPerson != null) {
			if (sameSpeaker) {
				pStr = entry.person.name() + "=" + proPerson.name();
			} else {
				pStr = entry.person.name() + "!=" + proPerson.name();
			}
		}
		if (proNumber != null) {
			nStr = entry.number.name() + "=" + proNumber.name();
		}
		if (proGender != null) {
			gStr = entry.gender.name() + "=" + proGender.name();
		}
		if (proAni != null) {
			aStr = entry.animacy.name() + "=" + proAni.name();
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

	private static String transform(String[] tks, int all, int y,
			double probs[]) {
		StringBuilder ysb = new StringBuilder();
		ysb.append(all + "\n");
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

	private double[] runAttri(String attri, String str, int all, String v) {
		switch (mode) {
		case prepare: {
			ArrayList<String> lines = null;
			if (attri.equals("number")) {
				lines = numberTest;
			} else if (attri.equals("gender")) {
				lines = genderTest;
			} else if (attri.equals("person")) {
				lines = personTest;
			} else if (attri.equals("animacy")) {
				lines = animacyTest;
			}
			String tks[] = str.split("\n");
			if (lines.isEmpty()) {
				lines.add(tks[0]);
				lines.add(tks[1]);
			} else {
				lines.add(tks[1]);
			}
			return new double[all];
		}
		case classify: {
			// String lineStr = "";
			// String cmd =
			// "/users/yzcchen/tool/YASMET/./a.out /users/yzcchen/tool/YASMET/"
			// + attri
			// + ".model";
			// Runtime run = Runtime.getRuntime();
			// try {
			// Process p = run.exec(cmd);
			//
			// BufferedOutputStream out = new BufferedOutputStream(
			// p.getOutputStream());
			// out.write(str.getBytes());
			// out.flush();
			// out.close();
			//
			// BufferedInputStream in = new BufferedInputStream(
			// p.getInputStream());
			// BufferedReader inBr = new BufferedReader(new InputStreamReader(
			// in));
			// lineStr = inBr.readLine();
			// if (p.waitFor() != 0) {
			// if (p.exitValue() == 1) {
			// System.err.println("ERROR YASMET");
			// Common.bangErrorPOS("");
			// }
			// }
			// System.out.println(lineStr);
			// inBr.close();
			// in.close();
			// } catch (IOException e) {
			// e.printStackTrace();
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }
			//
			// String tks[] = lineStr.split("\\s+");
			// double ret[] = new double[tks.length - 1];
			// for (int i = 1; i < tks.length; i++) {
			// ret[i - 1] = Double.parseDouble(tks[i]);
			// }
			// return ret;
			String lineStr = "";
			if (attri.equals("number")) {
				lineStr = numberRS.get(zpID);
			} else if (attri.equals("gender")) {
				lineStr = genderRS.get(zpID);
			} else if (attri.equals("person")) {
				lineStr = personRS.get(zpID);
			} else if (attri.equals("animacy")) {
				lineStr = animacyRS.get(zpID);
			} else {
				Common.bangErrorPOS("No Such Attri");
			}
			String tks[] = lineStr.split("\\s+");
			double ret[] = new double[tks.length - 1];
			for (int i = 1; i < tks.length; i++) {
				ret[i - 1] = Double.parseDouble(tks[i]);
			}
			return ret;
		}
		case load: {
			String lineStr = "";
			if (attri.equals("number")) {
				lineStr = numberRS.get(zpID);
			} else if (attri.equals("gender")) {
				lineStr = genderRS.get(zpID);
			} else if (attri.equals("person")) {
				lineStr = personRS.get(zpID);
			} else if (attri.equals("animacy")) {
				lineStr = animacyRS.get(zpID);
			} else {
				Common.bangErrorPOS("No Such Attri");
			}
			String tks[] = lineStr.split("\\s+");
			double ret[] = new double[tks.length - 1];
			for (int i = 1; i < tks.length; i++) {
				ret[i - 1] = Double.parseDouble(tks[i]);
			}
			return ret;
			// if (true) {
			// // double ret[] = new double[all];
			// // return ret;
			// return selectRestriction(attri, all, v);
			// }
			// return selectRestriction(attri, all, v);
		}
		default: {
			Common.bangErrorPOS("WRONG MODE");
		}
		}
		return null;
	}

	public static double[] selectRestriction(String attri, int all, String v) {
		if (Context.svoStat == null) {
			return new double[all];
		}
		HashMap<Integer, Integer> map = null;
		if (attri.equals("number")) {
			map = Context.svoStat.numberStat.get(v);
		} else if (attri.equals("gender")) {
			map = Context.svoStat.genderStat.get(v);
		} else if (attri.equals("person")) {
			map = Context.svoStat.personStat.get(v);
		} else if (attri.equals("animacy")) {
			map = Context.svoStat.animacyStat.get(v);
		} else {
			Common.bangErrorPOS("No Such Attri");
		}
		double ret[] = new double[all];
		if (map == null) {
			for (int i = 0; i < all; i++) {
				ret[i] = 1.0 / all;
			}
		} else {
			double overall = 0;
			for (Integer k : map.keySet()) {
				overall += map.get(k);
			}
			for (int i = 0; i < all; i++) {
				double val = 0;
				if (map.containsKey(i)) {
					val = map.get(i);
				}
				ret[i] = val / overall;
			}
		}
		return ret;
	}

	static int maxAnts = 1200;

	private double[] runYasmet(String str, int antCount, String model) {
		String tks[] = str.split("@");
		int numberOfUnit = tks.length - 2;
		if (numberOfUnit > maxAnts) {
			Common.bangErrorPOS("!!!MAX: " + numberOfUnit);
		}
		for (int i = numberOfUnit; i < maxAnts; i++) {
			str += "@ 0 NOCLASS 1 # ";
		}

		switch (mode) {
		case prepare: {
			ArrayList<String> lst = anteTest.get(model);
			if (lst == null) {
				lst = new ArrayList<String>();
				lst.add(Integer.toString(maxAnts));
				anteTest.put(model, lst);
			}
			lst.add(str);
			return new double[antCount];
		}
		case classify: {
			String lineStr = "";
			String cmd = "/users/yzcchen/tool/YASMET/./a.out /users/yzcchen/tool/YASMET/"
					+ model;

			Runtime run = Runtime.getRuntime();
			double ret[] = new double[antCount];
			try {
				Process p = run.exec(cmd);

				BufferedOutputStream out = new BufferedOutputStream(
						p.getOutputStream());
				out.write(Integer.toString(maxAnts).getBytes());
				out.write(("\n" + str).getBytes());
				out.flush();
				out.close();

				// BufferedInputStream errIn = new
				// BufferedInputStream(p.getErrorStream());
				// BufferedReader errBr = new BufferedReader(new
				// InputStreamReader(errIn));

				BufferedInputStream in = new BufferedInputStream(
						p.getInputStream());
				BufferedReader inBr = new BufferedReader(new InputStreamReader(
						in));
				lineStr = inBr.readLine();
				if (p.waitFor() != 0) {
					if (p.exitValue() == 1) {
						System.err.println("ERROR YASMET");
						Common.bangErrorPOS("");
					}
				}
				inBr.close();
				in.close();

				tks = lineStr.split("\\s+");
				double norm = 0;
				for (int i = 0; i < antCount; i++) {
					double conf = Double.parseDouble(tks[i + 1]);
					norm += conf;
					ret[i] = conf;
				}
				for (int i = 0; i < antCount; i++) {
					ret[i] = ret[i] / norm;
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return ret;
		}
		case (load): {
			double ret[] = new double[antCount];
			ArrayList<String> lst = anteRS.get(model);
			String lineStr = lst.get(zpID);
			tks = lineStr.split("\\s+");
			double norm = 0;
			for (int i = 0; i < antCount; i++) {
				double conf = Double.parseDouble(tks[i + 1]);
				norm += conf;
				ret[i] = conf;
			}
			for (int i = 0; i < antCount; i++) {
				ret[i] = ret[i] / norm;
			}
			return ret;
		}
		default:
			Common.bangErrorPOS("");
		}
		return null;
	}

	// dd
	private String runSVMRank() {
		String lineStr = "";
		String cmd = "./svmRank.sh";

		Runtime run = Runtime.getRuntime();
		try {
			Process p = run.exec(cmd);
			BufferedInputStream in = new BufferedInputStream(p.getInputStream());
			BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
			lineStr = inBr.readLine();
			// System.out.println(lineStr);
			if (p.waitFor() != 0) {
				if (p.exitValue() == 1) {
					System.err.println("ERROR YASMET");
					Common.bangErrorPOS("");
				}
			}
			inBr.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ArrayList<String> lines = Common.getLines("svmRank.result");
		StringBuilder sb = new StringBuilder();
		double maxP = Double.NEGATIVE_INFINITY;
		int maxIdx = -1;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			double p = Double.parseDouble(line);
			if (p > maxP) {
				maxP = p;
				maxIdx = i;
			}
			sb.append(p).append(" ");
		}
		sb.insert(0, maxIdx + " ");
		return sb.toString().trim();
	}

	private double runSVMLight() {
		String lineStr = "";
		String cmd = "./svmlight.sh";

		Runtime run = Runtime.getRuntime();
		try {
			Process p = run.exec(cmd);
			BufferedInputStream in = new BufferedInputStream(p.getInputStream());
			BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
			lineStr = inBr.readLine();
			// System.out.println(lineStr);
			if (p.waitFor() != 0) {
				if (p.exitValue() == 1) {
					System.err.println("ERROR YASMET");
					Common.bangErrorPOS("");
				}
			}
			inBr.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ArrayList<String> lines = Common.getLines("svmlight.result");

		return Double.parseDouble(lines.get(0).split("\\s+")[0]);
	}

	public double getMaxEntProb(Mention cand, Mention pro, boolean sameSpeaker,
			Context context, CoNLLPart part) {
		String pronoun = pro.extent;
		String pStr = "";
		if (sameSpeaker) {
			pStr = EMUtil.getAntPerson(cand.head).name() + "="
					+ EMUtil.getPerson(pronoun).name();
		} else {
			pStr = EMUtil.getAntPerson(cand.head).name() + "!="
					+ EMUtil.getPerson(pronoun).name();
		}
		String nStr = EMUtil.getAntNumber(cand).name() + "="
				+ EMUtil.getNumber(pronoun).name();
		String aStr = EMUtil.getAntAnimacy(cand).name() + "="
				+ EMUtil.getAnimacy(pronoun).name();
		String gStr = EMUtil.getAntGender(cand).name() + "="
				+ EMUtil.getGender(pronoun).name();
		superFea.configure(pStr, nStr, gStr, aStr, context, cand, pro, part);

		String svm = superFea.getSVMFormatString();
		svm = "-1 " + svm;
		// Datum<String, String> testIns = EMUtil.svmlightToStanford(svm);
		Datum<String, String> testIns = EMUtil.svmlightToStanford(
				superFea.getFeas(), "-1");
		Counter<String> scores = classifier.scoresOf(testIns);
		Distribution<String> distr = Distribution
				.distributionFromLogisticCounter(scores);
		double prob = distr.getCount("+1");
		return prob;
	}

	public void addEmptyCategoryNode(Mention zero) {
		MyTreeNode V = zero.V;
		MyTreeNode newNP = new MyTreeNode();
		newNP.value = "NP";
		int VIdx = V.childIndex;
		V.parent.addChild(VIdx, newNP);

		MyTreeNode empty = new MyTreeNode();
		empty.value = "-NONE-";
		newNP.addChild(empty);

		MyTreeNode child = new MyTreeNode();
		child.value = zero.extent;
		empty.addChild(child);
		child.emptyCategory = true;
		zero.NP = newNP;
	}

	public static boolean findBest(Mention zero, ArrayList<Mention> cands) {
		boolean findBest = false;
		for (int i = 0; i < cands.size(); i++) {
			Mention cand = cands.get(i);
			if (cand.gram == EMUtil.Grammatic.subject && cand.MI > 0
					&& cand.s == zero.s) {
				findBest = true;
				cand.isBest = true;
				break;
			}
		}

		if (!findBest) {
			for (int i = 0; i < cands.size(); i++) {
				Mention cand = cands.get(i);
				if (cand.MI > 0 && cand.s == zero.s) {
					findBest = true;
					cand.isBest = true;
					break;
				}
			}
		}

		if (!findBest) {
			for (int i = 0; i < cands.size(); i++) {
				Mention cand = cands.get(i);
				if (cand.MI > 0 && cand.gram == EMUtil.Grammatic.subject) {
					findBest = true;
					cand.isBest = true;
					break;
				}
			}
		}

		if (!findBest) {
			for (int i = 0; i < cands.size(); i++) {
				Mention cand = cands.get(i);
				if (cand.MI > 0) {
					findBest = true;
					cand.isBest = true;
					break;
				}
			}
		}
		return findBest;
	}

	static String prefix = "/shared/mlrdir1/disk1/mlr/corpora/CoNLL-2012/conll-2012-train-v0/data/files/data/chinese/annotations/";
	static String anno = "annotations/";
	static String suffix = ".coref";

	public void evaluate(List<ArrayList<Mention>> zeroses,
			List<ArrayList<Entity>> entitieses) {
		double gold = 0;
		double system = 0;
		double hit = 0;

		for (int i = 0; i < zeroses.size(); i++) {
			ArrayList<Mention> zeros = zeroses.get(i);
			ArrayList<Entity> entities = entitieses.get(i);
			ArrayList<Mention> goldInChainZeroses = EMUtil
					.getAnaphorZeros(entities);
			HashMap<String, Integer> chainMap = EMUtil.formChainMap(entities);
			gold += goldInChainZeroses.size();
			system += zeros.size();
			for (Mention zero : zeros) {
				Mention ant = zero.antecedent;
				Integer zID = chainMap.get(zero.toName());
				Integer aID = chainMap.get(ant.toName());
				if (zID != null && aID != null
						&& zID.intValue() == aID.intValue()) {
					hit++;
				}
			}
		}

		double r = hit / gold;
		double p = hit / system;
		f = 2 * r * p / (r + p);

		String para = p1 + " " + p2 + " " + p3 + " " + p4;
		String result = "R:" + r + " P: " + p + " F: " + f;
		if (f > best) {
			best = f;
			bestParas = para;
		}
		System.err.println(para + "\t" + result);
		System.err.println(bestParas + "\t" + best);
		System.out.println(para);
		System.out.println("============");
		System.out.println("Hit: " + hit);
		System.out.println("Gold: " + gold);
		System.out.println("System: " + system);
		System.out.println("============");
		System.out.println("Recall: " + r * 100);
		System.out.println("Precision: " + p * 100);
		System.out.println("F-score: " + f * 100);

		if (!this.folder.equals("all")) {
			zeroses.clear();
			entitieses.clear();
		}
		bads.clear();
		goods.clear();
		zpID = 0;
	}

	static double f;
	static double best = 0;
	static String bestParas = "";

	static String setting = "setting1";
	static CoNLLPart engPart;

	static void loadAlign() {
		ArrayList<SentForAlign[]> alignCachelist = DocumentMap
				.loadRealBAAlignResult("/users/yzcchen/chen3/zeroEM/EMAlgorithm/src/setting1/ba/");
		for (SentForAlign[] align : alignCachelist) {
			String chi = align[0].getText();
			ArrayList<SentForAlign[]> lst = alignMap.get(chi);
			if (lst == null) {
				lst = new ArrayList<SentForAlign[]>();
				alignMap.put(chi, lst);
			}
			lst.add(align);
		}

		System.out.println("Done1.");
		CoNLLPart.processDiscourse = false;
		CoNLLDocument engDoc = new CoNLLDocument(setting + "/conll/eng.conll");
		CoNLLPart.processDiscourse = true;
		engPart = engDoc.getParts().get(0);
		for (CoNLLSentence s : engDoc.getParts().get(0).getCoNLLSentences()) {
			String engS = s.getText();
			ArrayList<CoNLLSentence> lst = engSMap.get(engS);
			if (lst == null) {
				lst = new ArrayList<CoNLLSentence>();
				engSMap.put(engS, lst);
			}
			lst.add(s);
		}
		System.out.println("Done2.");
	}

	public static void main(String args[]) {
		loadAlign();
		if (args.length < 1) {
			System.err.println("java ~ folder [mode]");
			System.exit(1);
		}
		if (args.length == 3) {
//			sigID = Integer.parseInt(args[2]);
			pp = Double.parseDouble(args[2]);
		}
		if (args[1].equals("prepare")) {
			mode = prepare;
			run(args[0]);

			Common.outputLines(numberTest, "number.test" + args[0]);
			Common.outputLines(genderTest, "gender.test" + args[0]);
			Common.outputLines(personTest, "person.test" + args[0]);
			Common.outputLines(animacyTest, "animacy.test" + args[0]);
			for (String key : anteTest.keySet()) {
				ArrayList<String> lst = anteTest.get(key);
				Common.outputLines(lst, "antetest" + args[0] + key);
			}
			return;
		} else if (args[1].equals("load")) {
			mode = load;
			personRS = Common.getLines("/users/yzcchen/tool/YASMET/person.rs"
					+ args[0]);
			genderRS = Common.getLines("/users/yzcchen/tool/YASMET/gender.rs"
					+ args[0]);
			numberRS = Common.getLines("/users/yzcchen/tool/YASMET/number.rs"
					+ args[0]);
			animacyRS = Common.getLines("/users/yzcchen/tool/YASMET/animacy.rs"
					+ args[0]);

			anteRS = new HashMap<String, ArrayList<String>>();
			ArrayList<String> lines1 = Common
					.getLines("/users/yzcchen/tool/YASMET/anters" + args[0]
							+ "WT");
			System.out.println(lines1.size());
			anteRS.put("WT", lines1);
			ArrayList<String> lines2 = Common
					.getLines("/users/yzcchen/tool/YASMET/anters" + args[0]
							+ "WTZ");
			System.out.println(lines2.size());
			anteRS.put("WTZ", lines2);

			// run(args[0]);
			// tuneWay1(args);
			tuneWay2(args);
		} else if (args[1].equals("classify")) {
			personRS = Common.getLines("/users/yzcchen/tool/YASMET/person.rs"
					+ args[0]);
			genderRS = Common.getLines("/users/yzcchen/tool/YASMET/gender.rs"
					+ args[0]);
			numberRS = Common.getLines("/users/yzcchen/tool/YASMET/number.rs"
					+ args[0]);
			animacyRS = Common.getLines("/users/yzcchen/tool/YASMET/animacy.rs"
					+ args[0]);

			mode = classify;
			run(args[0]);
			return;
		} else {
			Common.bangErrorPOS("");
		}
	}

	private static void tuneWay2(String[] args) {
		// double para[] = { 0, 0.1, 0.3, 0.5, 0.7, 0.9, 1, 1.2, 1.4, 1.6, 1.8,
		// 2};
		// double para[] = { 0, 0.2, 0.4, 0.6, 0.8, 1, 1.1, 1.3, 1.5, 1.7, 1.9};
		// double para[] = { 0, 0.08, 0.18, 0.28, 0.38, 0.48, 0.58, 0.68, 0.78,
		// 0.88, 1.0};
		double para[] = { 0, 0.1, 0.2, 0.3, 0.4, 0.5, .6, .7, .8, .9, 1, 1.1,
				1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9 };
		// double para[] = { 0, 0.05, 0.15, 0.25, 0.35, 0.45, 0.55, .65, .75,
		// .85, .95, 1};

		p1 = 1.4;
		p2 = 0.;
		p3 = 0;
		p4 = 0.0;

		while (true) {
			double bestAlpha = p1;
			double localBest = 0;
			for (double p : para) {
				p1 = p;
				run(args[0]);
				if (f > localBest) {
					localBest = f;
					bestAlpha = p;
				}
			}
			p1 = bestAlpha;

			double bestBeta = p2;
			for (double p : para) {
				p2 = p;
				run(args[0]);
				if (f > localBest) {
					localBest = f;
					bestBeta = p;
				}
			}
			p2 = bestBeta;

			double bestTheta = p3;
			for (double p : para) {
				p3 = p;
				run(args[0]);
				if (f > localBest) {
					localBest = f;
					bestTheta = p;
				}
			}
			p3 = bestTheta;

			double bestDelta = p4;
			for (double p : para) {
				p4 = p;
				run(args[0]);
				if (f > localBest) {
					localBest = f;
					bestDelta = p;
				}
			}
			p4 = bestDelta;
		}
	}

	private static void tuneWay1(String[] args) {
		double para[] = { 0, 0.1, 0.5, 1, 2 };
		for (int b = 0; b < para.length; b++) {
			for (int c = 0; c < para.length; c++) {
				for (int d = 0; d < para.length; d++) {
					for (int a = 0; a < para.length; a++) {
						alpha = para[a];
						beta = para[b];
						theta = para[c];
						delta = para[d];
						run(args[0]);
					}
				}
			}
		}
	}

	public static void run(String folder) {
		EMUtil.train = false;
		ApplyMaxEnt10Eng test = new ApplyMaxEnt10Eng(folder);
		test.test();

		// System.out.println(EMUtil.missed);
		System.out.println(EMUtil.missed.size());

		Common.outputLines(goods, "goods");
		Common.outputLines(bads, "bas");

		Common.outputHashMap(EMUtil.NEMap, "NEMAP");

		Common.outputHashSet(Context.ss, "miniS");
		Common.outputHashSet(Context.vs, "miniV");

	}
}

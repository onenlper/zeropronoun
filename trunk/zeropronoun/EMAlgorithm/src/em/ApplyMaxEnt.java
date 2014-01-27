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

import lpsolve.LpSolveException;
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
import em.EMUtil.Grammatic;
import em.ResolveGroup.Entry;

public class ApplyMaxEnt {

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

	HashMap<Short, Double> pronounPrior;
	HashMap<Integer, HashMap<Short, Integer>> counts;
	HashMap<Integer, Integer> denomCounts;
	HashMap<Integer, HashSet<Integer>> subSpace;

	HashMap<String, Double> fracContextCount;

	GuessPronounFea guessFea;

	LinearClassifier<String, String> classifier;

	SuperviseFea superFea;

	static int pronounID;

	static ArrayList<String> numberTest = new ArrayList<String>();
	static ArrayList<String> genderTest = new ArrayList<String>();
	static ArrayList<String> personTest = new ArrayList<String>();
	static ArrayList<String> animacyTest = new ArrayList<String>();

	static ArrayList<String> anteTest = new ArrayList<String>();

	static ArrayList<String> numberRS;
	static ArrayList<String> genderRS;
	static ArrayList<String> personRS;
	static ArrayList<String> animacyRS;
	static ArrayList<String> anteRS;

	public static ArrayList<String> goods = new ArrayList<String>();
	public static ArrayList<String> bads = new ArrayList<String>();

	double good = 0;
	double bad = 0;

	@SuppressWarnings("unchecked")
	public ApplyMaxEnt(String folder) {
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
			loadGuessProb();
			guessFea = new GuessPronounFea(false, "guessPronoun");

			superFea = new SuperviseFea(false, "supervise");
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

	public void test() {
		ArrayList<String> files = Common.getLines("chinese_list_" + folder
				+ "_development");

		ArrayList<ArrayList<Mention>> corefResults = new ArrayList<ArrayList<Mention>>();
		ArrayList<ArrayList<Entity>> goldEntities = new ArrayList<ArrayList<Entity>>();

		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file.replace(
					"auto_conll", "gold_conll"));
			OntoCorefXMLReader.addGoldZeroPronouns(document, false);

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);

				for (CoNLLSentence s : part.getCoNLLSentences()) {
					for (CoNLLWord w : s.words) {
						if (!w.speaker.equals("-")) {
							// System.out.println(w.speaker + "#" +
							// s.getText());
						}
					}
				}

				ArrayList<Entity> goldChains = part.getChains();

				goldEntities.add(goldChains);

				HashMap<String, Integer> chainMap = EMUtil
						.formChainMap(goldChains);

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

				if (!file.contains("/nw/")
				// && !file.contains("/mz/")&& !file.contains("/wb/")
				) {
					 candidates.addAll(anaphorZeros);
				}
				Collections.sort(candidates);

				Collections.sort(anaphorZeros);

				findAntecedent(file, part, chainMap, corefResult, anaphorZeros,
						candidates);

				// findSVMLight(file, part, chainMap, corefResult, anaphorZeros,
				// candidates);
			}
		}
		System.out.println("Good: " + good);
		System.out.println("Bad: " + bad);
		System.out.println("Precission: " + good / (good + bad) * 100);

		bad = 0;
		good = 0;
		evaluate(corefResults, goldEntities);
	}

	private void findAntecedent(String file, CoNLLPart part,
			HashMap<String, Integer> chainMap, ArrayList<Mention> corefResult,
			ArrayList<Mention> anaphorZeros, ArrayList<Mention> allCandidates) {
		for (Mention zero : anaphorZeros) {
			zero.sentenceID = part.getWord(zero.start).sentence
					.getSentenceIdx();
			zero.s = part.getWord(zero.start).sentence;
			EMUtil.assignVNode(zero, part);
			if (zero.notInChainZero) {
				continue;
			}
			if (zero.V == null) {
				continue;
			}
			Mention antecedent = null;
			double maxP = -1;
			Collections.sort(allCandidates);
			String proSpeaker = part.getWord(zero.start).speaker;
			String overtPro = "";

			ArrayList<Mention> cands = new ArrayList<Mention>();
			boolean findFS = false;
			HashSet<Integer> filters = new HashSet<Integer>();
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
					if (!findFS && cand.gram == EMUtil.Grammatic.subject
					// && !cand.s.getWord(cand.headInS).posTag.equals("NT")
					// && MI>0
					) {
						cand.isFS = true;
						findFS = true;
					}
					// if(cand.s==zero.s && cand.gram==Grammatic.object &&
					// cand.end+2==zero.start &&
					// part.getWord(cand.end+1).word.equals("，") && cand.MI>0){
					// cand.isFS = true;
					// findFS = true;
					// }
					cands.add(cand);
				}
			}
			findBest(zero, cands);

			// call yasmet to get Prob(gender|context) Prob(number|context)
			// Prob(person|context) Prob(animacy|context)
			guessFea.configure(zero.start - 1, zero.start,
					part.getWord(zero.start).sentence, part);

			// label = pronoun.animacy.ordinal() + 1;

			String feaStr = guessFea.getSVMFormatString();

			String v = EMUtil.getFirstVerb(zero.V);
			// Yasmet format
			// NUMBER, GENDER, PERSON, ANIMACY
			String tks[] = feaStr.split("\\s+");
			int all = EMUtil.Number.values().length;
			double[] numberProbs = ApplyMaxEnt.selectRestriction("number", all,
					v);
			String nYSB = transform(tks, all, 0, numberProbs);
			double probNum[] = runAttri("number", nYSB, all, v);

			all = EMUtil.Gender.values().length - 1;
			double[] genderProbs = ApplyMaxEnt.selectRestriction("gender", all,
					v);
			String gYSB = transform(tks, all, 0, genderProbs);
			double probGen[] = runAttri("gender", gYSB, all, v);

			all = EMUtil.Person.values().length;
			double[] personProbs = ApplyMaxEnt.selectRestriction("person", all,
					v);
			String pYSB = transform(tks, all, 0, personProbs);
			double probPer[] = runAttri("person", pYSB, all, v);

			all = EMUtil.Animacy.values().length - 1;
			double[] animacyProbs = ApplyMaxEnt.selectRestriction("animacy",
					all, v);
			String aYSB = transform(tks, all, 0, animacyProbs);
			double probAni[] = runAttri("animacy", aYSB, all, v);
			// TODO

			// init yasmet
			StringBuilder ysb = new StringBuilder();
			ysb.append("0 @ ");
			int antCount = 0;
			HashMap<Integer, Integer> idMap = new HashMap<Integer, Integer>();
			for (int m = 0; m < EMUtil.pronounList.size(); m++) {
				String pronoun = EMUtil.pronounList.get(m);

				zero.extent = pronoun;
				ArrayList<String> units = new ArrayList<String>();
				ArrayList<String> svmRanks = new ArrayList<String>();
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
					boolean coref = chainMap.containsKey(zero.toName())
							&& chainMap.containsKey(cand.toName())
							&& chainMap.get(zero.toName()).intValue() == chainMap
									.get(cand.toName()).intValue();

					//
					Context context = Context.buildContext(cand, zero, part,
							cand.isFS);
					cand.msg = Context.message;
					cand.MI = Context.MI;

					if (m == 0) {
						if (coref) {
							goods.add(Double.toString(cand.MI));
						} else {
							bads.add(Double.toString(cand.MI));
						}
					}
					boolean sameSpeaker = proSpeaker.equals(antSpeaker);
					Entry entry = new Entry(cand, context, sameSpeaker,
							cand.isFS);

					int conflict = 0;
					if (sameSpeaker) {
						if (!entry.person.name().equals(
								EMUtil.getPerson(pronoun).name())) {
							conflict++;
						}
					} else {
						if (entry.person.name().equals(
								EMUtil.getPerson(pronoun).name())
								&& entry.person != EMUtil.Person.third) {
							conflict++;
						}
					}
					if (!entry.number.name().equals(
							EMUtil.getNumber(pronoun).name())) {
						conflict++;
					}
					if (!entry.gender.name().equals(
							EMUtil.getGender(pronoun).name())) {
						conflict++;
					}
					if (!entry.animacy.name().equals(
							EMUtil.getAnimacy(pronoun).name())
							&& entry.animacy != Animacy.unknown) {
						conflict++;
					}

					if (conflict > 0) {
						if (chainMap.containsKey(zero.toName())
								&& chainMap.containsKey(cand.toName())
								&& chainMap.get(zero.toName()).intValue() == chainMap
										.get(cand.toName()).intValue()) {
							// System.err.println(sameSpeaker + "#"
							// + entry.person.name() + "="
							// + EMUtil.getPerson(pronoun).name());
							// System.err.println(entry.number.name() + "="
							// + EMUtil.getNumber(pronoun).name());
							// System.err.println(entry.gender.name() + "="
							// + EMUtil.getGender(pronoun).name());
							// System.err.println(entry.animacy.name() + "="
							// + EMUtil.getAnimacy(pronoun).name());
						}
						filters.add(antCount);
					}

					String unit = MaxEntLearn.getYamset(false, cand, zero,
							context, sameSpeaker, entry, superFea, 1, part);

					ysb.append(unit);
					units.add(unit);
					if (cand.isFS) {
						// System.out.println(antCount + "###");
						// System.out.println(unit);
					}
					idMap.put(antCount, i);
					antCount++;
					String svmRank = MaxEntLearn.getSVMRank(0, cand, zero,
							context, sameSpeaker, entry, superFea, part);
					svmRanks.add(svmRank);
				}
				if (antCount == 0) {
					continue;
				}

				Common.outputLines(svmRanks, "svmRank.test");
				// Common.pause("");
				// break;
			}
			if (antCount > maximam) {
				maximam = antCount;
			}
			double probAnt[] = runYasmet(ysb.toString(), antCount);
			pronounID++;
			// System.err.println(cands.size());
			if (antCount != 0 && (mode == classify || mode == load)) {
				// run yasmet here

				int numberOfAnt = probAnt.length / EMUtil.pronounList.size();
				if (probAnt.length % EMUtil.pronounList.size() != 0) {
					Common.bangErrorPOS("!!");
				}

				// re-normalize?
				for (Integer f : filters) {
					// probAnt[f] = -1000000;
				}

				// do re-normalize
				double sumover = 0;
				for (int i = 0; i < antCount; i++) {
					if (!filters.contains(i)) {
						// sumover += probAnt[i];
					}
				}
				for (int i = 0; i < antCount; i++) {
					// probAnt[i] = probAnt[i] / sumover;
				}

//				HashSet<Integer> reranks = new HashSet<Integer>();
//				for (int i = 0; i < EMUtil.pronounList.size(); i++) {
//					double rankMax = 0;
//					int rerank = -1;
//					for (int j = 0; j < numberOfAnt; j++) {
//						double prob = probAnt[numberOfAnt * i + j];
//						if (prob > rankMax) {
//							rankMax = prob;
//							rerank = numberOfAnt * i + j;
//						}
//					}
//					reranks.add(rerank);
//				}
//				for (int i = 0; i < antCount; i++) {
//					if (reranks.contains(i)) {
//						sumover += probAnt[i];
//					}
//				}
//				for (int i = 0; i < antCount; i++) {
//					if (reranks.contains(i)) {
//						probAnt[i] = probAnt[i] / sumover;
//					} else {
//						probAnt[i] = -10000;
//					}
//				}

				System.out.println(filters.size() + "###############"
						+ antCount);
				int rankID = -1;
				double rankMax = 0;
				for (int i = 0; i < probAnt.length; i++) {
					double prob = probAnt[i];
					if (prob > rankMax) {
						rankMax = prob;
						rankID = i;
					}
				}
				int antIdx = -1;
				// if(cands.get(idMap.get(rankID)).end==-1) {
				// antIdx = idMap.get(rankID);
				// } else {
				ILP ilp = new ILP(numberOfAnt, probAnt, probNum, probGen,
						probPer, probAni);
				try {
					antIdx = ilp.execute();
				} catch (LpSolveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// }
				antecedent = cands.get(antIdx);

			}
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
		}
		for (Mention zero : anaphorZeros) {
			if (zero.antecedent != null) {
				corefResult.add(zero);
			}
		}
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

	// private void findSVMLight(String file, CoNLLPart part,
	// HashMap<String, Integer> chainMap, ArrayList<Mention> corefResult,
	// ArrayList<Mention> anaphorZeros, ArrayList<Mention> allCandidates) {
	// for (Mention zero : anaphorZeros) {
	// zero.sentenceID = part.getWord(zero.start).sentence
	// .getSentenceIdx();
	// zero.s = part.getWord(zero.start).sentence;
	// EMUtil.assignVNode(zero, part);
	// if (zero.notInChainZero) {
	// continue;
	// }
	// if (zero.V == null) {
	// continue;
	// }
	// Mention antecedent = null;
	// double maxP = -1;
	// Collections.sort(allCandidates);
	// String proSpeaker = part.getWord(zero.start).speaker;
	// String overtPro = "";
	//
	// ArrayList<Mention> cands = new ArrayList<Mention>();
	// boolean findFS = false;
	// for (int h = allCandidates.size() - 1; h >= 0; h--) {
	// Mention cand = allCandidates.get(h);
	// cand.sentenceID = part.getWord(cand.start).sentence
	// .getSentenceIdx();
	// cand.s = part.getWord(cand.start).sentence;
	// cand.isFS = false;
	// cand.isBest = false;
	// cand.MI = Context.calMI(cand, zero);
	// if (cand.start < zero.start
	// && zero.sentenceID - cand.sentenceID <= 2) {
	// if (!findFS && cand.gram == EMUtil.Grammatic.subject
	// // && !cand.s.getWord(cand.headInS).posTag.equals("NT")
	// // && MI>0
	// ) {
	// cand.isFS = true;
	// findFS = true;
	// }
	// // if(cand.s==zero.s && cand.gram==Grammatic.object &&
	// // cand.end+2==zero.start &&
	// // part.getWord(cand.end+1).word.equals("，") && cand.MI>0){
	// // cand.isFS = true;
	// // findFS = true;
	// // }
	// cands.add(cand);
	// }
	// }
	// findBest(zero, cands);
	//
	// for (int m = 0; m < EMUtil.pronounList.size(); m++) {
	// String pronoun = EMUtil.pronounList.get(m);
	// zero.extent = pronoun;
	// StringBuilder ysb = new StringBuilder();
	// ysb.append("0 @ ");
	// int antCount = 0;
	// ArrayList<String> units = new ArrayList<String>();
	//
	// ArrayList<String> svmRanks = new ArrayList<String>();
	// for (int i = 0; i < cands.size(); i++) {
	// Mention cand = cands.get(i);
	// if (cand.extent.isEmpty()) {
	// continue;
	// }
	// // if(!cand.isFS)
	// // continue;
	// String antSpeaker = part.getWord(cand.start).speaker;
	// cand.sentenceID = part.getWord(cand.start).sentence
	// .getSentenceIdx();
	// boolean coref = chainMap.containsKey(zero.toName())
	// && chainMap.containsKey(cand.toName())
	// && chainMap.get(zero.toName()).intValue() == chainMap
	// .get(cand.toName()).intValue();
	//
	// Context context = Context.buildContext(cand, zero, part,
	// cand.isFS);
	// cand.msg = Context.message;
	// cand.MI = Context.MI;
	//
	// if (m == 0) {
	// if (coref) {
	// goods.add(Double.toString(cand.MI));
	// } else {
	// bads.add(Double.toString(cand.MI));
	// }
	// }
	// boolean sameSpeaker = proSpeaker.equals(antSpeaker);
	// Entry entry = new Entry(cand, context, sameSpeaker,
	// cand.isFS);
	// String unit = MaxEntLearn.getYamset(false, cand, zero,
	// context, sameSpeaker, entry, superFea, 1, part);
	//
	// ysb.append(unit);
	// units.add(unit);
	// if (cand.isFS) {
	// System.out.println(antCount + "###");
	// System.out.println(unit);
	// }
	// antCount++;
	// String svmRank = MaxEntLearn.getSVMRank(0, cand, zero,
	// context, sameSpeaker, entry, superFea, part);
	//
	// int a1 = svmRank.indexOf(" ");
	// int a2 = svmRank.indexOf(" ", a1 + 1);
	// ArrayList<String> lines = new ArrayList<String>();
	// lines.add("0 " + svmRank.substring(a2 + 1));
	// Common.outputLines(lines, "svmlight.test");
	// double p = runSVMLight();
	//
	// if (p > maxP) {
	// antecedent = cand;
	// maxP = p;
	// overtPro = pronoun;
	// }
	//
	// svmRanks.add(svmRank);
	// }
	// if (antCount == 0) {
	// continue;
	// }
	//
	// break;
	// }
	//
	// if (antecedent != null) {
	// if (antecedent.end != -1) {
	// zero.antecedent = antecedent;
	// } else {
	// zero.antecedent = antecedent.antecedent;
	// }
	// zero.extent = antecedent.extent;
	// zero.head = antecedent.head;
	// zero.gram = Grammatic.subject;
	// zero.mType = antecedent.mType;
	// zero.NE = antecedent.NE;
	// this.addEmptyCategoryNode(zero);
	// // System.out.println(zero.start);
	// // System.out.println(antecedent.extent);
	// }
	// if (zero.antecedent != null
	// && zero.antecedent.end != -1
	// && chainMap.containsKey(zero.toName())
	// && chainMap.containsKey(zero.antecedent.toName())
	// && chainMap.get(zero.toName()).intValue() == chainMap.get(
	// zero.antecedent.toName()).intValue()) {
	// good++;
	// // if(antecedent.mType==MentionType.tmporal) {
	// // System.out.println(antecedent.extent + "GOOD!");
	// // }
	// // System.out.println(overtPro + "  " + zero.antecedent.extent);
	// // System.out.println("+++");
	// // printResult(zero, zero.antecedent, part);
	// // System.out.println("Predicate: " +
	// // this.getPredicate(zero.V));
	// // System.out.println("Object NP: " +
	// // this.getObjectNP(zero));
	// // System.out.println("===");
	// // if (zero.antecedent.MI < 0) {
	// // System.out.println("Right!!! " + good + "/" + bad);
	// // System.out.println(zero.antecedent.msg);
	// // }
	// } else {
	// // if(antecedent!=null && antecedent.mType==MentionType.tmporal)
	// // {
	// // System.out.println(antecedent.extent + "BAD !");
	// // }
	// bad++;
	// System.out.println("Error??? " + good + "/" + bad);
	// if (zero.antecedent != null) {
	// System.out.println(zero.antecedent.msg);
	// }
	// }
	// String conllPath = file;
	// int aa = conllPath.indexOf(anno);
	// int bb = conllPath.indexOf(".");
	// String middle = conllPath.substring(aa + anno.length(), bb);
	// String path = prefix + middle + suffix;
	// System.out.println(path);
	// // System.out.println("=== " + file);
	// EMUtil.addEmptyCategoryNode(zero);
	//
	// // if (antecedent != null) {
	// // CoNLLWord candWord = part.getWord(antecedent.start);
	// // CoNLLWord zeroWord = part.getWord(zero.start);
	// //
	// // String zeroSpeaker = part.getWord(zero.start).speaker;
	// // String candSpeaker = part.getWord(antecedent.start).speaker;
	// // // if (!zeroSpeaker.equals(candSpeaker)) {
	// // // if (antecedent.source.equals("我") &&
	// // // zeroWord.toSpeaker.contains(candSpeaker)) {
	// // // zero.head = "你";
	// // // zero.source = "你";
	// // // } else if (antecedent.source.equals("你") &&
	// // // candWord.toSpeaker.contains(zeroSpeaker)) {
	// // // zero.head = "我";
	// // // zero.source = "我";
	// // // }
	// // // } else {
	// // zero.extent = antecedent.extent;
	// // zero.head = antecedent.head;
	// // // }
	// //
	// // }
	// }
	// for (Mention zero : anaphorZeros) {
	// if (zero.antecedent != null) {
	// corefResult.add(zero);
	// }
	// }
	// }

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
			return new double[0];
		}
		case classify: {
			if (true) {
				// double ret[] = new double[all];
				// return ret;
				return selectRestriction(attri, all, v);
			}
			String lineStr = "";
			String cmd = "/users/yzcchen/tool/YASMET/./a.out /dev/shm/" + attri
					+ ".model";
			Runtime run = Runtime.getRuntime();
			try {
				Process p = run.exec(cmd);

				BufferedOutputStream out = new BufferedOutputStream(
						p.getOutputStream());
				out.write(str.getBytes());
				out.flush();
				out.close();

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
				System.out.println(lineStr);
				inBr.close();
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			String tks[] = lineStr.split("\\s+");
			double ret[] = new double[tks.length - 1];
			for (int i = 1; i < tks.length; i++) {
				ret[i - 1] = Double.parseDouble(tks[i]);
			}
			return ret;
		}
		case load: {
			// String lineStr = "";
			// if (attri.equals("number")) {
			// lineStr = numberRS.get(pronounID);
			// } else if (attri.equals("gender")) {
			// lineStr = genderRS.get(pronounID);
			// } else if (attri.equals("person")) {
			// lineStr = personRS.get(pronounID);
			// } else if (attri.equals("animacy")) {
			// lineStr = animacyRS.get(pronounID);
			// } else {
			// Common.bangErrorPOS("No Such Attri");
			// }
			// String tks[] = lineStr.split("\\s+");
			// double ret[] = new double[tks.length - 1];
			// for (int i = 1; i < tks.length; i++) {
			// ret[i - 1] = Double.parseDouble(tks[i]);
			// }
			// return ret;
			if (true) {
				 double ret[] = new double[all];
				 return ret;
//				return selectRestriction(attri, all, v);
			}
			return selectRestriction(attri, all, v);
		}
		default: {
			Common.bangErrorPOS("WRONG MODE");
		}
		}
		return null;
	}

	public static double[] selectRestriction(String attri, int all, String v) {
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

	private double[] runYasmet(String str, int antCount) {
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
			if (anteTest.isEmpty()) {
				anteTest.add(Integer.toString(maxAnts));
			}
			anteTest.add(str);
			return new double[0];
		}
		case classify: {
			String lineStr = "";
			String cmd = "/users/yzcchen/tool/YASMET/./a.out /dev/shm/WT";

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
			String lineStr = anteRS.get(pronounID);
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

	public static void evaluate(ArrayList<ArrayList<Mention>> zeroses,
			ArrayList<ArrayList<Entity>> entitieses) {
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
		double f = 2 * r * p / (r + p);

		String para = ILP.a_num + " " + ILP.b_gen + " " + ILP.c_per + " "
				+ ILP.d_ani;
		String result = "R:" + r + " P: " + p + " F: " + f;
		System.err.println(para + "\t" + result);
		System.out.println(para);
		System.out.println("============");
		System.out.println("Hit: " + hit);
		System.out.println("Gold: " + gold);
		System.out.println("System: " + system);
		System.out.println("============");
		System.out.println("Recall: " + r * 100);
		System.out.println("Precision: " + p * 100);
		System.out.println("F-score: " + f * 100);

		zeroses.clear();
		entitieses.clear();
		bads.clear();
		goods.clear();
		pronounID = 0;
	}

	static int maximam = 0;

	public static void main(String args[]) {
		if (args.length < 1) {
			System.err.println("java ~ folder [mode]");
			System.exit(1);
		}
		if (args[1].equals("prepare")) {
			mode = prepare;
		} else if (args[1].equals("load")) {
			mode = load;
		} else if (args[1].equals("classify")) {
			mode = classify;
		} else {
			Common.bangErrorPOS("");
		}

		if (mode == load) {
			personRS = Common.getLines("/users/yzcchen/tool/YASMET/person.rs"
					+ args[0]);
			genderRS = Common.getLines("/users/yzcchen/tool/YASMET/gender.rs"
					+ args[0]);
			numberRS = Common.getLines("/users/yzcchen/tool/YASMET/number.rs"
					+ args[0]);
			animacyRS = Common.getLines("/users/yzcchen/tool/YASMET/animacy.rs"
					+ args[0]);
			anteRS = Common.getLines("/users/yzcchen/tool/YASMET/ante.rs"
					+ args[0]);
		}
		double para[] = { 0, 0.008, 0.01, 0.02, 0.04, 0.06, 0.08 };

		String paras[] = { "0.008 0.01 0.06 0.01", "0.0075 0.01 0.06 0.01",
				"0.0085 0.01 0.06 0.01", "0.008 0.005 0.06 0.01",
				"0.008 0.015 0.06 0.01", "0.008 0.01 0.065 0.01",
				"0.008 0.01 0.055 0.01", "0.008 0.01 0.06 0.015",
				"0.008 0.01 0.06 0.0095", "0.008 0.009 0.055 0.01",
				"0.008 0.01 0.06 0.01", "0.008 0.009 0.06 0.01",
				"0.008 0.01 0.06 0.009", "0.008 0.01 0.06 0.012",
				"0.009 0.01 0.06 0.01", "0.009 0.012 0.06 0.01",
				"0.008 0.015 0.06 0.012" };

//		for (int a = 0; a < para.length; a++) {
//			for (int b = 0; b < para.length; b++) {
//				for (int c = 0; c < para.length; c++) {
//					for (int d = 0; d < para.length; d++) {
//						ILP.a_num = para[a];
//						ILP.b_gen = para[b];
//						ILP.c_per = para[c];
//						ILP.d_ani = para[d];
//						// while(true) {
//						// for (String par : paras) {
//						// String tks[] = par.trim().split("\\s+");
//						// ILP.a_num = Double.parseDouble(tks[0]);
//						// ILP.b_gen = Double.parseDouble(tks[1]);
//						// ILP.c_per = Double.parseDouble(tks[2]);
//						// ILP.d_ani = Double.parseDouble(tks[3]);
//						// if(ILP.c_per>ILP.d_ani && ILP.c_per>ILP.b_gen)
//
//						if (para[a] <= 0.04 && para[a] > 0 && para[b] <= 0.01
//								&& para[c] >= 0.04 && para[d] >= 0.02) {
							run(args[0]);
//						}
//						// Common.input("");
//						// System.exit(1);
//						// run("nw");
//						// run("mz");
//						// run("wb");
//						// run("bn");
//						// run("bc");
//						// run("tc");
//						// }
//						// System.exit(1);
//					}
//				}
//			}
//		}

		// run("nw");
		// run("mz");
		// run("wb");
		// run("bn");
		// run("bc");
		// run("tc");
	}

	public static void run(String folder) {
		EMUtil.train = false;
		ApplyMaxEnt test = new ApplyMaxEnt(folder);
		test.test();

		// System.out.println(EMUtil.missed);
		System.out.println(EMUtil.missed.size());

		Common.outputLines(goods, "goods");
		Common.outputLines(bads, "bas");

		Common.outputHashMap(EMUtil.NEMap, "NEMAP");

		Common.outputHashSet(Context.ss, "miniS");
		Common.outputHashSet(Context.vs, "miniV");

		if (mode == prepare) {
			Common.outputLines(numberTest, "number.test" + folder);
			Common.outputLines(genderTest, "gender.test" + folder);
			Common.outputLines(personTest, "person.test" + folder);
			Common.outputLines(animacyTest, "animacy.test" + folder);
			Common.outputLines(anteTest, "ante.test" + folder);
			System.out.println("MAX: " + maximam);
			System.exit(1);
		}
		System.out.println("MAX: " + maximam);
		// Common.input("!!");
	}
}

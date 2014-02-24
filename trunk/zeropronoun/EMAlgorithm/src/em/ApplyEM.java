package em;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

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
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Distribution;
import em.EMUtil.Grammatic;

public class ApplyEM {

	String folder;

	Parameter numberP;
	Parameter genderP;
	Parameter animacyP;
	Parameter personP;
	Parameter personQP;

	double contextOverall;

	HashMap<String, Double> contextPrior;

	int overallGuessPronoun;

	HashMap<Short, Double> pronounPrior;
	HashMap<Integer, HashMap<Short, Integer>> counts;
	HashMap<Integer, Integer> denomCounts;
	HashMap<Integer, HashSet<Integer>> subSpace;

	HashMap<String, Double> fracContextCount;

	GuessPronounFea fea;

	LinearClassifier<String, String> classifier;

	SuperviseFea superFea;

	HashMap<String, double[]> contextSuper;

	@SuppressWarnings("unchecked")
	public ApplyEM(String folder) {
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
			contextSuper = (HashMap<String, double[]>) modelInput.readObject();
			modelInput.close();

			classifier = LinearClassifier
					.readClassifier("stanfordClassifier.gz");
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
//			loadGuessProb();
			fea = new GuessPronounFea(false, "guessPronoun");

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
		calContextStat();
	}

	public void calContextStat() {

		// fracContextCount = (HashMap<String, Double>) modelInput
		// .readObject();
		// contextPrior = (HashMap<String, Double>) modelInput.readObject();
		HashSet<String> allContext = new HashSet<String>();
		allContext.addAll(contextPrior.keySet());

		HashMap<String, Double> allProbs = new HashMap<String, Double>();

		for (String context : allContext) {
			double p_context = 1;
			if (fracContextCount.containsKey(context.toString())) {
				p_context = (1.0 * EMUtil.alpha + fracContextCount.get(context
						.toString()))
						/ (2592.0 * EMUtil.alpha + contextPrior.get(context
								.toString()));
			} else {
				p_context = 1.0 / 2592.0;
			}
			allProbs.put(context, p_context);
//			System.out.println(context + " " + p_context);
		}

		int[] ids = { 0, 6, 7, 8, 9, 10, 13, 15 };
		HashMap<Integer, HashSet<Character>> valses = new HashMap<Integer, HashSet<Character>>();
		for (Integer id : ids) {
			HashSet<Character> vals = new HashSet<Character>();
			for (String context : allContext) {
				vals.add(context.charAt(id));
			}
			valses.put(id, vals);
		}

		for (Integer id : ids) {
			HashSet<Character> vals = valses.get(id);
			for (Character val : vals) {
				double count = 0;
				for (String context : allProbs.keySet()) {
					if (context.charAt(id) == val) {
						count++;
					}
				}

				double p = 1;
				for (String context : allProbs.keySet()) {
					double prob = allProbs.get(context);
					if (context.charAt(id) == val) {
						double mean = Math.pow(prob, 1 / count);
						p *= mean;
					}
				}
//				System.out.println(id + " : " + val + " : " + p);
			}
		}
//		Common.pause("");
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

	public static ArrayList<String> goods = new ArrayList<String>();
	public static ArrayList<String> bads = new ArrayList<String>();

	double good = 0;
	double bad = 0;

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
//				ArrayList<Mention> anaphorZeros = ZeroDetect.getHeuristicZeros(part);
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

				// findAntecedentMaxEnt(file, part, chainMap, corefResult,
				// anaphorZeros,
				// candidates);
			}
		}
		System.out.println("Good: " + good);
		System.out.println("Bad: " + bad);
		System.out.println("Precission: " + good / (good + bad) * 100);

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

			ArrayList<String> taMSGs = new ArrayList<String>();
			String taMSg = "";
			String bestMSg = "";
			for (int h = allCandidates.size() - 1; h >= 0; h--) {
				Mention cand = allCandidates.get(h);
				String antSpeaker = part.getWord(cand.start).speaker;
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

			boolean findBest = findBest(zero, cands);

			// if(zero.start==179) {
			// for(Mention cand : cands) {
			// System.out.println(cand.extent);
			// }
			// Common.bangErrorPOS("");
			// }
			int chose = -1;
			double votes[] = new double[cands.size()];
			for (int m = 0; m < EMUtil.pronounList.size(); m++) {
				String pronoun = EMUtil.pronounList.get(m);
				zero.extent = pronoun;
				double norm = 0;

				double probs[] = new double[cands.size()];

				for (int i = 0; i < cands.size(); i++) {
					Mention cand = cands.get(i);
					if (cand.extent.isEmpty()) {
						continue;
					}

					// if(cand.gram != EMUtil.Grammatic.subject) {
					// continue;
					// }
					// if(!cand.isBest) {
					// continue;
					// }

					// if(!cand.isFS)
					// continue;

					String antSpeaker = part.getWord(cand.start).speaker;
					cand.sentenceID = part.getWord(cand.start).sentence
							.getSentenceIdx();
					boolean coref = chainMap.containsKey(zero.toName())
							&& chainMap.containsKey(cand.toName())
							&& chainMap.get(zero.toName()).intValue() == chainMap
									.get(cand.toName()).intValue();

					// calculate P(overt-pronoun|ant-context)
					String ant = cand.head;

					// TODO
					Context context = Context.buildContext(cand, zero, part,
							cand.isFS);
					cand.msg = Context.message;
					cand.MI = Context.MI;
					if (cand.MI < -1) {
						if (m == 0) {
							if (coref) {
								// System.out.println(coref);
								// System.out.println(cand.msg);
							} else {
								// System.out.println(coref);
								// System.out.println(cand.msg);
							}
						}
						// continue;
					}

					if (m == 0) {
						if (coref) {
							goods.add(Double.toString(cand.MI));
						} else {
							bads.add(Double.toString(cand.MI));
						}
					}

					boolean sameSpeaker = proSpeaker.equals(antSpeaker);
					double p_person = 0;
					if (sameSpeaker) {
						p_person = personP.getVal(EMUtil.getAntPerson(ant)
								.name(), EMUtil.getPerson(pronoun).name());
					} else {
						p_person = personQP.getVal(EMUtil.getAntPerson(ant)
								.name(), EMUtil.getPerson(pronoun).name());
					}
					cand.person = EMUtil.getAntPerson(ant);
					double p_number = numberP.getVal(EMUtil.getAntNumber(cand)
							.name(), EMUtil.getNumber(pronoun).name());
					cand.number = EMUtil.getAntNumber(cand);
					double p_animacy = animacyP.getVal(
							EMUtil.getAntAnimacy(cand).name(), EMUtil
									.getAnimacy(pronoun).name());
					cand.animacy = EMUtil.getAntAnimacy(cand);
					double p_gender = genderP.getVal(EMUtil.getAntGender(cand)
							.name(), EMUtil.getGender(pronoun).name());
					cand.gender = EMUtil.getAntGender(cand);
					// double p_number = numberP.getVal(cand.head,
					// EMUtil.getNumber(pronoun).name());
					// double p_animacy = animacyP.getVal(cand.head,
					// EMUtil.getAnimacy(pronoun).name());
					// double p_gender = genderP.getVal(cand.head,
					// EMUtil.getGender(pronoun).name());

					double p_context = 0.0000000000000000000000000000000000000000000001;
					if (fracContextCount.containsKey(context.toString())) {
						p_context = (1.0 * EMUtil.alpha + fracContextCount
								.get(context.toString()))
								/ (2592.0 * EMUtil.alpha + contextPrior
										.get(context.toString()));
					} else {
						p_context = 1.0 / 2592.0;
					}

					// double p_context = 1;
					// if(contextSuper.containsKey(context.toString())) {
					// double[] s = contextSuper.get(context.toString());
					// p_context = s[0]/(s[0]+s[1]);
					// }

//					double maxentP = this.getMaxEntProb(cand, zero,
//							sameSpeaker, context, part);

					double p2nd = p_person * p_number * p_gender * p_animacy
							* p_context * 1;

					if (pronoun.equals("它")) {
						String msg = p_person + "\t" + p_number + "\t"
								+ p_gender + "\t" + p_animacy + "\t"
								+ p_context;
						taMSGs.add(msg);
					}

					double p = p2nd;
					norm += p;
					probs[i] = p;
					// p = 1;
					// System.out.println(p);

					// p = maxentP;

					if (p > maxP) {
						antecedent = cand;
						maxP = p;
						overtPro = pronoun;
						bestMSg = p_person + "\t" + p_number + "\t" + p_gender
								+ "\t" + p_animacy + "\t" + p_context;
						chose = i;
					}
					// if(coref) {
					// antecedent = cand;
					// }
				}

				// normalize
				for (int i = 0; i < probs.length; i++) {
					probs[i] = probs[i] / norm;
				}

				for (int i = 0; i < probs.length; i++) {
					double prob = probs[i];
					votes[i] += prob;
				}
			}

			if (votes.length != 0) {
				double maxProb = 0;
				int maxIndex = -1;
				for (int i = 0; i < votes.length; i++) {
					double vote = votes[i];
					if (vote > maxProb) {
						maxProb = vote;
						maxIndex = i;
					}
				}
				// antecedent = cands.get(maxIndex);
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
				taMSg = taMSGs.get(chose);
			}
			if (zero.antecedent != null
					&& zero.antecedent.end != -1
					&& chainMap.containsKey(zero.toName())
					&& chainMap.containsKey(zero.antecedent.toName())
					&& chainMap.get(zero.toName()).intValue() == chainMap.get(
							zero.antecedent.toName()).intValue()) {
				good++;
				String key = part.docName + ":" + part.getPartID() + ":"
						+ zero.start + "-" + zero.antecedent.start + ","
						+ zero.antecedent.end + ":GOOD";
				corrects.add(key);
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
				// if(!zero.antecedent.isFS) {
				System.out.println("==========");
				System.out.println("Correct!!! " + good + "/" + bad);
				if (zero.antecedent != null) {
					System.out.println(zero.antecedent.extent + ":"
							+ zero.antecedent.NE + "#" + zero.antecedent.number
							+ "#" + zero.antecedent.gender + "#"
							+ zero.antecedent.person + "#"
							+ zero.antecedent.animacy);
					System.out.println(zero);
					printResult(zero, zero.antecedent, part);
					System.out.println(overtPro + "#");
				}
				// System.out.println(overtPro + "#" + bestMSg);
				// System.out.println("它: " + taMSg);
				// }
				// }
			} else {
				if(zero.antecedent==null) {
					String key = part.docName + ":" + part.getPartID() + ":"
							+ zero.start + "-NULL:BAD";
					corrects.add(key);
				} else {
					String key = part.docName + ":" + part.getPartID() + ":"
						+ zero.start + "-" + zero.antecedent.start + ","
						+ zero.antecedent.end + ":BAD";
					corrects.add(key);
				}
				// if(antecedent!=null && antecedent.mType==MentionType.tmporal)
				// {
				// System.out.println(antecedent.extent + "BAD !");
				// }
				bad++;
				System.out.println("==========");
				System.out.println("Error??? " + good + "/" + bad);
				if (zero.antecedent != null) {
					System.out.println(zero.antecedent.extent + ":"
							+ zero.antecedent.NE + "#" + zero.antecedent.number
							+ "#" + zero.antecedent.gender + "#"
							+ zero.antecedent.person + "#"
							+ zero.antecedent.animacy);
					System.out.println(zero);
					printResult(zero, zero.antecedent, part);
					System.out.println(overtPro + "#" + bestMSg);
					System.out.println("它: " + taMSg);
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

	protected void printResult(Mention zero, Mention systemAnte, CoNLLPart part) {
		StringBuilder sb = new StringBuilder();
		CoNLLSentence s = part.getWord(zero.start).sentence;
		CoNLLWord word = part.getWord(zero.start);
		for (int i = word.indexInSentence; i < s.words.size(); i++) {
			sb.append(s.words.get(i).word).append(" ");
		}
		System.out.println(sb.toString() + " # " + zero.start);
		System.out.println(systemAnte != null ? systemAnte.extent + "#"
				+ part.getWord(systemAnte.end + 1).word : "");

		// System.out.println("========");
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
		Datum<String, String> testIns = Dataset.svmLightLineToDatum(svm);
		// Datum<String, String> testIns =
		// EMUtil.svmlightToStanford(superFea.getFeas(), "-1");
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
		System.out.println("============");
		System.out.println("Hit: " + hit);
		System.out.println("Gold: " + gold);
		System.out.println("System: " + system);
		System.out.println("============");
		System.out.println("Recall: " + r * 100);
		System.out.println("Precision: " + p * 100);
		System.out.println("F-score: " + f * 100);
	}

	static ArrayList<String> corrects = new ArrayList<String>();

	public static void main(String args[]) {
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		run(args[0]);
		run("nw");
		run("mz");
		run("wb");
		run("bn");
		run("bc");
		run("tc");
	}

	public static void run(String folder) {
		EMUtil.train = false;
		ApplyEM test = new ApplyEM(folder);
		test.test();

		// System.out.println(EMUtil.missed);
		System.out.println(EMUtil.missed.size());

		Common.outputLines(goods, "goods");
		Common.outputLines(bads, "bas");

//		Common.outputHashMap(EMUtil.NEMap, "NEMAP");

//		Common.outputHashSet(Context.ss, "miniS");
//		Common.outputHashSet(Context.vs, "miniV");

		// System.out.println(Context.svoStat.unigramAll);
		// System.out.println(Context.svoStat.svoAll);

//		Common.outputLines(corrects, "EM.correct.all");
		Common.pause("!!#");
	}
}

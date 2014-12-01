package em;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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
import em.EMUtil.Grammatic;

public class ApplyEMNAACLRule {

	String folder;

	public ApplyEMNAACLRule(String folder) {
		this.folder = folder;
		EMUtil.loadPredictNE(folder, "dev");
	}

	double good = 0;
	double bad = 0;

	HashMap<String, Entity> entityCorefMap;

	public void test() {
		ArrayList<String> files = Common.getLines("chinese_list_" + folder
				+ "_development");

		String tag = "gold_conll";
		// String tag = "auto_conll";
		HashMap<String, HashMap<String, Entity>> entityCorefMaps = EMLearnNAACL
				.loadEntityCorefMap("dev_" + tag + ".coref");

		ArrayList<ArrayList<Mention>> corefResults = new ArrayList<ArrayList<Mention>>();
		ArrayList<ArrayList<Entity>> goldEntities = new ArrayList<ArrayList<Entity>>();

		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file.replace(
					"auto_conll", tag));
			OntoCorefXMLReader.addGoldZeroPronouns(document, false);

			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				entityCorefMap = entityCorefMaps.get(part.getPartName());

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
				for (Mention m : goldBoundaryNPMentions) {
					CoNLLSentence s = part.getWord(m.getStart()).sentence;
					s.mentions.add(m);
				}

				Collections.sort(goldBoundaryNPMentions);

				ArrayList<Mention> anaphorZeros = EMUtil.getAnaphorZeros(part
						.getChains());
				// ArrayList<Mention> anaphorZeros =
				// ZeroDetect.getHeuristicZeros(part);
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
			Collections.sort(allCandidates);

			ArrayList<Mention> cands = new ArrayList<Mention>();

			for (int h = allCandidates.size() - 1; h >= 0; h--) {
				Mention cand = allCandidates.get(h);
				if (((cand.end < zero.start && cand.end != -1) || (cand.end == -1 && cand.start < zero.start))
						&& zero.sentenceID - cand.sentenceID <= 2) {
					cands.add(cand);
				}
			}
			EMLearnNAACL.sortBySalience(cands, zero, part, entityCorefMap);

			if(RuleUtil.zeroStartsSentence(zero, part)) {
//				Recall: 8.873321657910099
//				Precision: 51.178451178451176
//				F-score: 15.124378109452739
//				Coverage: 17.338003502626968
				if (cands.size() != 0) {
					antecedent = cands.get(0);
				}
			}
			else if(RuleUtil.zeroAfterComma(zero, part)) {
//				Recall: 14.010507880910684
//				Precision: 42.47787610619469
//				F-score: 21.071115013169447
//				Coverage: 32.98307063631057
				if (cands.size() != 0) {
					antecedent = cands.get(0);
				}
			}
			else if(RuleUtil.zeroAfterVerb(zero, part)) {
//				Recall: 6.42148277875073
//				Precision: 43.30708661417323
//				F-score: 11.184544992374173
//				Coverage: 14.827787507297138
				if (cands.size() != 0) {
					antecedent = cands.get(0);
				}
			}
			else if(RuleUtil.zeroAfterNoun(zero, part)) {
//				Recall: 3.852889667250438
//				Precision: 46.15384615384615
//				F-score: 7.112068965517242
//				Coverage: 8.347927612375948
				if (cands.size() != 0) {
					antecedent = cands.get(0);
				}
			}  
			else if(RuleUtil.zeroAfterCC(zero, part)) {
//				Recall: 0.11675423234092236
//				Precision: 20.0
//				F-score: 0.2321532211259431
//				Coverage: 0.5837711617046117
				if (cands.size() != 0) {
					antecedent = cands.get(0);
				}
			}
			else if(RuleUtil.zeroAfterNN_P(zero, part)) {
				if (cands.size() != 0) {
					antecedent = cands.get(0);
				}
			}
			else if(RuleUtil.zeroOnlyAfterAD_P_CS(zero, part)) {
				if (cands.size() != 0) {
					antecedent = cands.get(0);
				}
			}
			else if(RuleUtil.zeroAfterQuota(zero, part)) {
				if (cands.size() != 0) {
					antecedent = cands.get(0);
				}
			}
			else {
				RuleUtil.printZero(zero, part);
			}
			
			

			CoNLLWord w = part.getWord(zero.start);
			CoNLLSentence s = w.getSentence();

			s.mentions.add(zero);
			Collections.sort(s.mentions);

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
				String key = part.docName + ":" + part.getPartID() + ":"
						+ zero.start + "-" + zero.antecedent.start + ","
						+ zero.antecedent.end + ":GOOD";
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
				// System.out.println("==========");
				// System.out.println("Correct!!! " + good + "/" + bad);
				// if (zero.antecedent != null) {
				// System.out.println(zero.antecedent.extent + ":"
				// + zero.antecedent.NE + "#" + zero.antecedent.number
				// + "#" + zero.antecedent.gender + "#"
				// + zero.antecedent.person + "#"
				// + zero.antecedent.animacy);
				// System.out.println(zero);
				// printResult(zero, zero.antecedent, part);
				// System.out.println(overtPro + "#");
				// }
				// System.out.println(overtPro + "#" + bestMSg);
				// System.out.println("它: " + taMSg);
				// }
				// }
			} else {
				if (zero.antecedent == null) {
					String key = part.docName + ":" + part.getPartID() + ":"
							+ zero.start + "-NULL:BAD";
				} else {
					String key = part.docName + ":" + part.getPartID() + ":"
							+ zero.start + "-" + zero.antecedent.start + ","
							+ zero.antecedent.end + ":BAD";
				}
				// if(antecedent!=null && antecedent.mType==MentionType.tmporal)
				// {
				// System.out.println(antecedent.extent + "BAD !");
				// }
				bad++;
				// System.out.println("==========");
				// System.out.println("Error??? " + good + "/" + bad);
				// if (zero.antecedent != null) {
				// System.out.println(zero.antecedent.extent + ":"
				// + zero.antecedent.NE + "#" + zero.antecedent.number
				// + "#" + zero.antecedent.gender + "#"
				// + zero.antecedent.person + "#"
				// + zero.antecedent.animacy);
				// System.out.println(zero);
				// printResult(zero, zero.antecedent, part);
				// System.out.println(overtPro + "#" + bestMSg);
				// System.out.println("它: " + taMSg);
				// }
			}
			String conllPath = file;
			int aa = conllPath.indexOf(anno);
			int bb = conllPath.indexOf(".");
			String middle = conllPath.substring(aa + anno.length(), bb);
			String path = prefix + middle + suffix;
			// System.out.println(path);
			// System.out.println("=== " + file);
			EMUtil.addEmptyCategoryNode(zero);

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
		
		System.out.println("Coverage: " + system/gold * 100);
	}

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
		ApplyEMNAACLRule test = new ApplyEMNAACLRule(folder);
		test.test();
		System.out.println(EMUtil.missed.size());
		Common.pause("!!#");
	}
}

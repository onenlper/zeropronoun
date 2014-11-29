package em;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import model.Element;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.CoNLL.OntoCorefXMLReader;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import util.Common;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.ling.Datum;
import em.EMUtil.Person;
import em.ResolveGroupNAACL.EntryNAACL;

public class EMLearnNAACL {

	// static HashMap<ContextNAACL, Double> p_context_ = new HashMap<ContextNAACL,
	// Double>();

	static Parameter numberP;
	static Parameter genderP;
	static Parameter personP;
	static Parameter personQP;
	static Parameter animacyP;

//	static Parameter gNumberP;
//	static Parameter gGenderP;
//	static Parameter gPersonP;
//	static Parameter gPersonQP;
//	static Parameter gAnimacyP;

	static HashMap<String, Double> contextPrior;
	static HashMap<String, Double> contextOverall;
	static HashMap<String, Double> fracContextCount;
	static List<Datum<String, String>> trainingData;
	static ArrayList<String> svmRanks;
	
	static double countl0 = 0;
	static double countl1 = 0;
	
	static double pl0 = 0;
	static double pl1 = 0;
	
	static ArrayList<HashMap<String, Double>> multiFracContextsCountl0 = new ArrayList<HashMap<String, Double>>();
	static ArrayList<HashMap<String, Double>> multiFracContextsCountl1 = new ArrayList<HashMap<String, Double>>();
	
	static ArrayList<HashMap<String, Double>> multiFracContextsProbl0 = new ArrayList<HashMap<String, Double>>();
	static ArrayList<HashMap<String, Double>> multiFracContextsProbl1 = new ArrayList<HashMap<String, Double>>();

	
//	static HashMap<String, Double> numberPrior = new HashMap<String, Double>();
//	static HashMap<String, Double> genderPrior = new HashMap<String, Double>();
//	static HashMap<String, Double> personPrior = new HashMap<String, Double>();
//	static HashMap<String, Double> personQPrior = new HashMap<String, Double>();
//	static HashMap<String, Double> animacyPrior = new HashMap<String, Double>();
	
	public static int qid = 0;

	static int count = 0;

	public static void init() {
		// static HashMap<ContextNAACL, Double> p_context_ = new HashMap<ContextNAACL,
		// Double>();
		numberP = new Parameter(1.0 / ((double) EMUtil.Number.values().length));
		numberP.subKeys.add(EMUtil.Number.single.name());
		numberP.subKeys.add(EMUtil.Number.plural.name());
		
		genderP = new Parameter(1.0 / ((double) EMUtil.Gender.values().length));
		genderP.subKeys.add(EMUtil.Gender.male.name());
		genderP.subKeys.add(EMUtil.Gender.female.name());
		genderP.subKeys.add(EMUtil.Gender.neuter.name());
		
		personP = new Parameter(1.0 / ((double) EMUtil.Person.values().length));
		personP.subKeys.add(EMUtil.Person.first.name());
		personP.subKeys.add(EMUtil.Person.second.name());
		personP.subKeys.add(EMUtil.Person.third.name());
		
		personQP = new Parameter(1.0 / ((double) EMUtil.Person.values().length));
		personQP.subKeys.add(EMUtil.Person.first.name());
		personQP.subKeys.add(EMUtil.Person.second.name());
		personQP.subKeys.add(EMUtil.Person.third.name());
		
		animacyP = new Parameter(
				1.0 / ((double) EMUtil.Animacy.values().length));
		animacyP.subKeys.add(EMUtil.Animacy.animate.name());
		animacyP.subKeys.add(EMUtil.Animacy.unanimate.name());

//		gNumberP = new Parameter();
//		gGenderP = new Parameter();
//		gPersonP = new Parameter();
//		gPersonQP = new Parameter();
//		gAnimacyP = new Parameter();

		contextPrior = new HashMap<String, Double>();
		contextOverall = new HashMap<String, Double>();
		fracContextCount = new HashMap<String, Double>();
		trainingData = new ArrayList<Datum<String, String>>();
		svmRanks = new ArrayList<String>();
		qid = 0;
		count = 0;
		ContextNAACL.contextCache.clear();
		
		
		for(int i=0;i<ContextNAACL.getSubContext().size();i++) {
			multiFracContextsCountl0.add(new HashMap<String, Double>());
			multiFracContextsCountl1.add(new HashMap<String, Double>());
			multiFracContextsProbl0.add(new HashMap<String, Double>());
			multiFracContextsProbl1.add(new HashMap<String, Double>());
		}
	}

	static FileWriter feaWriter;

	static FileWriter nbFeaWriter;

	static GuessPronounFea guessFea;

	static SuperviseFea superFea;
	static FileWriter superviseFw;

	public static void extractGuessPronounFea(Mention pronoun, CoNLLSentence s,
			CoNLLPart part) {
		try {
			guessFea.configure(pronoun.start - 1, pronoun.start + 1, s, part);
			int label = EMUtil.pronounList.indexOf(pronoun.extent) + 1;

			// label = pronoun.animacy.ordinal() + 1;

			String feaStr = guessFea.getSVMFormatString();
			feaWriter.write(label + " " + feaStr + "\n");

			String nbFeaStr = guessFea.getMyBNFormatString();
			nbFeaWriter.write((label - 1) + " " + nbFeaStr + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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

	public static ArrayList<ResolveGroupNAACL> extractGroups(CoNLLPart part) {
		// ArrayList<Element> goldNE = getChGoldNE(part);

		HashMap<String, Integer> chainMap = EMUtil.formChainMap(part
				.getChains());
		// System.out.println(chainMap.size());
		// System.out.println(part.getChains().size());

		ArrayList<ResolveGroupNAACL> groups = new ArrayList<ResolveGroupNAACL>();
		for (int i = 0; i < part.getCoNLLSentences().size(); i++) {
			CoNLLSentence s = part.getCoNLLSentences().get(i);

			ArrayList<Mention> precedMs = new ArrayList<Mention>();

			if (i >= 2) {
				precedMs.addAll(part.getCoNLLSentences().get(i - 2).mentions);
			}
			if (i >= 1) {
				precedMs.addAll(part.getCoNLLSentences().get(i - 1).mentions);
			}

			for (int j = 0; j < s.mentions.size(); j++) {
				Mention m = s.mentions.get(j);

				if (m.gram == EMUtil.Grammatic.subject
						&& EMUtil.pronouns.contains(m.extent)) {
					qid++;
					EMUtil.setPronounAttri(m, part);

					String proSpeaker = part.getWord(m.start).speaker;

					ArrayList<Mention> ants = new ArrayList<Mention>();
					ants.addAll(precedMs);
					if (j > 0) {
						ants.addAll(s.mentions.subList(0, j
								- 1
								));
					}
					ResolveGroupNAACL rg = new ResolveGroupNAACL(m.extent);
					Collections.sort(ants);
					Collections.reverse(ants);
					boolean findFirstSubj = false;
					// TODO

					for (Mention ant : ants) {
						ant.MI = ContextNAACL.calMI(ant, m);
						ant.isBest = false;
					}

					ApplyEM.findBest(m, ants);

					// System.out.println(m.toName() + " " +
					// m.s.getSentenceIdx());
					// System.out.println("-----");
					// for(Mention ant : ants) {
					// System.out.println(ant.toName() + " " +
					// ant.s.getSentenceIdx());
					// }
					// Common.pause("");

					for (int k = 0; k < ants.size(); k++) {
						Mention ant = ants.get(k);
						// add antecedents
						boolean fs = false;
						if (!findFirstSubj
								&& ant.gram == EMUtil.Grammatic.subject
						// && !ant.s.getWord(ant.headInS).posTag.equals("NT")
						) {
							findFirstSubj = true;
							fs = true;
						}

						String antSpeaker = part.getWord(ant.start).speaker;

						ContextNAACL context = ContextNAACL
								.buildContext(ant, m, part, fs);

						boolean sameSpeaker = proSpeaker.equals(antSpeaker);
						EntryNAACL entry = new EntryNAACL(ant, context, sameSpeaker, fs);
						
						entry.p_c = EMUtil.getP_C(ant, m, part, m.extent);
						
//						addOne(entry.number.name(), numberPrior);
//						addOne(entry.gender.name(), genderPrior);
//						if(sameSpeaker) {
//							addOne(entry.person.name(), personPrior);
//						} else {
//							addOne(entry.person.name(), personQPrior);
//						}
//						addOne(entry.animacy.name(), animacyPrior);
						
						rg.entries.add(entry);
						count++;
						Double d = contextPrior.get(context.toString());
						if (d == null) {
							contextPrior.put(context.toString(), 1.0);
						} else {
							contextPrior.put(context.toString(),
									1.0 + d.doubleValue());
						}
						boolean coref = chainMap.containsKey(m.toName())
								&& chainMap.containsKey(ant.toName())
								&& chainMap.get(m.toName()).intValue() == chainMap
										.get(ant.toName()).intValue();

//						double[] contextStat = contextSuper.get(context
//								.toString());
//						if (contextStat == null) {
//							contextStat = new double[2];
//							contextSuper.put(context.toString(), contextStat);
//						}
//						if (coref) {
//							contextStat[0]++;
//						} else {
//							contextStat[1]++;
//						}

						String pronoun = m.extent;

						if (coref) {
//							gNumberP.addFracCount(entry.number.name(), EMUtil
//									.getNumber(pronoun).name(), 1);
//							gGenderP.addFracCount(entry.gender.name(), EMUtil
//									.getGender(pronoun).name(), 1);
//							gAnimacyP.addFracCount(entry.animacy.name(), EMUtil
//									.getAnimacy(pronoun).name(), 1);
//							gNumberP.addFracCount(entry.head, EMUtil
//									.getNumber(pronoun).name(), 1);
//							gGenderP.addFracCount(entry.head, EMUtil
//									.getGender(pronoun).name(), 1);
//							gAnimacyP.addFracCount(entry.head, EMUtil
//									.getAnimacy(pronoun).name(), 1);
//							
//							if (sameSpeaker) {
//								gPersonP.addFracCount(entry.person.name(),
//										EMUtil.getPerson(pronoun).name(), 1);
//							} else {
//								gPersonQP.addFracCount(entry.person.name(),
//										EMUtil.getPerson(pronoun).name(), 1);
//							}
						}

//						addMaxEnt(chainMap, ant, m, context, sameSpeaker,
//								entry, part);
					}
					groups.add(rg);

//					try {
//						extractGuessPronounFea(m, s, part);
//					} catch (Exception e) {
//						System.out.println(s.getText());
//						System.out.println(m.extent);
//						System.exit(1);
//					}
				}
			}
		}
		return groups;
	}
	
	public static ArrayList<ResolveGroupNAACL> extractZeroGroups(CoNLLPart part) {
		ArrayList<ResolveGroupNAACL> groups = new ArrayList<ResolveGroupNAACL>();
		
		ArrayList<Mention> anaphorZeros = EMUtil.getAnaphorZeros(part
				.getChains());
//		ArrayList<Mention> anaphorZeros = ZeroDetect.getHeuristicZeros(part);
		Collections.sort(anaphorZeros);
		for(Mention m : anaphorZeros) {
			String proSpeaker = part.getWord(m.start).speaker;
			
			m.s = part.getWord(m.start).sentence;
			ArrayList<Mention> ants = new ArrayList<Mention>();
			int sid = m.s.getSentenceIdx(); 
			if(sid >= 2) {
				ants.addAll(part.getCoNLLSentences().get(sid-2).mentions);
			}
			if(sid >= 1) {
				ants.addAll(part.getCoNLLSentences().get(sid-1).mentions);
			}
			for(Mention ant : m.s.mentions) {
				if(ant.end<m.start) {
					ants.add(ant);
				}
			}
			
			EMUtil.assignVNode(m, part);
			String v = EMUtil.getFirstVerb(m.V);
			String o = EMUtil.getObjectNP(m.V);
			
			
			Collections.sort(ants);
			Collections.reverse(ants);
			boolean findFirstSubj = false;
			// TODO

			HashSet<String> genders = new HashSet<String>();
			HashSet<String> numbers = new HashSet<String>();
			HashSet<String> animacys = new HashSet<String>();
			HashSet<String> persons = new HashSet<String>();
			
			for (Mention ant : ants) {
				ant.MI = ContextNAACL.calMI(ant, m);
				ant.isBest = false;
				
				genders.add(EMUtil.getAntGender(ant).name());
				numbers.add(EMUtil.getAntNumber(ant).name());
				animacys.add(EMUtil.getAntAnimacy(ant).name());
				Person p = EMUtil.getAntPerson(ant.extent);
				
				String antSpeaker = part.getWord(ant.start).speaker;
				boolean sameSpeaker = proSpeaker.equals(antSpeaker);
				if(!sameSpeaker) {
					if(p==Person.first) {
						p = Person.second;
					} 
					if(p==Person.second) {
						p = Person.first;
					}
				}
				persons.add(p.name());
			}
			HashMap<String, Double> anaphorConfNumber = new HashMap<String, Double>();
			HashMap<String, Double> anaphorConfGender = new HashMap<String, Double>();
			HashMap<String, Double> anaphorConfPerson = new HashMap<String, Double>();
			HashMap<String, Double> anaphorConfAnimacy = new HashMap<String, Double>();
			
			anaphorConfNumber = ApplyEMNAACL.selectRestriction("number", 2, v, o, numbers);
			anaphorConfGender = ApplyEMNAACL.selectRestriction("gender", 3, v, o, genders);
			anaphorConfPerson = ApplyEMNAACL.selectRestriction("person", 3, v, o, persons);
			anaphorConfAnimacy = ApplyEMNAACL.selectRestriction("animacy", 2, v, o, animacys);

			ApplyEM.findBest(m, ants);
			for(Mention t : m.entity.mentions) {
				if(t.end==-1) {
					continue;
				}
				t.head = part.getWord(t.end).word;
				StringBuilder sb = new StringBuilder();
				for(int i=t.start;i<=t.end;i++) {
					sb.append(part.getWord(i).word);
				}
				t.extent = sb.toString();
			}
//			String numberGold = EMUtil.inferNumber(m.entity.mentions).name();
//			String genderGold = EMUtil.inferGender(m.entity.mentions).name();
//			String personGold = EMUtil.inferPerson(m.entity.mentions, m, part).name();
//			String animacyGold = EMUtil.inferAnimacy(m.entity.mentions).name();
//			anaphorConfNumber.clear();
//			anaphorConfGender.clear();
//			anaphorConfPerson.clear();
//			anaphorConfAnimacy.clear();
//			anaphorConfNumber.put(numberGold, 1.0);
//			anaphorConfGender.put(genderGold, 1.0);
//			anaphorConfPerson.put(personGold, 1.0);
//			anaphorConfAnimacy.put(animacyGold, 1.0);
			
			ResolveGroupNAACL rg = new ResolveGroupNAACL(anaphorConfAnimacy, anaphorConfGender, anaphorConfPerson,
					anaphorConfNumber);
			
			for (int k = 0; k < ants.size(); k++) {
				Mention ant = ants.get(k);
				// add antecedents
				boolean fs = false;
				if (!findFirstSubj
						&& ant.gram == EMUtil.Grammatic.subject
				// && !ant.s.getWord(ant.headInS).posTag.equals("NT")
				) {
					findFirstSubj = true;
					fs = true;
				}

				String antSpeaker = part.getWord(ant.start).speaker;

				ContextNAACL context = ContextNAACL
						.buildContext(ant, m, part, fs);

				boolean sameSpeaker = proSpeaker.equals(antSpeaker);
				EntryNAACL entry = new EntryNAACL(ant, context, sameSpeaker, fs);
				entry.p_c = EMUtil.getP_C(ant, m, part, m.extent);
				
//				addOne(entry.number.name(), numberPrior);
//				addOne(entry.gender.name(), genderPrior);
//				if(sameSpeaker) {
//					addOne(entry.person.name(), personPrior);
//				} else {
//					addOne(entry.person.name(), personQPrior);
//				}
//				addOne(entry.animacy.name(), animacyPrior);
				
				rg.entries.add(entry);
				count++;
				Double d = contextPrior.get(context.toString());
				if (d == null) {
					contextPrior.put(context.toString(), 1.0);
				} else {
					contextPrior.put(context.toString(),
							1.0 + d.doubleValue());
				}
			}
			groups.add(rg);
		}
		return groups;
	}

	private static void addOne(String key, HashMap<String, Double> map) {
		if (map.containsKey(key)) {
			map.put(key, map.get(key) + 1.0);
		} else {
			map.put(key, 1.0);
		}
	}
	
//	private static void addMaxEnt(HashMap<String, Integer> chainMap,
//			Mention ant, Mention m, ContextNAACL context, boolean sameSpeaker,
//			EntryNAACL entry, CoNLLPart part) {
//		boolean coref = chainMap.containsKey(m.toName())
//				&& chainMap.containsKey(ant.toName())
//				&& chainMap.get(m.toName()).intValue() == chainMap.get(
//						ant.toName()).intValue();
//
//		String pStr = "";
//		if (sameSpeaker) {
//			pStr = entry.person.name() + "="
//					+ EMUtil.getPerson(m.extent).name();
//		} else {
//			pStr = entry.person.name() + "!="
//					+ EMUtil.getPerson(m.extent).name();
//		}
//		String nStr = entry.number.name() + "="
//				+ EMUtil.getNumber(m.extent).name();
//		String gStr = entry.gender.name() + "="
//				+ EMUtil.getGender(m.extent).name();
//		String aStr = entry.animacy.name() + "="
//				+ EMUtil.getAnimacy(m.extent).name();
//
//		superFea.configure(pStr, nStr, gStr, aStr, context, m, ant, part);
//
//		String svm = superFea.getSVMFormatString();
//		String label = "";
//		if (coref) {
//			label = "+1";
//			svmRanks.add("2 qid:" + qid + " " + svm);
//		} else {
//			label = "-1";
//			svmRanks.add("1 qid:" + qid + " " + svm);
//		}
//		svm = label + " " + svm;
//		try {
//			superviseFw.write(svm + "\n");
//		} catch (Exception e) {
//			e.printStackTrace();
//			Common.bangErrorPOS("!");
//		}
//		// TODO
//		trainingData.add(Dataset.svmLightLineToDatum(svm));
//		ArrayList<String> feas = superFea.getFeas();
//		// if (!feas.get(feas.size() - 1).startsWith("0")) {
//		// if (coref) {
//		// match++;
//		// }
//		// XallX++;
//		// }
//		// trainingData.add(EMUtil.svmlightToStanford(superFea.getFeas(),
//		// label));
//	}

	private static void extractCoNLL(ArrayList<ResolveGroupNAACL> groups) {
//		 CoNLLDocument d = new CoNLLDocument("train_auto_conll");
//		CoNLLDocument d = new CoNLLDocument("train_gold_conll");
		System.out.println("READ IN>>>");

		int docNo = 0;
		String previousDoc = "";
		int partNo = 0;
		ArrayList<String> files = Common.getLines("chinese_list_all_train");
		for (String file : files) {
//			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file
					.replace("auto_conll", "gold_conll")
					);
			OntoCorefXMLReader.addGoldZeroPronouns(document, false);
		
			for (CoNLLPart part : document.getParts()) {
				
				
				for (int i = 0; i < part.getCoNLLSentences().size(); i++) {
					CoNLLSentence s = part.getCoNLLSentences().get(i);
					s.mentions = EMUtil.extractMention(s);

					EMUtil.assignNE(s.mentions, part.getNameEntities());
				}
				
				partNo += 1;
				// System.out.println(part.docName + " " + part.getPartID());
				if (!part.docName.equals(previousDoc)) {
					docNo++;
					previousDoc = part.docName;
				}
//				if (docNo % 10 < percent) {
				groups.addAll(extractGroups(part));
				
//				if(partNo%1==0)
//				groups.addAll(extractZeroGroups(part));
			}
		}
		
		for(ResolveGroupNAACL group : groups) {
			countl0 += group.entries.size() - 1;
			countl1 += 1;
		}
		
		pl0 = countl0/(countl0 + countl1);
		pl1 = countl1/(countl0 + countl1);
		
		
//		buildPrior(numberPrior);
//		buildPrior(genderPrior);
//		buildPrior(personPrior);
//		buildPrior(personQPrior);
//		buildPrior(animacyPrior);
	}
	
	private static void buildPrior(HashMap<String, Double> priorMap) {
		double all = 0;
		for (String key : priorMap.keySet()) {
			all += priorMap.get(key);
		}
		for(String key : priorMap.keySet()) {
			priorMap.put(key, priorMap.get(key) / all);
		}
	}

	static int percent = 0;

	private static void extractGigaword(ArrayList<ResolveGroupNAACL> groups)
			throws Exception {

		String folder = "/users/yzcchen/chen3/zeroEM/parser/";
		int j = 0;
		ArrayList<String> fns = new ArrayList<String>();
		for (File subFolder : (new File(folder)).listFiles()) {
			if (subFolder.isDirectory()
			// && !subFolder.getName().contains("cna")
			) {
				for (File file : subFolder.listFiles()) {
					if (file.getName().endsWith(".text")) {
						String filename = file.getAbsolutePath();
						fns.add(filename);
					}
				}
			}
		}

		for (String filename : fns) {
			System.out.println(filename + " " + (j++));
			System.out.println(groups.size());
			BufferedReader br = new BufferedReader(new FileReader(filename));
			CoNLLPart part = new CoNLLPart();
			int wID = 0;
			String line = "";
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) {
					// part.setDocument(doc);
					// doc.getParts().add(part);
					part.wordCount = wID;
					part.processDocDiscourse();

					// for(CoNLLSentence s : part.getCoNLLSentences()) {
					// for(CoNLLWord w : s.getWords()) {
					// if(!w.speaker.equals("-") &&
					// !w.speaker.startsWith("PER")) {
					// System.out.println(w.speaker);
					// }
					// }
					// }
					groups.addAll(extractGroups(part));
					part = new CoNLLPart();
					wID = 0;
					continue;
				}
				MyTree tree = Common.constructTree(line);
				CoNLLSentence s = new CoNLLSentence();
				part.addSentence(s);
				s.setStartWordIdx(wID);
				s.syntaxTree = tree;
				ArrayList<MyTreeNode> leaves = tree.leaves;
				for (int i = 0; i < leaves.size(); i++) {
					MyTreeNode leaf = leaves.get(i);
					CoNLLWord word = new CoNLLWord();
					word.orig = leaf.value;
					word.word = leaf.value;
					word.sentence = s;
					word.indexInSentence = i;
					word.index = wID++;
					word.posTag = leaf.parent.value;

					// find speaker
					word.speaker = "-";

					s.addWord(word);
				}
				s.setEndWordIdx(wID - 1);
			}
			part.processDocDiscourse();
			groups.addAll(extractGroups(part));
			br.close();
		}
	}

	public static void estep(ArrayList<ResolveGroupNAACL> groups) {
		for (ResolveGroupNAACL group : groups) {
			double norm = 0;
			for (EntryNAACL entry : group.entries) {
				ContextNAACL context = entry.context;
				double p_person = 0;
				if (entry.sameSpeaker) {
					p_person = ApplyEMNAACL.getProb2(group.personConf, personP, entry.person.name());
					
				} else {
					p_person = ApplyEMNAACL.getProb2(group.personConf, personQP, entry.person.name());
				}

				double p_number = ApplyEMNAACL.getProb2(group.numberConf, numberP, entry.number.name());
				double p_gender = ApplyEMNAACL.getProb2(group.genderConf, genderP, entry.gender.name());
				double p_animacy = ApplyEMNAACL.getProb2(group.animacyConf, animacyP, entry.animacy.name());
				
				double p_context = 1;

				if (fracContextCount.containsKey(context.toString())) {
					p_context = (EMUtil.alpha + fracContextCount.get(context
							.toString()))
							/ (2 * EMUtil.alpha + contextPrior.get(context
									.toString()));
				} else {
//					p_context = 1.0 / 2592.0;
					p_context = 1.0 / 2;
				}
				
				double p_context_l1 = pl1;
				double p_context_l0 = pl0;
				
				for(int i=0;i<ContextNAACL.getSubContext().size();i++) {
					int pos[] = ContextNAACL.getSubContext().get(i);
					String key = context.getKey(i);
					if(key.equals("-")) {
						System.out.println(context.toString());
						Common.bangErrorPOS("!!!");
					}
					if(multiFracContextsProbl1.get(i).containsKey(key)) {
						p_context_l1 *= multiFracContextsProbl1.get(i).get(key);
					} else {
						p_context_l1 *= ContextNAACL.normConstant.get(i);
					}
					
					if(multiFracContextsProbl0.get(i).containsKey(key)) {
						p_context_l0 *= multiFracContextsProbl0.get(i).get(key);
					} else {
						p_context_l0 *= ContextNAACL.normConstant.get(i);
					}
				}
//				p_context = p_context_l1/(p_context_l1 + p_context_l0);

				// System.out.println(p_context);

				entry.p = 1 * entry.p_c * 
						p_person * 
						p_number * 
						p_gender * 
						p_animacy *
						p_context * 
						1;
				
				norm += entry.p;
			}

			for (EntryNAACL entry : group.entries) {
				entry.p = entry.p / norm;
			}
		}
	}

	public static void mstep(ArrayList<ResolveGroupNAACL> groups) {
		genderP.resetCounts();
		numberP.resetCounts();
		animacyP.resetCounts();
		personP.resetCounts();
		personQP.resetCounts();
		fracContextCount.clear();

		for(int i=0;i<multiFracContextsCountl1.size();i++) {
			multiFracContextsCountl0.get(i).clear();
			multiFracContextsCountl1.get(i).clear();
			multiFracContextsProbl0.get(i).clear();
			multiFracContextsProbl1.get(i).clear();
		}
		
		for (ResolveGroupNAACL group : groups) {
			for (EntryNAACL entry : group.entries) {
				double p = entry.p;
				ContextNAACL context = entry.context;
				
				for(String aa : group.numberConf.keySet()) {
					double pa = group.numberConf.get(aa);
					numberP.addFracCount(entry.number.name(), aa, pa * p);	
				}
				
				for(String aa : group.genderConf.keySet()) {
					double pa = group.genderConf.get(aa);
					genderP.addFracCount(entry.gender.name(), aa, pa * p);
				}
				
				for(String aa : group.animacyConf.keySet()) {
					double pa = group.animacyConf.get(aa);
					animacyP.addFracCount(entry.animacy.name(), aa, pa * p);
				}
				
				
				if (entry.sameSpeaker) {
					for(String aa : group.personConf.keySet()) {
						double pa = group.personConf.get(aa);
						personP.addFracCount(entry.person.name(), aa, pa * p);
					}
				} else {
					for(String aa : group.personConf.keySet()) {
						double pa = group.personConf.get(aa);
						personQP.addFracCount(entry.person.name(), aa, pa * p);
					}
				}

				Double d = fracContextCount.get(context.toString());
				if (d == null) {
					fracContextCount.put(context.toString(), p);
				} else {
					fracContextCount.put(context.toString(), d.doubleValue() + p);
				}
				
				for(int i=0;i<ContextNAACL.getSubContext().size();i++) {
					int ps[] = ContextNAACL.getSubContext().get(i);
					String key = context.getKey(i);
					double l1 = p;
					double l0 = 1 - p;
					
					Double cl0 = multiFracContextsCountl0.get(i).get(key); 
					if(cl0==null) {
						multiFracContextsCountl0.get(i).put(key, l0);
					} else {
						multiFracContextsCountl0.get(i).put(key, l0 + cl0.doubleValue());
					}
					
					Double cl1 = multiFracContextsCountl1.get(i).get(key); 
					if(cl1==null) {
						multiFracContextsCountl1.get(i).put(key, l1);
					} else {
						multiFracContextsCountl1.get(i).put(key, l1 + cl1.doubleValue());
					}
				}
			}
		}
		genderP.setVals();
		numberP.setVals();
		animacyP.setVals();
		personP.setVals();
		personQP.setVals();
		
		for(int i=0;i<ContextNAACL.getSubContext().size();i++) {

			for(String key : multiFracContextsCountl1.get(i).keySet()) {
				
				double contextcountl0 = 1;
				if(multiFracContextsCountl0.get(i).containsKey(key)) {
					contextcountl0 += multiFracContextsCountl0.get(i).get(key);
				}
				double pcountl0 = contextcountl0/(countl0 + ContextNAACL.normConstant.get(i));
				
				double contextcountl1 = 1;
				if(multiFracContextsCountl1.get(i).containsKey(key)) {
					contextcountl1 += multiFracContextsCountl1.get(i).get(key);
				}
				double pcountl1 = contextcountl1/(countl1 + ContextNAACL.normConstant.get(i));
				
				multiFracContextsProbl0.get(i).put(key, pcountl0);
				multiFracContextsProbl1.get(i).put(key, pcountl1);
			}
		}
	}

	public static void main(String args[]) throws Exception {
		// percent = 0;
		// while(percent<=9) {
		// run();
		// percent++;
		// }
		percent = 10;
		run();
		// System.out.println(match/XallX);
		// Common.outputLines(svmRanks, "svmRank.train");
		// System.out.println("Qid: " + qid);
	}

	private static void run() throws IOException, FileNotFoundException {
		init();

		EMUtil.train = true;
		feaWriter = new FileWriter("guessPronoun.train.svm");
		nbFeaWriter = new FileWriter("guessPronoun.train.nb");
		guessFea = new GuessPronounFea(true, "guessPronoun");

		superFea = new SuperviseFea(true, "supervise");
		superviseFw = new FileWriter("supervise.train");

		ArrayList<ResolveGroupNAACL> groups = new ArrayList<ResolveGroupNAACL>();

		extractCoNLL(groups);
		// extractGigaword(groups);
		// Common.pause("count:  " + count);
		Common.pause(groups.size());

		HashMap<String, Double> map = new HashMap<String, Double>();
		for(ResolveGroupNAACL rg : groups) {
			String pr = rg.pronoun;
			Double i = map.get(pr);
			if(i==null) {
				map.put(pr, 1.0);
			} else {
				map.put(pr, i.doubleValue() + 1.0);
			}
		}
		for(String key : map.keySet()) {
			System.out.println(key + ":" + map.get(key) + "=" + map.get(key)/groups.size());
		}
		
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(
				"resolveGroups"));
		out.writeObject(groups);
		out.close();

		guessFea.freeze();
		guessFea.clear();
		nbFeaWriter.close();
		feaWriter.close();

		superFea.freeze();
		superviseFw.close();

		int it = 0;
		while (it < 20) {
			System.out.println("Iteration: " + it);
			estep(groups);
			mstep(groups);
			it++;
		}

		numberP.printParameter("numberP");
		genderP.printParameter("genderP");
		animacyP.printParameter("animacyP");
		personP.printParameter("personP");
		personQP.printParameter("personQP");

//		gGenderP.setVals();
//		gNumberP.setVals();
//		gAnimacyP.setVals();
//		gPersonP.setVals();
//		gPersonQP.setVals();
//
//		gNumberP.printParameter("gnumberP");
//		gGenderP.printParameter("ggenderP");
//		gAnimacyP.printParameter("ganimacyP");
//		gPersonP.printParameter("gpersonP");
//		gPersonQP.printParameter("gpersonQP");

		ObjectOutputStream modelOut = new ObjectOutputStream(
				new FileOutputStream("EMModel"));
		modelOut.writeObject(numberP);
		modelOut.writeObject(genderP);
		modelOut.writeObject(animacyP);
		modelOut.writeObject(personP);
		modelOut.writeObject(personQP);

		// modelOut.writeObject(gNumberP);
		// modelOut.writeObject(gGenderP);
		// modelOut.writeObject(gAnimacyP);
		// modelOut.writeObject(gPersonP);
		// modelOut.writeObject(gPersonQP);

		modelOut.writeObject(fracContextCount);
		modelOut.writeObject(contextPrior);

		modelOut.writeObject(ContextNAACL.ss);
		modelOut.writeObject(ContextNAACL.vs);
		
		modelOut.writeObject(multiFracContextsProbl0);
		modelOut.writeObject(multiFracContextsProbl1);
		modelOut.writeObject(pl0);
		modelOut.writeObject(pl1);
		
		for(int i=0;i<multiFracContextsProbl1.size();i++) {
			ArrayList<String> output = new ArrayList<String>();
			for(String key : multiFracContextsProbl1.get(i).keySet()) {
				output.add(key + ":\t" + multiFracContextsProbl1.get(i).get(key));
			}
			Common.outputLines(output, "probl1_.sub" + i);
		}
		
		for(int i=0;i<multiFracContextsProbl0.size();i++) {
			ArrayList<String> output = new ArrayList<String>();
			for(String key : multiFracContextsProbl0.get(i).keySet()) {
				output.add(key + ":\t" + multiFracContextsProbl0.get(i).get(key));
			}
			Common.outputLines(output, "probl0_.sub" + i);
		}
		// modelOut.writeObject(ContextNAACL.svoStat);

//		modelOut.writeObject(numberPrior);
//		modelOut.writeObject(genderPrior);
//		modelOut.writeObject(personPrior);
//		modelOut.writeObject(personQPrior);
//		modelOut.writeObject(animacyPrior);

		modelOut.close();

		// ObjectOutputStream svoStat = new ObjectOutputStream(new
		// FileOutputStream(
		// "/dev/shm/svoStat"));
		// svoStat.writeObject(ContextNAACL.svoStat);
		// svoStat.close();

		// System.out.println(EMUtil.missed);
		System.out.println(EMUtil.missed.size());

		LinearClassifierFactory<String, String> factory = new LinearClassifierFactory<String, String>();
		factory.useConjugateGradientAscent();
		// Turn on per-iteration convergence updates
		factory.setVerbose(false);
		// Small amount of smoothing
		factory.setSigma(10);
		// Build a classifier
		// Build a classifier
		LinearClassifier<String, String> classifier = factory
				.trainClassifier(trainingData);
		classifier.dump();
		LinearClassifier.writeClassifier(classifier, "stanfordClassifier.gz");

		ApplyEMNAACL.run("all");

		ApplyEMNAACL.run("nw");
		ApplyEMNAACL.run("mz");
		ApplyEMNAACL.run("wb");
		ApplyEMNAACL.run("bn");
		ApplyEMNAACL.run("bc");
		ApplyEMNAACL.run("tc");
	}

}

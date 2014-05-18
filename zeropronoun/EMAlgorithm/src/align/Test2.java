package align;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import mentionDetect.ParseTreeMention;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.CoNLL.OntoCorefXMLReader;
import util.Common;
import align.DocumentMap.SentForAlign;
import align.DocumentMap.Unit;
import em.EMUtil;

public class Test2 {

	public static void main(String args[]) {
		CoNLLPart.processDiscourse = false;
		String setting = "setting1";

		ArrayList<SentForAlign[]> alignCache = DocumentMap
				.loadRealBAAlignResult("/users/yzcchen/chen3/zeroEM/EMAlgorithm/src/allSetting/" + "/ba/");
		EMUtil.loadPredictNE("all", "dev");
		System.out.println("Done.");
		CoNLLDocument engDoc = new CoNLLDocument(setting + "/conll/eng.conll");
		System.out.println("Done2.");
		System.out.println(alignCache.size() + "@#"
				+ engDoc.getParts().get(0).getCoNLLSentences().size());

		ParseTreeMention ptm = new ParseTreeMention();

		System.out.println("Done3.");
		int segID = 0;
		ArrayList<String> lines = Common
				.getLines("chinese_list_all_development");
		
		double allM =0;
		double mapM = 0;
		
		for (int g = 0; g < lines.size(); g++) {
			String l = lines.get(g);
			CoNLLDocument chiDoc = new CoNLLDocument(l.replace("auto_conll",
					"gold_conll"));
			OntoCorefXMLReader.addGoldZeroPronouns(chiDoc, false);

			for (CoNLLPart part : chiDoc.getParts()) {

				ArrayList<Mention> anaphorZeros = EMUtil.getAnaphorZeros(part
						.getChains());
				// ArrayList<Mention> anaphorZeros =
				// ZeroDetect.getHeuristicZeros(part);

				anaphorZeros = EMUtil.removeDuplcate(anaphorZeros);

				for (CoNLLSentence chiS : part.getCoNLLSentences()) {
					int start = chiS.getWord(0).index;
					int end = chiS.getWord(chiS.getWords().size() - 1).index;
					ArrayList<Mention> chiMentionsInS = ptm.getMentions(chiS);
					Collections.sort(chiMentionsInS);

					ArrayList<Mention> zerosInS = EMUtil.getInBetweenMention(
							anaphorZeros, start, end);
					ArrayList<ArrayList<CoNLLWord>> wordsArr = EMUtil.split(chiS,
							zerosInS);

					for (ArrayList<CoNLLWord> words : wordsArr) {
						int segStart = words.get(0).index;
						int segEnd = words.get(words.size() - 1).index;
						ArrayList<Mention> zeros = EMUtil.getInBetweenMention(
								zerosInS, segStart, segEnd);
						Collections.reverse(zeros);

						ArrayList<ArrayList<CoNLLWord>> combinations = EMUtil
								.getAllPossibleFill(words, zeros);

						ArrayList<Mention> chMentions = EMUtil
								.getInBetweenMention(chiMentionsInS, segStart, segEnd);
						
						for (ArrayList<CoNLLWord> combination : combinations) {
							int offset = 0;
							HashMap<Integer, Integer> offsetMap = new HashMap<Integer, Integer>();
							for(CoNLLWord w : combination) {
								if(w.isZeroWord) {
									offset++;
								}
								offsetMap.put(w.index, offset);
							}
							
							// clear chi's xMention
							for (Mention cm : chMentions) {
								cm.units.clear();
							}

							String str = EMUtil.listToString(combination);
							SentForAlign[] align = alignCache.get(segID);
							CoNLLSentence engS = engDoc.getParts().get(0)
									.getCoNLLSentences().get(segID);
							
							int engStart = engS.getWords().get(0).index;
							
							ArrayList<Mention> engMentions = ptm
									.getMentions(engS);
							// register eng mentions
							for (Mention em : engMentions) {
//								StringBuilder sb = new StringBuilder();
								for (int i = em.start - engStart; i <= em.end - engStart; i++) {
									Unit unit = align[1].units.get(i);
									unit.sentence = engS;
//									sb.append(unit.getToken()).append(" ");
									unit.addMention(em);
									em.units.add(unit);
								}
//								if(!em.extent.trim().equalsIgnoreCase(sb.toString().trim())) {
//									System.out.println("#" + sb.toString().trim() + "#"+ em.extent.trim() + "#");
//									Common.bangErrorPOS("");
//								}
							}
							// register chi mentions
							for (Mention em : chMentions) {
								int from = em.start-segStart+offsetMap.get(em.start);
								int to = em.end-segStart+offsetMap.get(em.end);
								for (int i = from; i <= to; i++) {
									Unit unit = align[0].units.get(i);
									unit.sentence = chiS;
									unit.addMention(em);
									em.units.add(unit);
								}
							}

							if (segID % 1000 == 0) {
								System.out.println(segID);
								System.out.println(mapM/allM * 100 + "%");
							}
							for (int i = 1; i <= 4; i++) {
								Mention.assignMode = i;
								for (Mention m : engMentions) {
									m.getXSpan();
								}
								for (Mention m : chMentions) {
									m.getXSpan();
								}
							}
							allM += chMentions.size();
							for(Mention m : chMentions) {
								Mention xm = m.getXSpan();
								if(xm!=null) {
//									System.out.println(m.extent + "#" + xm.extent);
									mapM++;
								}
							}

							segID++;
						}

					}
				}
			}
			segID++;
		}
		System.out.println(segID);
	}

}

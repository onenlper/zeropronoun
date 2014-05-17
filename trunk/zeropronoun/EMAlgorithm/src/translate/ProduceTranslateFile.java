package translate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.CoNLL.OntoCorefXMLReader;
import util.Common;
import em.EMUtil;
import em.ZeroDetect;

public class ProduceTranslateFile {

	static HashMap<String, String> transMap = new HashMap<String, String>();
	
	private static void loadTran() {
		ArrayList<String> chi = Common.getLines("moreTrans/moreTranslate");
		chi.addAll(Common.getLines("toTranslate/tran.gold"));
		
		ArrayList<String> eng = Common.getLines("en.moretrans");
		eng.addAll(Common.getLines("en.gold"));
		
		for(int i=0;i<chi.size();i++) {
			String c = chi.get(i);
			String e = eng.get(i);
			transMap.put(c.trim(), e.trim());
		}
	}
	
	public static void main(String args[]) {
		loadTran();
		System.out.println(transMap.size());
		ArrayList<String> files = Common
				.getLines("chinese_list_all_development");
		
		ArrayList<String> chi = new ArrayList<String>();
		ArrayList<String> eng = new ArrayList<String>();
		
		for (String file : files) {
			CoNLLDocument doc = new CoNLLDocument(file
					.replace("auto_conll", "gold_conll")
					);
			OntoCorefXMLReader.addGoldZeroPronouns(doc, false);
			for (CoNLLPart part : doc.getParts()) {
				ArrayList<Mention> allZeros = EMUtil.getAnaphorZeros(part
						.getChains());
				
//				ArrayList<Mention> allZeros = ZeroDetect.getHeuristicZeros(part);
				
				HashSet<Mention> zeroSet = new HashSet<Mention>(allZeros);
				allZeros = new ArrayList<Mention>(zeroSet);
				Collections.sort(allZeros);
				for (CoNLLSentence s : part.getCoNLLSentences()) {
					int start = s.getWord(0).index;
					int end = s.getWord(s.getWords().size() - 1).index;

					ArrayList<Mention> zerosInS = EMUtil.getProperZeros(allZeros,
							start, end);
					Collections.sort(zerosInS);

					ArrayList<ArrayList<CoNLLWord>> groups = EMUtil.split(s, zerosInS);

					for (int i=0;i<groups.size();i++) {
						ArrayList<CoNLLWord> group = groups.get(i);
						ArrayList<Mention> zeroInGroup = EMUtil.getProperZeros(
								zerosInS, group.get(0).index,
								group.get(group.size() - 1).index);
						Collections.sort(zeroInGroup);
						Collections.reverse(zeroInGroup);

						
						
						
						ArrayList<ArrayList<CoNLLWord>> combinations = EMUtil.getAllPossibleFill(
								group, zeroInGroup);
						
						
						for(ArrayList<CoNLLWord> comb : combinations) {
							String str = EMUtil.listToString(comb);
//							System.out.println(str);
							chi.add(str);
							
							String e = transMap.get(str);
							if(e==null) {
								Common.bangErrorPOS("!!");
							}
							eng.add(e);
						}
					}

				}
			}
			chi.add("============");
			eng.add("============");
		}
//		Common.outputLines(chi, "setting3.chi");
//		Common.outputLines(eng, "setting3.eng");
	}

}

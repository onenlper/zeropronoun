package align;

import java.util.ArrayList;
import java.util.Collections;

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
				.loadRealBAAlignResult(setting + "/ba/");

		System.out.println("Done.");
		CoNLLDocument engDoc = new CoNLLDocument(setting + "/conll/eng.conll");
		System.out.println("Done2.");
		System.out.println(alignCache.size() + "@#" + engDoc.getParts().get(0).getCoNLLSentences().size());
		
		ParseTreeMention ptm = new ParseTreeMention();
//		ArrayList<Mention> allEnglishMentions = ptm.getMentions(engDoc.getParts().get(0));
//		
//		// register to unit
//		for(Mention em : allEnglishMentions) {
//			int sid = em.s.getSentenceIdx();
//			System.out.println(sid + ":" + em.start + "." + em.end);
//			for (int i = em.start; i <= em.end; i++) {
//				Unit unit = alignCache.get(sid)[1].units.get(i);
//				if (unit != null) {
//					unit.addMention(em);
//				}
//			}
//		}
		System.out.println("Done3.");
		int segID = 0;
		ArrayList<String> lines = Common.getLines("chinese_list_all_development");
		for (int g = 0;g<lines.size();g++) {
			String l = lines.get(g);
			CoNLLDocument chiDoc = new CoNLLDocument(l
					.replace("auto_conll", "gold_conll")
					);
			OntoCorefXMLReader.addGoldZeroPronouns(chiDoc, false);

			for (CoNLLPart part : chiDoc.getParts()) {

//				ArrayList<Mention> goldBoundaryNPMentions = ptm
//						.getMentions(part);
				
				ArrayList<Mention> anaphorZeros = EMUtil.getAnaphorZeros(part
						.getChains());
//				ArrayList<Mention> anaphorZeros = ZeroDetect.getHeuristicZeros(part);

				anaphorZeros = EMUtil.removeDuplcate(anaphorZeros);
				
				for (CoNLLSentence s : part.getCoNLLSentences()) {
					int start = s.getWord(0).index;
					int end = s.getWord(s.getWords().size() - 1).index;

					ArrayList<Mention> zerosInS = EMUtil.getProperZeros(
							anaphorZeros, start, end);
					ArrayList<ArrayList<CoNLLWord>> wordsArr = EMUtil.split(s,
							zerosInS);

					for (ArrayList<CoNLLWord> words : wordsArr) {
						ArrayList<Mention> zeros = EMUtil.getProperZeros(
								zerosInS, words.get(0).index,
								words.get(words.size() - 1).index);
						Collections.reverse(zeros);
						
						ArrayList<ArrayList<CoNLLWord>> combinations = EMUtil.getAllPossibleFill(
								words, zeros);
						
						for(ArrayList<CoNLLWord> combination : combinations) {
							String str = EMUtil.listToString(combination);
							
							CoNLLSentence engS = engDoc.getParts().get(0).getCoNLLSentences().get(segID);
//							ArrayList<Mention> chiMentions = 
//							ArrayList<Mention> engMentions = 
							SentForAlign[] align = alignCache.get(segID);

							if(!align[1].getText().equals(engS.getText()) || !str.equals(align[0].getText())) {
								System.out.println(align[1].getText() + "#" + align[1].getText().length());
								System.out.println(engS.getText() + "#" + engS.getText().length());
//								for(int i=0;i<align[1].getText().length()&&i<engS.getText().length();i++) {
//									System.out.println(align[1].getText().charAt(i) + "#" + engS.getText().charAt(i) + "#"
//											+ (align[1].getText().charAt(i)==engS.getText().charAt(i))	);
//								}
//								System.out.println(align[1].equals(engS.getText()));
//								
								System.out.println(str + "#");
								System.out.println(align[0].getText() + "#");
								System.out.println(str.equals(align[0].getText()));
								System.out.println(g);
								Common.bangErrorPOS("");
							}
							
							if(engS==null || align==null) { 
								System.out.println(s.getText());
								Common.bangErrorPOS(str);
							}
							segID ++;
						}
						
					}
				}
			}
			segID++;
		}
		System.out.println(segID);
	}

}

package em;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import model.Entity;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLWord;
import model.CoNLL.OntoCorefXMLReader;
import util.Common;

public class Stat {

	public static void main(String args[]) {
		String folder = "all";
		ArrayList<String> files = Common.getLines("chinese_list_" + folder
				+ "_development");
		double all = 0;
		double exe = 0;
		HashSet<String> verbs = new HashSet<String>();
		for (String file : files) {
			System.out.println(file);
			CoNLLDocument document = new CoNLLDocument(file.replace(
					"auto_conll", "gold_conll"));
			OntoCorefXMLReader.addGoldZeroPronouns(document, false);
			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
				ArrayList<Mention> anaphorZeros = EMUtil.getAnaphorZeros(part
						.getChains());
				all += anaphorZeros.size();
				for(Mention z : anaphorZeros) {
					CoNLLWord w = part.getWord(z.start);
					if(w.indexInSentence!=0) {
						CoNLLWord w_1 = part.getWord(w.index-1);
						if(w_1.posTag.startsWith("VV") && !w_1.word.trim().equals("是")) {
							System.out.println("#" + w_1.word + "#" + w_1.posTag);
							exe++;
							verbs.add(w_1.word);
						}
					}
				}
			}
		}
		Common.outputHashSet(verbs, "verbs");
		System.out.println(exe + "/" + all + "=" + exe/all);
	}
}

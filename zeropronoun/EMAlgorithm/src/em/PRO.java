package em;

import java.util.*;
import util.Common;
import model.CoNLL.*;
import model.Mention;
import model.syntaxTree.*;

public class PRO {

	public static void main(String args[]) {

		ArrayList<String> files = Common.getLines("chinese_list_all_train");		
//		ArrayList<String> files2 = Common.getLines("chinese_list_all_development");
//		files.addAll(files2);
		int allPro = 0;
		int good = 0;
		for(String file : files) {
			CoNLLDocument document = new CoNLLDocument(file.replace("auto_conll", "gold_conll"));
			OntoCorefXMLReader.addGoldZeroPronouns(document, true);
			for (int k = 0; k < document.getParts().size(); k++) {
				CoNLLPart part = document.getParts().get(k);
			
				ArrayList<Mention> zeros = EMUtil.getZeros(part.getChains());
				for(Mention m : zeros) {
					CoNLLWord w = part.getWord(m.start);
					CoNLLSentence s = w.sentence;
					MyTree tree = s.getSyntaxTree();
					MyTreeNode node = tree.root.getLeaves().get(w.indexInSentence);
					
					ArrayList<MyTreeNode> ancestors = node.getAncestors();
					boolean beforeVP = false;
					for(MyTreeNode tmp : ancestors) {
						if(tmp.getLeaves().get(0) == node && tmp.value.equals("VP")) {
							beforeVP = true;
							break;
						}
					}
					if(beforeVP) {
						good++;
					}
				}
				allPro += zeros.size();			
			}
		}
		System.out.println(allPro);
		System.out.println(good);
		System.out.println(good*1.0/(allPro*1.0));
	}
}

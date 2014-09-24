package align;

import java.util.ArrayList;

import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import util.Common;
import em.EMUtil;

public class Test3 {

	public static void main(String args[]) {
//		CoNLLDocument doc = new CoNLLDocument("MT.chiCoNLL.all");
//		System.out.println(doc.getParts().size());
//		HashSet<String> set1 = new HashSet<String>();
//		for(CoNLLPart part : doc.getParts()) {
//			System.out.println(part.getPartName());
//			set1.add(part.getPartName());
//			for(CoNLLSentence s : part.getCoNLLSentences()) {
////				System.out.println(s.getText());
//			}
//		}
//		
		ArrayList<String> chiStr = new ArrayList<String>();
		chiStr.addAll(Common.getLines("chinese_list_all_train"));
		chiStr.addAll(Common.getLines("chinese_list_all_development"));
		chiStr.addAll(Common.getLines("chinese_list_all_test"));
//		
//		System.out.println(doc.getParts().size());
//		
//		System.out.println(chiStr.size());
//		for(String l : chiStr) {
//			int a = l.indexOf("annotations");
//			a += "annotations/".length();
//			int b = l.lastIndexOf(".");
//			String partName = l.substring(a, b).replace("/", "-") + "_0";
//			if(set1.contains(partName)) {
////				System.out.println("GOOD");
//			} else {
////				System.out.println(partName);
//			}
//		}
		EMUtil.loadAlign();
		double all =0;
		double align = 0;
		for(int i=0;i<chiStr.size();i++) {
			if(i!=chiStr.size()-1) {
//				continue;
			}
			String line = chiStr.get(i);
			CoNLLDocument doc = new CoNLLDocument(line);
			doc.language = "chinese";
			int a = line.indexOf("annotations");
			a += "annotations/".length();
			int b = line.lastIndexOf(".");
			
			String docName = line.substring(a, b);
			for(CoNLLPart part : doc.getParts()) {
				for(CoNLLSentence s : part.getCoNLLSentences()) {
					String text = s.getText();
					
					ArrayList<Mention> chiNPs = EMUtil.extractMention(s);
					EMUtil.alignMentions(s, chiNPs, docName);
					
					
					all += chiNPs.size();
					for(Mention m : chiNPs) {
						System.out.println(docName + " # " + DocumentMap.idMap.get(docName));
						System.out.println(s.idInDoc);
						System.out.println(m.extent);
						Mention xm = m.getXSpan();
						System.out.println(xm==null?"null":xm.extent);
						System.out.println("------");
						if(xm!=null) {
							align++;
						}
					}
				}
			}
		}
		System.out.println(align + "/" + all + ":" + align/all);
	}
}

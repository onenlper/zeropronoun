package align;

import java.util.ArrayList;
import java.util.HashSet;

import em.EMUtil;

import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import util.Common;

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
//		ArrayList<String> chiStr = new ArrayList<String>();
//		chiStr.addAll(Common.getLines("chinese_list_all_train"));
//		chiStr.addAll(Common.getLines("chinese_list_all_development"));
//		chiStr.addAll(Common.getLines("chinese_list_all_test"));
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
	}
}

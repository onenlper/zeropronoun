//package align;
//
//import java.util.HashSet;
//
//import mentionDetect.ParseTreeMention;
//import model.Mention;
//import model.CoNLL.CoNLLDocument;
//import model.CoNLL.CoNLLPart;
//import em.EMUtil;
//
//public class Test {
//
//	public static void main(String args[]) {
//		
////		engAlign = "/users/yzcchen/chen3/ijcnlp2013/googleMTALL/eng_MT/align/";
////		chiAlign = "/users/yzcchen/chen3/ijcnlp2013/googleMTALL/chi_MT/align/";
////		engDoc = "engCoNLL.train.4";
////		mtEngDoc = "MT.engCoNLL.train.4";
////		chiDoc = "chiCoNLL.train.4";
////		mtChiDoc = "MT.chiCoNLL.train.4";
//		
//		DocumentMap
//				.loadRealBAAlignResult("/users/yzcchen/chen3/ijcnlp2013/googleMTALL/chi_MT/align/");
//
//		CoNLLDocument engDoc = new CoNLLDocument(
//				"/users/yzcchen/chen3/ijcnlp2013/OntoNotesParallel/src/MT.chiCoNLL.train.4", "english");
//		CoNLLDocument chiDoc = new CoNLLDocument(
//				"/users/yzcchen/chen3/ijcnlp2013/OntoNotesParallel/src/chiCoNLL.train.4", "chinese");
//
//		HashSet<Mention> engSpans = new HashSet<Mention>();
//		HashSet<Mention> chiSpans = new HashSet<Mention>();
//
//		ParseTreeMention ptm = new ParseTreeMention();
//
//		for (CoNLLPart part : engDoc.getParts()) {
//			// need to extract spans
//			engSpans.addAll(ptm.getMentions(part));
//		}
//
//		for (CoNLLPart part : chiDoc.getParts()) {
//			// need to extract spans
//			chiSpans.addAll(EMUtil.extractMention(part));
//		}
//
//		// read once
//		for (int i = 1; i <= 4; i++) {
//			Mention.assignMode = i;
//			for (Mention s : engSpans) {
//				s.getXSpan();
//			}
//			for (Mention s : chiSpans) {
//				s.getXSpan();
//			}
//		}
//		
//		for(Mention m : engSpans) {
//			Mention xm = m.getXSpan();
////			System.out.println(xm);
//			if(xm!=null) {
//				System.out.println(m.extent + "#" + xm.extent);
//			}
//		}
//		
//		Mention.assignMode = 5;
//	}
//
//}

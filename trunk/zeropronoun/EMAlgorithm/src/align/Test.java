package align;

import java.util.ArrayList;
import java.util.HashSet;

import mentionDetect.ParseTreeMention;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;

public class Test {

	public static void main(String args[]) {
		DocumentMap
				.loadRealBAAlignResult("/users/yzcchen/chen3/ijcnlp2013/wordAlignCor/align/");

		CoNLLDocument engDoc = new CoNLLDocument(
				"/users/yzcchen/chen3/ijcnlp2013/OntoNotesParallel/src/engCoNLL.train.1");
		CoNLLDocument chiDoc = new CoNLLDocument(
				"/users/yzcchen/chen3/ijcnlp2013/OntoNotesParallel/src/engCoNLL.train.1");

		HashSet<Mention> engSpans = new HashSet<Mention>();
		HashSet<Mention> chiSpans = new HashSet<Mention>();

		ParseTreeMention ptm = new ParseTreeMention();

		for (CoNLLPart part : engDoc.getParts()) {
			// need to extract spans
			engSpans.addAll(ptm.getMentions(part));
		}

		for (CoNLLPart part : chiDoc.getParts()) {
			// need to extract spans
			chiSpans.addAll(ptm.getMentions(part));
		}

		// read once
		for (int i = 1; i <= 4; i++) {
			Mention.assignMode = i;
			for (Mention s : engSpans) {
				s.getXSpan();
			}
			for (Mention s : chiSpans) {
				s.getXSpan();
			}
		}
		Mention.assignMode = 5;
	}

}

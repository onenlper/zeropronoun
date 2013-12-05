package mentionDetect;

import java.util.ArrayList;

import model.Mention;
import model.CoNLL.CoNLLPart;

public class ParseTreeMention extends MentionDetect {

	@Override
	public ArrayList<Mention> getMentions(CoNLLPart part) {
		ChineseMention ch = new ChineseMention();
		return ch.getChineseMention(part);
	}

}

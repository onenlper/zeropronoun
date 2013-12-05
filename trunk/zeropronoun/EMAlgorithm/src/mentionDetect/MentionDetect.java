package mentionDetect;

import java.util.ArrayList;

import model.Mention;
import model.CoNLL.CoNLLPart;

public abstract class MentionDetect {
	public abstract ArrayList<Mention>  getMentions(CoNLLPart part);
}

package em;

import java.util.ArrayList;

import model.Entity;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;

public class Test {

	
	public static void main(String args[]) {
		CoNLLDocument doc = new CoNLLDocument("train_gold_conll");
		for(CoNLLPart part : doc.getParts()) {
			ArrayList<Entity> chains = part.getChains();
			for(Entity chain : chains) {
				boolean plura = false;
				for(Mention m : chain.mentions) {
					if(EMUtil.plurals.contains(m.extent)) {
						plura = true;
					}
				}
				if(plura) {
					for(Mention m : chain.mentions) {
						System.out.println(m.extent);
					}
				}
			}
		}
	}
}

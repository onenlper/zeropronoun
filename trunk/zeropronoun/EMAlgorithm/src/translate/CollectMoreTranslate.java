package translate;

import java.util.ArrayList;
import java.util.HashSet;

import util.Common;

public class CollectMoreTranslate {

	public static void main(String args[]) {
		HashSet<String> chi = new HashSet<String>(Common.getLines("toTranslate/tran.gold"));
		
//		ArrayList<String> eng = Common.getLines("");
//		HashMap<String, String> map = new HashMap<String, String>();
		
		ArrayList<String> toTranslate = new ArrayList<String>();
		
		ArrayList<String> moreChi = Common.getLines("ch.systemParseSystemAZP");
		moreChi.addAll(Common.getLines("ch.goldParseSystemAZP"));
		
		for(String l : moreChi) {
			if(!chi.contains(l)) {
				toTranslate.add(l);
			}
		}
		
		Common.outputLines(toTranslate, "moreTranslate");
		
	}
}

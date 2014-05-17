package translate;

import java.io.File;
import java.util.ArrayList;

import util.Common;

public class CombineTranslate {

	public static void main(String args[]) {
		ArrayList<String> lines = Common.getLines("en.moretrans");
		for(String line : lines) {
			boolean hanzi = false;
			for(char c : line.toCharArray()) {
				if(c>0 && c<128) {
					continue;
				} else {
					System.out.print(c);
					hanzi = true;
				}
			}
			if(hanzi) {
				System.out.println();
			}
		}
//		go();
	}

	private static void go() {
		String base = "/users/yzcchen/chen3/zeroEM/EMAlgorithm/src/moreTrans/";

		File folder = new File(base);
		ArrayList<String> allLines = new ArrayList<String>();
		for(int i=0;i<=103;i++) {
			
			String id = Integer.toString(i);
			if(i<10) {
				id = "00" + id;
			} else if(i<100) {
				id = "0" + id;
			}
			
			String name = "x" + id + ".yy";
			ArrayList<String> ls;
			if(!(new File(base + name).exists())) {
				name = "x" + id + ".en";
			}
			ls = Common.getLines(base + name);
			if(ls.size()==401 && ls.get(400).isEmpty()) {
				ls.remove(400);
			}
			if(ls.size()!=400) {
				System.out.println(name);
			}
			allLines.addAll(ls);
		}
		
		Common.outputLines(allLines, "en.moretrans");
	}

}

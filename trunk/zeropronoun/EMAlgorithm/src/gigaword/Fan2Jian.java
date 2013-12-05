package gigaword;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

public class Fan2Jian {

	public static String convert(String str, HashMap<String, String> map) {
		String newStr = "";

		for (int i = 0; i < str.length(); i++) {
			if (map.containsKey(str.substring(i, i + 1))) {
				newStr += map.get(str.substring(i, i + 1));
			} else {
				newStr += str.substring(i, i + 1);
			}
		}

		return newStr;
	}

	public static void main(String args[]) {
		String fans = Common.getLines("dict/fan").get(0);
		String jians = Common.getLines("dict/jian").get(0);

		HashMap<String, String> map = new HashMap<String, String>();

		for(int i=0;i<fans.length();i++) {
			String fan = fans.substring(i, i+1);
			String jian = jians.substring(i, i+1);
			map.put(fan, jian);
		}
		
		String folder = "/users/yzcchen/chen3/zeroEM/rawText/cna/";

		int i = 0;
		for(File file : (new File(folder)).listFiles()) {
			if(file.getName().endsWith(".text")) {
				System.out.println(file.getAbsolutePath() + " " + (i++));
				ArrayList<String> lines = Common.getLines(file.getAbsolutePath());
				ArrayList<String> output= new ArrayList<String>();
				for(String line : lines) {
					output.add(convert(line, map));
				}
				Common.outputLines(output, file.getAbsolutePath().replace("/cna/", "/cna_simple/"));
			}
		}
	}
}

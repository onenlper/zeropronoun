package em;

import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

public class Analyze {

	public static void main(String args[]) {
		HashMap<String, String> m1 = parse("EM.correct.all");
		HashMap<String, String> m2 = parse("correct.super");

		double hit = 0;
		int a = 0;
		for (String k1 : m1.keySet()) {
			String s1 = m1.get(k1);
			String s2 = m2.get(k1);
			if (s1.equals(s2)) {
				hit++;
				System.out.println(s1);
			}
		}
		System.out.println(hit / m1.keySet().size());
	}

	public static HashMap<String, String> parse(String f) {
		ArrayList<String> em = Common.getLines(f);
		HashMap<String, String> map = new HashMap<String, String>();
		for (String line : em) {
			if (true 
					&& line.endsWith("GOOD")
					) {
				String tks[] = line.split(":");
				String fn = tks[0] + tks[1];

				String ms = tks[2];
				String rs = tks[3];

				String mTks[] = ms.split("-");
				String zm = mTks[0];
				String antm = mTks[1];

				String key = fn + "-" + zm;

				map.put(key, antm);
			}
		}
		return map;
	}
}

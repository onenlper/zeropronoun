package ngram;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;
import em.EMUtil;

public class Ngram {

	// public static HashSet<String> pronouns = new
	// HashSet<String>(Arrays.asList(
	// "你", "我", "他", "她", "它", "你们", "我们", "他们", "她们", "它们"
	// // , "这", "这里"
	// ));

	public static void addMap(HashMap<String, Integer> map, String key,
			int count) {
		Integer i = map.get(key);
		if (i == null) {
			map.put(key, count);
		} else {
			map.put(key, i.intValue() + count);
		}
	}

	public static void main(String args[]) throws Exception {
		String ngramFn = "/users/yzcchen/chen3/5-gram/5-gram/";
		HashMap<String, Integer> goodPattern = new HashMap<String, Integer>();
		HashMap<String, Integer> badPattern = new HashMap<String, Integer>();
		int m = 0;
		for (File fn : (new File(ngramFn)).listFiles()) {
			System.out.println(fn.getAbsolutePath() + "\t" + (m++));
			if (!fn.getAbsolutePath().endsWith("-00394")) {
				continue;
			}
			BufferedReader br = new BufferedReader(new FileReader(fn));
			String line = "";
			while ((line = br.readLine()) != null) {
				String tks[] = line.split("\\s+");
				int pros = 0;
				HashSet<String> proSet = new HashSet<String>();
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < tks.length - 1; i++) {
					String tk = tks[i];
					if (EMUtil.pronouns.contains(tk)) {
						sb.append("#PRO# ");
						proSet.add(tk);
						pros++;
					} else {
						sb.append(tk).append(" ");
					}
				}
				if (pros == 2) {
					String pattern = sb.toString();
					// System.out.println(pattern);
					int count = Integer.parseInt(tks[tks.length - 1]);
					// good
					if (proSet.size() == 1) {
						addMap(goodPattern, pattern, count);
					} else {
						// bad
						addMap(badPattern, pattern, count);
					}
				}
			}
			br.close();
		}
		Common.outputHashMap(goodPattern, "goodPattern");
		Common.outputHashMap(badPattern, "badPattern");

		HashSet<String> ps = new HashSet<String>();
		ps.addAll(goodPattern.keySet());
		ps.addAll(badPattern.keySet());

		FileWriter fw = new FileWriter("ngram-patterns");
		for (String key : ps) {
			StringBuilder sb = new StringBuilder();
			double coref = 0;
			if (goodPattern.containsKey(key)) {
				coref = goodPattern.get(key);
			}
			double notCoref = 0;
			if (badPattern.containsKey(key)) {
				notCoref = badPattern.get(key);
			}
			double percent = coref / (coref + notCoref);
			sb.append(key).append(" ").append(coref).append(" ")
					.append(notCoref).append(" ").append(percent).append('\n');
			fw.write(sb.toString());
		}
		fw.close();
	}
}

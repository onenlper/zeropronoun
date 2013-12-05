package ngram;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;

import util.Common;
import em.EMUtil;

public class PatternAnalysis {

	public static void main(String args[]) throws Exception {
		ArrayList<String> lines = Common.getLines("ngram-patterns");
		HashSet<String> ps = new HashSet<String>();
		for (String line : lines) {
			int b3 = line.lastIndexOf(' ');
			int b2 = line.lastIndexOf(' ', b3 - 1);
			int b1 = line.lastIndexOf(' ', b2 - 1);
			String p = line.substring(0, b1).trim();
//			System.out.println(p);
			ps.add(p);
		}

		String ngramFn = "/users/yzcchen/chen3/5-gram/5-gram/";
		FileWriter fw = new FileWriter("match-ngram");
		int m = 0;
		for (File fn : (new File(ngramFn)).listFiles()) {
			System.out.println(fn.getAbsolutePath() + "\t" + (m++));
			if (!fn.getAbsolutePath().endsWith("-00394")) {
				continue;
			}
			BufferedReader br = new BufferedReader(new FileReader(fn));
			String line = "";
			while ((line = br.readLine()) != null) {
				boolean qualify = false;
				String tks[] = line.split("\\s+");
				int pros = 0;
				int proID = -1;
				for (int i = 0; i < tks.length - 1; i++) {
					String tk = tks[i];
					if (EMUtil.pronouns.contains(tk)) {
						tks[i] = "#PRO#";
						proID = i;
						pros++;
					}
				}
				if (pros == 1) {
					for(int i=0;i<tks.length-1;i++) {
						if(i==proID) {
							continue;
						}
						StringBuilder sb = new StringBuilder();
						for(int j=0;j<tks.length-1;j++) {
							if(j==i) {
								sb.append("#PRO#").append(" ");
							} else {
								sb.append(tks[j]).append(" ");
							}
						}
//						System.out.println(sb.toString().trim() + "@");
						if(ps.contains(sb.toString().trim())) {
//							System.out.println(line);
//							System.out.println(sb.toString().trim());
//							System.out.println("==========");
							qualify = true;
							break;
						}
					}
				}
				if(qualify) {
					fw.write(line + "\n");
				}
			}
			br.close();
		}
		fw.close();
	}
}

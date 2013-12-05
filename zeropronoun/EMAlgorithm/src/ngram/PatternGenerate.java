package ngram;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;
import em.EMUtil;

public class PatternGenerate {
	public static void addKey(HashMap<String, HashMap<String, Integer>> stats, String key, String subKey, int count) {
		HashMap<String, Integer> subMap = stats.get(key);
		if(subMap==null) {
			subMap = new HashMap<String, Integer>();
			stats.put(key, subMap);
		}
		Integer i = subMap.get(subKey);
		if(i==null) {
			subMap.put(subKey, count);
		} else {
			subMap.put(subKey, i.intValue() + count);
		}
	}

	public static void main(String args[]) throws Exception{
		ArrayList<String> ngrams = Common.getLines("match-ngram");
		
		ArrayList<String> lines = Common.getLines("ngram-patterns");
		HashSet<String> corefPattern = new HashSet<String>();
		for (String line : lines) {
			int b3 = line.lastIndexOf(' ');
			int b2 = line.lastIndexOf(' ', b3 - 1);
			int b1 = line.lastIndexOf(' ', b2 - 1);
			String p = line.substring(0, b1).trim();
			if(p.contains("#PRO# #PRO#")) {
//				continue;
			}
			double percent = Double.parseDouble(line.substring(b3+1));
			double positive = Double.parseDouble(line.substring(b1+1, b2));
			if(percent>.9 && positive>100) {
				corefPattern.add(p);
//				System.out.println(p);
			}
//			System.out.println(p);
		}
		
		HashMap<String, String> posDIC = Common.readFile2Map2("10POSDIC");
		
		HashMap<String, HashMap<String, Integer>> stats = new HashMap<String, HashMap<String, Integer>>();
		System.out.println(corefPattern.size());
		for(String ngram : ngrams) {
			String tks[] = ngram.split("\\s+");
			ArrayList<Integer> pronounIdxes = new ArrayList<Integer>();
			for(int i=0;i<tks.length-1;i++) {
				String tk = tks[i];
				if(EMUtil.pronouns.contains(tk)) {
					pronounIdxes.add(i);
				}
			}
			int count = Integer.parseInt(tks[tks.length-1]);
			boolean find = false;
			if(pronounIdxes.size()==0) {
				Common.bangErrorPOS("!!");
			}
//			System.out.println("=========");
//			System.out.println(ngram);
//			System.out.println("-----");
			for(int pronounIdx : pronounIdxes) {
				for(int i=0;i<tks.length-1;i++) {
					if(i==pronounIdx) {
						continue;
					}
					StringBuilder sb = new StringBuilder();
					for(int j=0;j<tks.length-1;j++) {
						if(j==i || j==pronounIdx) {
							sb.append("#PRO# ");
						} else {
							sb.append(tks[j]).append(" ");
						}
					}
//					System.out.println(sb.toString().trim());
					if(corefPattern.contains(sb.toString().trim())) {
						find = true;
						String pro = tks[pronounIdx];
						String noun = tks[i];
						if(posDIC.containsKey(noun) && !posDIC.get(noun).startsWith("N")) {
							continue;
						}
						if(noun.startsWith("不") || noun.startsWith("<")) {
							continue;
						}
						addKey(stats, noun, pro, count);
//						System.out.println(pro + " # " + noun);	
//						System.out.println(sb.toString());
					}
				}
			}
			if(!find) {
//				Common.bangErrorPOS("");
			}
		}
		FileWriter fw = new FileWriter("collectStats");
		
		for(String key : stats.keySet()) {
			HashMap<String, Integer> subMap = stats.get(key);
			for(String subKey : subMap.keySet()) {
				fw.write(key + " " + subKey + " " + subMap.get(subKey) + "\n");
			}
		}
		fw.close();
	}
}

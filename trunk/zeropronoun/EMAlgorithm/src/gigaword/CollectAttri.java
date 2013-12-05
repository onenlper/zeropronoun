package gigaword;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.DependSentence;
import model.GraphNode;
import util.Common;
import em.EMUtil;

public class CollectAttri {

	public static void main(String args[]) throws Exception {
		HashMap<String, Integer> goodPattern = Common.readFile2Map("goodPattern.giga");
		HashMap<String, Integer> badPattern = Common.readFile2Map("badPattern.giga");
		
		HashSet<String> ps = new HashSet<String>();
		ps.addAll(goodPattern.keySet());
		ps.addAll(badPattern.keySet());

		HashSet<String> qualifyPattern = new HashSet<String>();
		
		HashMap<String, HashMap<String, Integer>> stats = new HashMap<String, HashMap<String, Integer>>();
		double th = 0.8;
		for (String key : ps) {
			double coref = 0;
			if (goodPattern.containsKey(key)) {
				coref = goodPattern.get(key);
			}
			double notCoref = 0;
			if (badPattern.containsKey(key)) {
				notCoref = badPattern.get(key);
			}
			double percent = coref / (coref + notCoref);
			if(percent>th) {
				qualifyPattern.add(key);
			}
		}
		// process(str, ps);
		File folder = new File("/users/yzcchen/chen2/zeroEM/qxparser/");
		int i = 0;
		for (File subF : folder.listFiles()) {
			if (subF.isDirectory()) {
				for (File f : subF.listFiles()) {
					if (f.getAbsolutePath().endsWith(".gbk")) {
						System.out.println(f.getAbsolutePath() + " " + (i++));
						extract(f.getAbsolutePath(), qualifyPattern, stats);
					}
				}
			}
		}
		
		FileWriter fw = new FileWriter("collectStats.giga");
		
		for(String key : stats.keySet()) {
			HashMap<String, Integer> subMap = stats.get(key);
			for(String subKey : subMap.keySet()) {
				fw.write(key + " " + subKey + " " + subMap.get(subKey) + "\n");
			}
		}
		fw.close();
		
	}

	public static void extract(String fn, HashSet<String> patterns,
			HashMap<String, HashMap<String, Integer>> stats) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(fn), "gb18030"));
		String line;
		while ((line = br.readLine()) != null) {
			try {
				process(line, patterns, stats);
			} catch (Exception e) {
				System.out.println(line);
//				throw e;
			}
		}
		br.close();
	}

	private static void process(String line,
			HashSet<String> patterns,
			HashMap<String, HashMap<String, Integer>> stats) throws Exception{
		String tks[] = line.split("\\s+");
		DependSentence sent = new DependSentence();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tks.length; i++) {
			String tk = tks[i];
			if (tk.indexOf('/') == -1 || tk.length()==1 || tk.replace("/", "").length() +1==tk.length()) {
				tks[i + 1] = tks[i] + tks[i + 1];
				tks[i] = "";
			}
			sb.append(tks[i]).append(" ");
		}
		tks = sb.toString().trim().split("\\s+");
		for (int i = 0; i < tks.length; i++) {
			String tk = tks[i];
			if (tk.indexOf('/') == -1) {
				tks[i + 1] = tks[i] + tks[i + 1];
				continue;
			}
			String word = "";
			String POS = "";
			int linkTo = 0;
			String dep = "";

			int s3 = tk.lastIndexOf('/');
			int s2 = tk.lastIndexOf('/', s3 - 1);
			int s1 = tk.lastIndexOf('/', s2 - 1);

			if (s3 + 1 != tk.length()) {
				dep = tk.substring(s3 + 1);
			}
			try {
				linkTo = Integer.parseInt(tk.substring(s2 + 1, s3));
			} catch (Exception e) {
				System.out.println(tk + " " + i + " " + tks.length);
				throw e;
			}

			if (s1 == -1) {
				// encoding error
				POS = "";
				word = "";
			} else {
				POS = tk.substring(s1 + 1, s2);
				word = tk.substring(0, s1);
			}

			GraphNode node = new GraphNode(word, POS, linkTo, dep, i);
			sent.nodes.put(i, node);
		}
		GraphNode root = new GraphNode("ROOT", "ROOT", -1, "", tks.length);
		sent.nodes.put(tks.length, root);

		HashSet<Integer> proIdxes = new HashSet<Integer>();
		
		for (int i = 0; i < tks.length; i++) {
			GraphNode cNode = sent.nodes.get(i);
			GraphNode pNode = sent.nodes.get(cNode.linkTo);
			String type = cNode.dep;
			cNode.backNode = pNode;
			pNode.addEdge(cNode, type + "+");
			cNode.addEdge(pNode, type + "-");

			String word = cNode.token;
			if (EMUtil.pronouns.contains(word)) {
				proIdxes.add(i);
			}
		}
		
		for(int proIdx : proIdxes) {
			
			for(int i=0;i<tks.length;i++) {
				if(proIdxes.contains(i)) {
					continue;
				}
				
				GraphNode from = null;
				GraphNode to = null;
				if(i<proIdx) {
					from = sent.nodes.get(i);
					to = sent.nodes.get(proIdx);
				} else {
					from = sent.nodes.get(proIdx);
					to = sent.nodes.get(i);
				}
				
				ArrayList<GraphNode> path = EMUtil.findPath(from, to);
				String strPath = EMUtil.getPathString(path);
				
				if(patterns.contains(strPath)) {
					String word = sent.nodes.get(i).token;
					String pronoun = sent.nodes.get(proIdx).token;
					HashMap<String, Integer> subMap = stats.get(word);
					if(subMap==null) {
						subMap = new HashMap<String, Integer>();
						stats.put(word, subMap);
					}
					
					Integer count = subMap.get(pronoun);
					if(count==null) {
						subMap.put(pronoun, 1);
					} else {
						subMap.put(pronoun, count.intValue() + 1);
					}
				}
			}
		}
	}

	public static void addMap(HashMap<String, Integer> map, String key) {
		Integer count = map.get(key);
		if (count == null) {
			map.put(key, 1);
		} else {
			map.put(key, count.intValue() + 1);
		}
	}
}

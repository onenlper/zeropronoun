package gigaword;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

import model.DependSentence;
import model.GraphNode;
import em.EMUtil;

public class LoadDependency {

	public static void main(String args[]) throws Exception {
		HashMap<String, Integer> goodPattern = new HashMap<String, Integer>();
		HashMap<String, Integer> badPattern = new HashMap<String, Integer>();
		String str = "“/PU/8/P  欢迎/IJ/8/VMOD  来/MSP/1/DEP  我/PN/4/NMOD  家/NN/8/VMOD  ,/PU/8/P  我/PN/8/SUB  还/AD/8/VMOD  是/VC/18/  第一/OD/10/AMOD  次/M/12/VMOD  接待/VV/12/VC  来自/VV/14/SBAR  中国/NR/12/VMOD  的/DEC/15/NMOD  游客/NN/8/PRD  。/PU/8/P  ”/PU/8/P";
		// process(str, ps);
		File folder = new File("/users/yzcchen/chen2/zeroEM/qxparser/");
		int i = 0;
		for (File subF : folder.listFiles()) {
			if (subF.isDirectory()) {
				for (File f : subF.listFiles()) {
					if (f.getAbsolutePath().endsWith(".gbk")) {
						System.out.println(f.getAbsolutePath() + " " + (i++));
						extract(f.getAbsolutePath(), goodPattern, badPattern);
					}
				}
			}
		}
		Common.outputHashMap(goodPattern, "goodPattern.giga");
		Common.outputHashMap(badPattern, "badPattern.giga");
		
		HashSet<String> ps = new HashSet<String>();
		ps.addAll(goodPattern.keySet());
		ps.addAll(badPattern.keySet());

		FileWriter fw = new FileWriter("ngram-patterns.giga");
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
			sb.append(key).append("\t").append(coref).append(" ")
					.append(notCoref).append(" ").append(percent).append('\n');
			fw.write(sb.toString());
		}
		fw.close();
	}

	public static void extract(String fn, HashMap<String, Integer> goodPattern,
			HashMap<String, Integer> badPattern) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(fn), "gb18030"));
		String line;
		while ((line = br.readLine()) != null) {
			try {
				process(line, goodPattern, badPattern);
			} catch (Exception e) {
				System.out.println(line);
//				throw e;
			}
		}
		br.close();
	}

	private static void process(String line,
			HashMap<String, Integer> goodPattern,
			HashMap<String, Integer> badPattern) throws Exception{
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

		HashMap<String, ArrayList<Integer>> samePronouns = new HashMap<String, ArrayList<Integer>>();

		for (int i = 0; i < tks.length; i++) {
			GraphNode cNode = sent.nodes.get(i);
			GraphNode pNode = sent.nodes.get(cNode.linkTo);
			String type = cNode.dep;
			cNode.backNode = pNode;
			pNode.addEdge(cNode, type + "+");
			cNode.addEdge(pNode, type + "-");

			String word = cNode.token;
			if (EMUtil.pronouns.contains(word)) {
				ArrayList<Integer> arr = samePronouns.get(word);
				if (arr == null) {
					arr = new ArrayList<Integer>();
					samePronouns.put(word, arr);
				}
				arr.add(i);
			}
		}
		for (String pronoun : samePronouns.keySet()) {
			ArrayList<Integer> arr = samePronouns.get(pronoun);
			if (arr.size() != 1) {
				for (int i = 0; i < arr.size(); i++) {
					GraphNode from = sent.nodes.get(arr.get(i));
					for (int j = i + 1; j < arr.size(); j++) {
						GraphNode to = sent.nodes.get(arr.get(j));
						ArrayList<GraphNode> path = EMUtil.findPath(from, to);
						// System.out.println(EMUtil.getPathString(path));
						String pathStr = EMUtil.getPathString(path);
						addMap(goodPattern, pathStr);
					}
				}
			}
		}

		for (String pr1 : samePronouns.keySet()) {
			ArrayList<Integer> arr1 = samePronouns.get(pr1);
			for (String pr2 : samePronouns.keySet()) {
				ArrayList<Integer> arr2 = samePronouns.get(pr2);
				if (!pr1.equals(pr2)) {

					for (int a1 : arr1) {

						for (int a2 : arr2) {
							if (a1 < a2) {
								GraphNode from = sent.nodes.get(a1);
								GraphNode to = sent.nodes.get(a2);
								ArrayList<GraphNode> path = EMUtil.findPath(
										from, to);
								String pathStr = EMUtil.getPathString(path);

								// System.out.println(line);
								// System.out.println(pathStr);

								addMap(badPattern, pathStr);
							}
						}
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

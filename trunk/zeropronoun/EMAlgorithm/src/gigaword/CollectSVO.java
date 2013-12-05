package gigaword;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import model.DependSentence;
import model.GraphNode;
import util.Common;

public class CollectSVO {

	// collect animacy

	public static void main(String args[]) throws Exception {

		HashMap<String, Integer> SVO = new HashMap<String, Integer>();

		// process(str, ps);
		File folder = new File("/users/yzcchen/chen2/zeroEM/qxparser/");
		int i = 0;
		for (File subF : folder.listFiles()) {
			if (subF.isDirectory()) {
				for (File f : subF.listFiles()) {
					if (f.getAbsolutePath().endsWith(".gbk")) {
						System.out.println(f.getAbsolutePath() + " " + (i++));
						extract(f.getAbsolutePath(), SVO);
					}
				}
			}
		}
		Common.outputHashMap(SVO, "svogiga");
	}

	public static void extract(String fn,
			HashMap<String, Integer> unigrams) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(fn), "gb18030"));
		String line;
		while ((line = br.readLine()) != null) {
			try {
				process(line, unigrams);
			} catch (Exception e) {
				System.out.println(line);
				// throw e;
			}
		}
		br.close();
	}

	private static void process(String line,
			HashMap<String, Integer> SVO) throws Exception {
		String tks[] = line.split("\\s+");
		DependSentence sent = new DependSentence();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tks.length; i++) {
			String tk = tks[i];
			if (tk.indexOf('/') == -1 || tk.length() == 1
					|| tk.replace("/", "").length() + 1 == tk.length()) {
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

		for (int i = 0; i < tks.length; i++) {
			GraphNode cNode = sent.nodes.get(i);
			GraphNode pNode = sent.nodes.get(cNode.linkTo);
			String type = cNode.dep;
			cNode.backNode = pNode;
			pNode.addEdge(cNode, type + "+");
			cNode.addEdge(pNode, type + "-");
		}
		
		for(int i=0;i<tks.length;i++) {
			GraphNode node = sent.nodes.get(i);
			
			if(node.dep.equals("SUB")) {
				String subj = node.token;
				
				GraphNode predNode = sent.nodes.get(node.linkTo);
				String pred = predNode.token;
				
				String obj = "";
				for(GraphNode tmp : predNode.nexts) {
					if(tmp.dep.equals("PRD") || tmp.dep.equals("OBJ")) {
						obj = tmp.token;
						break;
					}
				}
				
				if(!subj.isEmpty() && !pred.isEmpty()) {
					String key = subj + " " + pred + " " + obj;
//					System.out.println(key);
					Integer count = SVO.get(key);
					if(count==null) {
						SVO.put(key, 1);
					} else {
						SVO.put(key, count.intValue() + 1);
					}
				}
			}
		}
	}
}

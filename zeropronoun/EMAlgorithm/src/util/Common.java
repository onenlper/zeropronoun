package util;

import em.ILP;
import gigaword.ConvertZYParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Common {

	public static boolean train = false;

	public static String part;

	public static String changeSurffix(String file, String suffix) {
		int a = file.lastIndexOf(".");
		if (a == -1) {
			return file + "." + suffix;
		} else {
			return file.substring(0, a + 1) + suffix;
		}
	}

	public static String concat(String antHead, String mHead) {
		return antHead.compareTo(mHead) > 0 ? (antHead + "_" + mHead) : (mHead
				+ "_" + antHead);
	}

	public static String wordnet = "/usr/local/WordNet-3.0/";

	// cache, store file content
	public static HashMap<String, ArrayList<String>> fileCache = new HashMap<String, ArrayList<String>>();

	public static class RegularXMLReader extends DefaultHandler {
		StringBuilder sb;

		public RegularXMLReader(StringBuilder sb) {
			this.sb = sb;
		}

		public void startElement(String uri, String name, String qName,
				Attributes atts) {
		}

		public void characters(char ch[], int start, int length)
				throws SAXException {
			String str = new String(ch, start, length);
			sb.append(str);
		}

		public void endElement(String uri, String localName, String qName)
				throws SAXException {
		}
	}

	public static String getXMLFile(String xmlFn) {
		StringBuilder sb = new StringBuilder();
		try {
			InputStream inputStream = new FileInputStream(new File(xmlFn));
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			RegularXMLReader reader = new RegularXMLReader(sb);
			sp.parse(new InputSource(inputStream), reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

	// whether the first String includes the second
	public static boolean engWordInclude(String str1, String str2) {
		String token1[] = str1.split("\\s+");
		String token2[] = str2.split("\\s+");
		HashSet<String> set1 = new HashSet<String>();
		HashSet<String> set2 = new HashSet<String>();
		set1.addAll(Arrays.asList(token1));
		set2.addAll(Arrays.asList(token2));
		for (String s2 : set2) {
			if (!set1.contains(s2)) {
				return false;
			}
		}
		return true;
	}

	public static void outputHashSet(HashSet<String> set, String filename) {
		try {
			FileWriter fw = new FileWriter(filename);
			for (String str : set) {
				fw.write(str + "\n");
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static HashSet<String> pronouns = null;

	public static boolean isPronoun(String str) {
		if (pronouns == null) {
			pronouns = Common.readFile2Set(dicPath + "pronoun");
		}
		if (pronouns.contains(str)) {
			return true;
		} else {
			return false;
		}
	}

	public static int PRONOUN_ME = 0;
	public static int PRONOUN_YOU = 1;
	public static int PRONOUN_HE = 2;
	public static int PRONOUN_SHE = 3;
	public static int PRONOUN_IT = 4;
	public static int PRONOUN_HE_S = 5;
	public static int PRONOUN_ME_S = 6;
	public static int PRONOUN_SHE_S = 7;
	public static int PRONOUN_YOU_S = 8;
	public static int PRONOUN_IT_S = 9;
	public static int PRONOUN_WHO = 10;

	public static int getPronounType(String str) {
		if (str.equals("我") || str.equals("俺") || str.equals("自己")
				|| str.equals("本身") || str.equals("本人")) {
			return PRONOUN_ME;
		} else if (str.equals("你") || str.equals("您")) {
			return PRONOUN_YOU;
		} else if (str.equals("他") || str.equals("那位") || str.equals("其他")) {
			return PRONOUN_HE;
		} else if (str.equals("她")) {
			return PRONOUN_SHE;
		} else if (str.equals("它") || str.equals("这") || str.equals("那")
				|| str.equals("那里") || str.equals("其它") || str.equals("其")) {
			return PRONOUN_IT;
		} else if (str.equals("他们") || str.equals("双方")) {
			return PRONOUN_HE_S;
		} else if (str.equals("咱们") || str.equals("我们") || str.equals("大家")) {
			return PRONOUN_ME_S;
		} else if (str.equals("她们")) {
			return PRONOUN_SHE_S;
		} else if (str.equals("你们")) {
			return PRONOUN_YOU_S;
		} else if (str.equals("它们") || str.equals("这些") || str.equals("那些")
				|| str.equals("一些")) {
			return PRONOUN_IT_S;
		} else if (str.equals("谁") || str.equals("什么") || str.equals("哪个")) {
			return PRONOUN_WHO;
		} else
			return -1;
	}

	private static HashMap<String, Integer> abbreHash = null;

	public static boolean contain(String str1, String str2) {
		for (int i = 0; i < str2.length(); i++) {
			if (str1.indexOf(str2.charAt(i)) == -1) {
				return false;
			}
		}
		return true;
	}

	public static boolean isEnglishAbbreviation(String str1, String str2) {
		return false;
	}

	public static boolean isAbbreviation(String str1, String str2) {
		if (abbreHash == null) {
			abbreHash = new HashMap<String, Integer>();
			ArrayList<String> lines = Common.getLines(dicPath + "abbreviation");
			for (int i = 0; i < lines.size(); i++) {
				String line = lines.get(i);
				String tokens[] = line.split("\\s+");
				for (String token : tokens) {
					abbreHash.put(token, i);
					if (token.endsWith("省") || token.endsWith("市")) {
						abbreHash
								.put(token.substring(0, token.length() - 1), i);
					}
				}
			}
		}
		if (abbreHash.containsKey(str1) && abbreHash.containsKey(str2)) {
			return (abbreHash.get(str1).intValue() == abbreHash.get(str2)
					.intValue());
		} else {
			String l = str1.length() < str2.length() ? str2 : str1;
			String s = str1.length() >= str2.length() ? str2 : str1;
			if (l.substring(0, l.length() - 1).equalsIgnoreCase(s)
					&& (l.charAt(l.length() - 1) == '省' || l
							.charAt(l.length() - 1) == '市')) {
				return true;
			} else {
				return false;
			}
		}
	}

	public static ArrayList<Double> getDoubles(String filename) {
		ArrayList<Double> doubles = new ArrayList<Double>();
		BufferedReader br = getBr(filename);
		String line;
		try {
			while ((line = br.readLine()) != null) {
				double value = Double.valueOf(line);
				doubles.add(value);
			}
			br.close();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return doubles;
	}

	public static String getFileContent(String filename) {
		StringBuilder lines = new StringBuilder();
		try {
			BufferedReader br = getBr(filename);
			String line;
			while ((line = br.readLine()) != null) {
				lines.append(line);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lines.toString();
	}

	public static ArrayList<String> getLines(String filename) {
		if (fileCache.containsKey(filename)) {
			return fileCache.get(filename);
		} else {
			ArrayList<String> lines = null;
			try {
				lines = new ArrayList<String>();
				BufferedReader br = getBr(filename);
				String line;
				while ((line = br.readLine()) != null) {
					lines.add(line);
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// fileCache.put(filename, lines);
			return lines;
		}
	}

	public static void outputLines(ArrayList<String> lines, String filename) {
		try {
			FileWriter fw;
			fw = new FileWriter(filename);
			for (String line : lines) {
				fw.write(line + "\n");
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void outputLine(String line, String filename) {
		try {
			FileWriter fw;
			fw = new FileWriter(filename);
			fw.write(line);
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//
	// public static void outputHashMap(HashMap<String, Integer> map,
	// String filename) {
	// try {
	// FileWriter fw = new FileWriter(filename);
	// for (String str : map.keySet()) {
	// fw.write(str + " " + map.get(str).toString() + "\n");
	// }
	// fw.close();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	//
	public static void outputHashMap(HashMap map, String filename) {
		try {
			FileWriter fw = new FileWriter(filename);
			for (Object str : map.keySet()) {
				Object obj = map.get(str);
				if (obj instanceof int[]) {
					int a[] = (int[]) obj;
					StringBuilder sb = new StringBuilder();
					sb.append(str.toString()).append(" ");
					for (int b : a) {
						sb.append(b).append(" ");
					}
					fw.write(sb.toString().trim() + "\n");
				} else {
					fw.write(str.toString() + " " + map.get(str).toString()
							+ "\n");
				}
			}
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void increaseHashValue(HashMap<String, Object> map, String key) {
		if (map.containsKey(key)) {
			Object value = map.get(key);
			if (value instanceof Integer) {
				int v = ((Integer) value).intValue();
				map.put(key, v + 1);
			} else if (value instanceof Long) {
				long v = ((Long) value).longValue();
				map.put(key, v + 1);
			}
		}
	}

	public static HashSet<String> readFile2Set(String filename) {
		HashSet<String> set = null;
		try {
			set = new HashSet<String>();
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				set.add(line.trim());
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return set;
	}

	/*
	 * read file to <String, int[]>
	 */
	public static HashMap<String, int[]> readFile2Map3(String filename) {
		HashMap<String, int[]> map = null;
		try {
			map = new HashMap<String, int[]>();
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				String token[] = line.split("\\s+");
				int a[] = new int[token.length - 1];
				for (int i = 0; i < token.length - 1; i++) {
					a[i] = Integer.valueOf(token[i + 1]);
				}
				map.put(token[0], a);
			}
			br.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public static HashMap<String, Integer> readFile2Map(String filename) {
		HashMap<String, Integer> map = null;
		try {
			map = new HashMap<String, Integer>();
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				int pos = line.lastIndexOf(' ');
				String str = line.substring(0, pos);
				int value = Integer.valueOf(line.substring(pos + 1,
						line.length()));
				map.put(str.trim(), value);
			}
			br.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public static HashMap<String, Integer> readFile2Map(String filename,
			char split) {
		HashMap<String, Integer> map = null;
		try {
			map = new HashMap<String, Integer>();
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				int pos = line.lastIndexOf(split);
				String str = line.substring(0, pos);
				int value = Integer.valueOf(line.substring(pos + 1,
						line.length()));
				map.put(str, value);
			}
			br.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public static HashMap<String, Long> readFile2Map4(String filename) {
		HashMap<String, Long> map = null;
		try {
			map = new HashMap<String, Long>();
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.isEmpty()) {
					continue;
				}
				String token[] = line.split("\\s+");
				long value = Long.valueOf(Long.valueOf(token[1]));
				map.put(token[0], value);
			}
			br.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public static HashMap<String, Double> readFile2Map5(String filename) {
		HashMap<String, Double> map = null;
		try {
			map = new HashMap<String, Double>();
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				int pos = line.lastIndexOf(' ');
				String str = line.substring(0, pos);
				double value = Double.valueOf(line.substring(pos + 1,
						line.length()));
				map.put(str, value);
			}
			br.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public static HashMap<String, String> readFile2Map2(String filename) {
		return readFile2Map2(filename, ' ');
	}

	public static HashMap<String, String> readFile2Map2(String filename, char c) {
		HashMap<String, String> map = null;
		try {
			map = new HashMap<String, String>();
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			while ((line = br.readLine()) != null) {
				int pos = line.lastIndexOf(c);
				String str = line.substring(0, pos);
				String value = line.substring(pos + 1, line.length());
				map.put(str, value);
			}
			br.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return map;
	}

	public static HashMap<String, Integer> combineHashMap(
			HashMap<String, Integer> total, HashMap<String, Integer> map) {
		for (String str : map.keySet()) {
			int value = map.get(str);
			if (total.containsKey(str)) {
				int oldValue = total.get(str);
				total.put(str, value + oldValue);
			} else {
				total.put(str, value);
			}
		}
		return total;
	}

	public static BufferedReader getBr(String filename) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return br;
	}

	// determine if this is a stop sign punctuation
	public static boolean isStopPun(char c) {
		if (c == '。' || c == '？' || c == '！')
			return true;
		return false;
	}

	// determine if this is a stop sign punctuation
	public static boolean isPun(char c) {
		if (c == '。' || c == '？' || c == '！' || c == '．' || c == '：'
				|| c == '，' || c == '；')
			return true;
		return false;
	}

	public static HashSet<String> readSurname(String filename) {
		HashSet<String> surnames = new HashSet<String>();
		BufferedReader br = getBr(filename);
		try {
			String line = br.readLine();
			br.close();
			String tokens[] = line.split("\\s+");
			for (String token : tokens) {
				surnames.add(token);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return surnames;
	}

	public static int findWSCount(String str) {
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) != ' ') {
				return i;
			}
		}
		return str.length();
	}

	// extract plane text from syntax tree, using )
	public static String extractPlainText(String syntaxTree) {
		// ArrayList<String> words = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		// the index of )
		int rightBIdx = -1;
		while ((rightBIdx = syntaxTree.indexOf(')', rightBIdx + 1)) > 0) {
			if (syntaxTree.charAt(rightBIdx - 1) == ')') {
				continue;
			}
			int start = rightBIdx;
			// special case, character is ")"
			if (syntaxTree.charAt(start - 1) == ' ') {
				sb.append(")");
				continue;
			}
			while (true) {
				start--;
				if (syntaxTree.charAt(start) == ' ') {
					break;
				}
			}
			String word = syntaxTree.substring(start + 1, rightBIdx);
			sb.append(word);
		}
		return sb.toString();
		// return words;
	}

	public static int[] findPosition(String big, String small, int from) {
		int[] position = new int[2];
		while (big.charAt(from) != small.charAt(0)) {
			from++;
		}
		position[0] = from;
		for (int i = 1; i < small.length(); i++) {
			if (big.charAt(from) != small.charAt(i)) {
				from++;
			}
		}
		position[1] = from;
		return position;
	}

	public static MyTree constructTree(String treeStr) {
		MyTree tree = new MyTree();
		MyTreeNode currentNode = tree.root;
		int leafIdx = 0;
		for (int i = 0; i < treeStr.length(); i++) {
			if (treeStr.charAt(i) == '(') {
				int j = i + 1;
				// try {
				while (treeStr.charAt(j) != ' ' && treeStr.charAt(j) != '\n') {
					j++;
				}
				// } catch(Exception e) {
				// System.out.println(treeStr.substring(i-2,i+2));
				// // System.out.println(treeStr.charAt(j));
				// System.out.println(treeStr);
				// System.exit(1);
				// }
				String value = treeStr.substring(i + 1, j);
				MyTreeNode node = new MyTreeNode(value);
				if (tree.root == null) {
					tree.root = node;
				} else {
					currentNode.addChild(node);
				}
				if (value.startsWith("NP")) {
					node.isNNP = true;
				} else {
					if (node != tree.root) {
						node.isNNP = node.parent.isNNP;
					}
				}
				currentNode = node;
				while (treeStr.charAt(j) == '\n' || treeStr.charAt(j) == ' ') {
					j++;
				}
				if (treeStr.charAt(j) == '(' && treeStr.charAt(j + 1) != ')') {
					i = j - 1;
				} else {
					int m = j + 1;
					while (!(treeStr.charAt(m) == ')' && (m == treeStr.length() - 1
							|| treeStr.charAt(m + 1) == ' ' || treeStr
								.charAt(m + 1) == ')'))) {
						m++;
					}
					String value2 = treeStr.substring(j, m);
					MyTreeNode node2 = new MyTreeNode(value2);
					node2.leafIdx = leafIdx++;
					currentNode.addChild(node2);
					tree.leaves.add(node2);
					node2.isNNP = node2.parent.isNNP;
					// System.out.println(value2);
					i = m;
					currentNode = currentNode.parent;
				}

			} else if (treeStr.charAt(i) == ')') {
				if (currentNode != tree.root) {
					currentNode = currentNode.parent;
				}
			}
		}
		return tree;
	}

	public static String dicPath = "/users/yzcchen/workspace/ACL12/src/dict/";
	// public static String dicPath = "./dict/";

	private static HashMap<String, String[]> semanticDic;

	public static HashMap<String, String[]> getSemanticDic() {
		if (semanticDic == null) {
			semanticDic = loadSemanticDic();
		}
		return semanticDic;
	}

	private static HashMap<String, String[]> loadSemanticDic() {
		HashMap<String, String[]> semanticDic = new HashMap<String, String[]>();
		ArrayList<String> lines = Common.getLines(dicPath
				+ "TongyiciCiLin_8.txt");
		for (String line : lines) {
			String tokens[] = line.split("\\s+");
			String word = tokens[0];
			String semantic[] = new String[tokens.length - 2];
			for (int i = 0; i < semantic.length; i++) {
				semantic[i] = tokens[2 + i];
			}
			semanticDic.put(word, semantic);
		}
		return semanticDic;
	}

	public static String[] getSemantic(String head) {
		if (semanticDic == null) {
			semanticDic = loadSemanticDic();
		}
		return semanticDic.get(head);
	}

	// determine whether is person, 1=true, -1=false, 0=NA
	public static int isSemanticPerson(String str) {
		String[] codes = semanticDic.get(str);
		if (codes == null) {
			return 0;
		}
		for (String code : codes) {
			if (code.charAt(0) == 'A') {
				return 1;
			}
		}
		return -1;
	}

	// determine whether is animal, 1=true, -1=false, 0=NA
	public static int isSemanticAnimal(String str) {
		if (semanticDic == null) {
			loadSemanticDic();
		}
		String[] codes = semanticDic.get(str);
		if (codes == null) {
			return 0;
		}
		for (String code : codes) {
			if (code.startsWith("Bi")) {
				return 1;
			}
		}
		return -1;
	}

	// 社会 政法
	public static int isSemanticSocialEntity(String str) {
		String[] codes = semanticDic.get(str);
		if (codes == null) {
			return 0;
		}
		for (String code : codes) {
			if (code.startsWith("Di")) {
				return 1;
			}
		}
		return -1;
	}

	public static void main(String args[]) {
		// String treeStr =
		// "(ROOT  (IP    (NP      (NP (NR 台湾) (NR 陈水扁))      (NP (NN 总统)))    (VP (VV 任命)      (IP        (NP          (NP            (NP (NR 高雄))            (NP (NN 市长)))          (NP (NR 谢长廷)))        (VP (VC 为)          (NP            (ADJP (JJ 新任))            (NP (NN 行政) (NN 院长))))))    (PU -) (PU -)))";
		// Tree tree = Common.constructTree(treeStr);
		// TreeNode tn = tree.leaves.get(5);
		// TreeNode parent = tn.parent;
		// while (parent != tree.root) {
		// System.out.println(parent.value);
		// parent = parent.parent;
		// }
		// String str1 = "中国人民";
		// String str2 = "中华人民共和国";
		// System.out.println(Common.getEditDistance(str1, str2));

		String treeStr = "(FRAG (NR#t (NR#y (NR#x (NR#b 新) (NR#i 华)) (NR#i 社))) (NR#t (NR#y (NR#b 北) (NR#i 京))) (NT#t (NT#y (NT#x (NT#b 1) (NT#i 0)) (NT#i 月))) (NT#t (NT#y (NT#b 1) (NT#i 日))) (NN#t (NN#b 电)) (IP (PU#t (PU#b ()) (NP (NN#t (NN#y (NN#b 记) (NN#i 者))) (NR#t (NR#x (NR#b 安) (NR#x (NR#i 蓓) (NR#i ))))) (NR#t (NR#y (NR#b 中) (NR#i 国))) (NN#t (NN#x (NN#b 海) (NN#i 洋))) (NN#t (NN#y (NN#b 石) (NN#i 油))) (NN#t (NN#y (NN#b 总) (NN#y (NN#i 公) (NN#i 司))))) (VP (VP (NP (NT#t (NT#y (NT#b 近) (NT#i 期)))) (VP (VV#t (VV#z (VV#b 调) (VV#i 整))) (AS#t (AS#b 了)) (NP (NP (PU#t (PU#b “)) (NT#t (NT#x (NT#x (NT#b 十) (NT#i 一)) (NT#i 五))) (PU#t (PU#b ”))) (NP (NN#t (NN#y (NN#b 期) (NN#i 间))) (NN#t (NN#z (NN#b 节) (NN#i 能))) (NN#t (NN#z (NN#b 计) (NN#i 划))))))) (PU#t (PU#b ,)) (IP (IP (IP (PP (ADVP (AD#t (AD#x (AD#b 明) (AD#i 确)))) (PP (P#t (P#b 到)) (LCP (NP (NT#t (NT#y (NT#x (NT#x (NT#x (NT#b 2) (NT#i 0)) (NT#i 0)) (NT#i 9)) (NT#i 年)))) (LC#t (LC#b 末))))) (PU#t (PU#b ,)) (NP (NP (NP (QP (CD#t (CD#b 万)) (CLP (M#t (M#b 元)))) (NP (NN#t (NN#y (NN#b 产) (NN#i 值))))) (PU#t (PU#b 、)) (NP (QP (CD#t (CD#b 万)) (CLP (M#t (M#b 元)))) (NP (NN#t (NN#y (NN#x (NN#b 增) (NN#i 加)) (NN#i 值)))))) (ADJP (JJ#t (JJ#y (JJ#b 综) (JJ#i 合)))) (NP (NN#t (NN#y (NN#b >能) (NN#i 耗))))) (VP (PP (P#t (P#b 比)) (NP (NT#t (NT#y (NT#x (NT#x (NT#x (NT#b 2) (NT#i 0)) (NT#i 0)) (NT#i 5)) (NT#i 年))))) (ADVP (AD#t (AD#y (AD#b 至) (AD#i 少)))) (VP (VV#t (VV#y (VV#b 下) (VV#i 降))) (QP (CD#t (CD#x (CD#b 2) (CD#i 0))) (CLP (M#t (M#b %))))))) (PU#t (PU#b ,)) (NP (NP (QP (CD#t (CD#b 万)) (CLP (M#t (M#b 元)))) (NP (NN#t (NN#y (NN#x (NN#b 增) (NN#i 加)) (NN#i 值))))) (ADJP (JJ#t (JJ#x (JJ#b 新) (JJ#i 鲜)))) (NP (NN#t (NN#y (NN#y (NN#b 水) (NN#i >用)) (NN#i 量))))) (VP (ADVP (AD#t (AD#y (AD#b 至) (AD#i 少)))) (VP (VV#t (VV#y (VV#b 下) (VV#i 降))) (QP (CD#t (CD#x (CD#b 3) (CD#i 0))) (CLP (M#t (M#b %))))))) (PU#t (PU#b ,)) (IP (NP (NP (ADJP (JJ#t (JJ#y (JJ#b 主) (JJ#i 要)))) (NP (NN#t (NN#y (NN#b 产) (NN#i 品))))) (NP (NN#t (NN#y (NN#b 单) (NN#i 耗))) (NN#t (NN#y (NN#b 指) (NN#i 标))))) (VP (VV#t (VV#b 要)) (VP (VP (VV#t (VV#z (VV#b 接) (VV#i 近)))) (CC#t (CC#b 或)) (VP (VV#t (VV#z (VV#b 达) (VV#i 到))) (NP (NP (NN#t (NN#y (NN#b 国) (NN#i 际)))) (ADJP (JJ#t (JJ#x (JJ#b 先) (JJ#i 进)))) (NP (NN#t (NN#y (NN#b 水) (NN#i 平))))))))))) (PU#t (PU#b 。))))";

		ArrayList<String> output = new ArrayList<String>();

		ConvertZYParser.convert(output, treeStr);

		System.out.println(output);
		// HashMap<String, ArrayList<String>> newDic = new HashMap<String,
		// ArrayList<String>>();
		// for(String word:semanticDic.keySet()) {
		// String codes[] = semanticDic.get(word);
		// for(String code:codes) {
		// if(newDic.containsKey(code)) {
		// newDic.get(code).add(word);
		// } else {
		// ArrayList<String> words = new ArrayList<String>();
		// words.add(word);
		// newDic.put(code, words);
		// }
		// }
		// }
		// Object[] codes = newDic.keySet().toArray();
		// Arrays.sort(codes);
		// try {
		// FileWriter fw = new
		// FileWriter("D:\\workspace\\ACL12\\ACL12\\src\\dict\\Tongyici.txt");
		// for(int i=0;i<codes.length;i++) {
		// String code = (String)codes[i];
		// ArrayList<String> words = newDic.get(code);
		// StringBuilder sb = new StringBuilder();
		// sb.append(code).append(" ");
		// for(String word:words) {
		// sb.append(word).append(" ");
		// }
		// fw.write(sb.toString()+ "\n");
		// }
		// fw.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	public static int getEditDistance(String s, String t) {
		int d[][]; // matrix
		int n; // length of s
		int m; // length of t
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		// Step 1

		n = s.length();
		m = t.length();
		if (n == 0) {
			return m;
		}
		if (m == 0) {
			return n;
		}
		d = new int[n + 1][m + 1];

		// Step 2

		for (i = 0; i <= n; i++) {
			d[i][0] = i;
		}

		for (j = 0; j <= m; j++) {
			d[0][j] = j;
		}

		// Step 3

		for (i = 1; i <= n; i++) {
			s_i = s.charAt(i - 1);
			// Step 4
			for (j = 1; j <= m; j++) {
				t_j = t.charAt(j - 1);
				// Step 5
				if (s_i == t_j) {
					cost = 0;
				} else {
					cost = 1;
				}
				// Step 6
				d[i][j] = Minimum(d[i - 1][j] + 1, d[i][j - 1] + 1,
						d[i - 1][j - 1] + cost);
			}
		}
		// Step 7
		return d[n][m];
	}

	private static int Minimum(int a, int b, int c) {
		int mi;

		mi = a;
		if (b < mi) {
			mi = b;
		}
		if (c < mi) {
			mi = c;
		}
		return mi;
	}

	public static String feasToSVMString(ArrayList<Feature> feas) {
		StringBuilder sb = new StringBuilder();
		int offset = 1;
		for (Feature fea : feas) {
			if (fea.value != 0) {
				sb.append(fea.idx + offset).append(":").append(fea.value)
						.append(" ");
			}
			offset += fea.space;
		}

		return sb.toString().trim();
	}

	public static class Feature {
		int idx;
		int value;
		int space;

		public Feature(int idx, int value, int space) {
			this.idx = idx;
			this.value = value;
			this.space = space;

			if (this.idx >= this.space) {
				bangErrorPOS("feature idx cannot equal or larger than feature space.");
			}
		}
	}

	public static void bangErrorPOS(Object message) {
		try {
			System.err.println("Error: " + message.toString());
			throw new Exception();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static <T> double[] getRPF(ArrayList<ArrayList<T>> goldses,
			ArrayList<ArrayList<T>> systemses) {
		double goldAll = 0;
		double systemAll = 0;
		double hitAll = 0;

		Iterator<ArrayList<T>> goldIts = goldses.iterator();
		Iterator<ArrayList<T>> systemIts = systemses.iterator();

		while (goldIts.hasNext()) {
			Collection<T> golds = goldIts.next();
			Collection<T> systems = systemIts.next();
			goldAll += golds.size();
			systemAll += systems.size();

			Iterator<T> goldIt = golds.iterator();

			while (goldIt.hasNext()) {
				Object gold = goldIt.next();
				Iterator<T> systemIt = systems.iterator();
				while (systemIt.hasNext()) {
					Object system = systemIt.next();
					if (gold.equals(system)) {
						hitAll++;
						break;
					}
				}
			}
		}
		double r = hitAll / goldAll;
		double p = hitAll / systemAll;
		double f = 2 * r * p / (r + p);
		double ret[] = new double[3];
		ret[0] = r;
		ret[1] = p;
		ret[2] = f;
		System.out.println("=====");
		System.out.println("Hit: " + hitAll);
		System.out.println("Gold: " + goldAll);
		System.out.println("System: " + systemAll);
		System.out.println("=====");
		System.out.println("Recall: " + r * 100);
		System.out.println("Precision: " + p * 100);
		System.out.println("F-score: " + f * 100);
		return ret;
	}

	public static <T> void increaseKey(HashMap<T, Integer> map, T key) {
		Integer value = map.get(key);
		if (value == null) {
			map.put(key, 1);
		} else {
			map.put(key, value.intValue() + 1);
		}
	}

	public static MyTreeNode getLowestCommonAncestor(MyTreeNode left,
			MyTreeNode right) {
		MyTreeNode ancestor = null;
		ArrayList<MyTreeNode> leftAns = left.getAncestors();
		ArrayList<MyTreeNode> rightAns = right.getAncestors();
		for (int i = 0; i < leftAns.size() && i < rightAns.size(); i++) {
			if (leftAns.get(i) == rightAns.get(i)) {
				ancestor = leftAns.get(i);
			}
		}
		return ancestor;
	}

	public static boolean isAncestor(MyTreeNode down, MyTreeNode up) {
		if (down == null || up == null) {
			return false;
		}
		boolean ance = false;
		for (MyTreeNode n : down.getAncestors()) {
			if (n == up) {
				ance = true;
				break;
			}
		}
		return ance;
	}

	public static double getValue(HashMap<String, Double> map, String key) {
		if (map == null) {
			return 0;
		}
		if (map.containsKey(key)) {
			return map.get(key);
		} else {
			return 0;
		}
	}

	public static void addMap(HashMap<String, Integer> map, String key, int val) {
		if (map.containsKey(key)) {
			map.put(key, map.get(key).intValue() + val);
		} else {
			map.put(key, val);
		}
	}

	public static void pause(Object message) {
		try {
			System.err.println("Pause: " + message.toString());
			System.err.println("Press g to continue");
			throw new Exception();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// ToDO
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			String line = "";
			while (!(line.equalsIgnoreCase("g"))) {
				line = br.readLine();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void input(Object message) {
		try {
			System.err.println("Pause: " + message.toString());
			System.err.println("Input Parameters: ");
			throw new Exception();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// ToDO
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String line = "";
		while (true) {
			try {
				line = br.readLine().trim();
				String tks[] = line.split("\\s+");
				ILP.a_num = Double.parseDouble(tks[0]);
				ILP.b_gen = Double.parseDouble(tks[1]);
				ILP.c_per = Double.parseDouble(tks[2]);
				ILP.d_ani = Double.parseDouble(tks[3]);
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			break;
		}
	}
}

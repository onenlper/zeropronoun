package align;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.Util;

public class DocumentMap {

	private static boolean initialized = false;

	public static boolean isInited() {
		return initialized;
	}

	public static ArrayList<DocumentMap> documentMaps = new ArrayList<DocumentMap>();

	private static HashMap<String, DocumentMap> engDocMaps = new HashMap<String, DocumentMap>();
	private static HashMap<String, DocumentMap> chiDocMaps = new HashMap<String, DocumentMap>();

	public static void clear() {
		initialized = false;
		documentMaps.clear();
		engDocMaps.clear();
		chiDocMaps.clear();
	}

	public static DocumentMap getDocumentMap(String id, String lang) {
		if (lang.equalsIgnoreCase("eng")) {
			return engDocMaps.get(id);
		} else if (lang.equalsIgnoreCase("chi")) {
			return chiDocMaps.get(id);
		} else {
			Common.bangErrorPOS("Not support language");
		}
		return null;
	}

	String parallel;
	public DocForAlign engDoc;
	public DocForAlign chiDoc;

	public int id;

	public DocumentMap(String parallel, int id) {
		this.id = id;
		this.parallel = parallel;
		String tokens[] = parallel.split("#");
		chiDoc = new DocForAlign("chi");
		engDoc = new DocForAlign("eng");
		chiDoc.mapDoc = engDoc;
		engDoc.mapDoc = chiDoc;

		chiDocMaps.put("chi", this);
		engDocMaps.put("eng", this);
		documentMaps.add(this);
	}

	public static class Unit {
		int id;
		int visable = 0;
		ArrayList<Unit> mapUnit;
		ArrayList<Double> mapProb;

		String token;
		public SentForAlign sentForAlign;

		public CoNLLPart part;
		public CoNLLSentence sentence;

		public int indexInSentence;

		// public ArrayList<Span> spans;

		public ArrayList<Mention> mentions;

		// public void addSpan(Span s) {
		// // check whether contain it already
		// for (Span tmp : spans) {
		// if (tmp.start == s.start && tmp.end == s.end) {
		// return;
		// }
		// }
		// // may need to check here
		// this.spans.add(s);
		// }

		public void addMention(Mention m) {
			for (Mention tmp : this.mentions) {
				if (tmp.start == m.start && tmp.end == m.end) {
					return;
				}
			}
			this.mentions.add(m);
		}

		public Unit() {
			this.mapUnit = new ArrayList<Unit>();
			// this.spans = new ArrayList<Span>();
			this.mapProb = new ArrayList<Double>();
			this.visable = 0;
			this.mentions = new ArrayList<Mention>();
		}

		public Unit(int id) {
			this();
			this.id = id;
		}

		public Unit(int id, String token) {
			this(id);
			this.token = token;
		}

		public int getId() {
			return this.id;
		}

		public void addMapUnit(Unit unit) {
			this.mapUnit.add(unit);
			unit.mapUnit.add(this);
		}

		public void addMapUnit(Unit unit, double prob) {
			// insert prob to array, and return position
			int k1 = this.insertProb(prob);
			this.mapUnit.add(k1, unit);

			int k2 = unit.insertProb(prob);
			unit.mapUnit.add(k2, this);
		}

		// insert prob to array, and return position
		private int insertProb(double prob) {
			int insert = this.mapProb.size();
			for (int i = 0; i < this.mapProb.size(); i++) {
				if (prob > this.mapProb.get(i)) {
					insert = i;
					break;
				}
			}
			this.mapProb.add(insert, prob);
			return insert;
		}

		public String getToken() {
			return this.token;
		}

		public ArrayList<Unit> getMapUnit() {
			return this.mapUnit;
		}

		public ArrayList<Double> getMapProb() {
			return this.mapProb;
		}
	}

	public static class SentForAlign {
		int id;
		public ArrayList<Unit> units;
		public SentForAlign mapSentence;
		DocForAlign doc;

		public SentForAlign(int id) {
			this.id = id;
			this.units = new ArrayList<Unit>();
		}

		public void addUnit(Unit unit) {
			this.units.add(unit);
			unit.sentForAlign = this;
			this.doc.units.put(unit.id, unit);
		}

		public String getText() {
			StringBuilder sb = new StringBuilder();
			for (Unit u : units) {
				sb.append(u.token).append(" ");
			}
			return sb.toString().trim();
		}
	}

	public static class DocForAlign {
		public String docID;
		ArrayList<SentForAlign> sentences;
		DocForAlign mapDoc;

		public int spanSize = 0;

		// CoNLLDocument document;
		// public Map<String, HashSet<String>> spanMatch = new HashMap<String,
		// HashSet<String>>();
		// public Map<String, HashSet<String>> headMatch = new HashMap<String,
		// HashSet<String>>();

		private HashMap<Integer, Unit> units;

		public DocForAlign(String docID) {
			this.docID = docID;
			sentences = new ArrayList<SentForAlign>();
			units = new HashMap<Integer, Unit>();
		}

		public void addSent(SentForAlign sent) {
			this.sentences.add(sent);
			sent.doc = this;
		}

		public Unit getUnit(int id) {
			return this.units.get(id);
		}
	}

	private static final Pattern baPattern = Pattern
			.compile("([^-]*)-([^-]*)-(.*)");

	public static void loadRealBAAlignResult(String alignFolder, String lang) {
		if (initialized) {
			Common.bangErrorPOS("Already inited!!!");
		}
		initialized = true;
		ArrayList<String> lines = null;
		if (lang.equalsIgnoreCase("eng")) {
			lines = Common.getLines("english_list_all");
		} else if (lang.equalsIgnoreCase("chi")) {
			lines = Common.getLines("chinese_list_all");
		}
		for (int i = 0; i < lines.size(); i++) {
			String docName = Util.getID(lines.get(i));
			String parallel = docName + " # " + docName;
			DocumentMap documentMap = new DocumentMap(parallel, i);

			// ArrayList<String> alignContent = Common.getLines(alignFolder
			// + File.separator + i + ".align");
			ArrayList<String> alignIDs = Common.getLines(alignFolder
					+ File.separator + i + ".id");
			ArrayList<String> alignsoftContent = Common.getLines(alignFolder
					+ File.separator + i + ".alignsoft");

			ArrayList<String> engStrs = Common.getLines(alignFolder
					+ File.separator + i + ".eng");
			ArrayList<String> chiStrs = Common.getLines(alignFolder
					+ File.separator + i + ".chi");

			// assign the most confidence map, other than map with confidence
			// greater than 0.5
			for (int j = 0; j < alignsoftContent.size(); j++) {
				SentForAlign engSent = new SentForAlign(j);
				SentForAlign chiSent = new SentForAlign(j);
				engSent.mapSentence = chiSent;
				chiSent.mapSentence = engSent;

				documentMap.engDoc.addSent(engSent);
				documentMap.chiDoc.addSent(chiSent);

				String chiIDStr = alignIDs.get(j * 3 + 1).trim();
				String chiIDs[] = chiIDStr.split("\\s+");

				String engIDStr = alignIDs.get(j * 3 + 2).trim();
				String engIDs[] = engIDStr.split("\\s+");

				String alignsoftStr = alignsoftContent.get(j);
				String alignsofts[] = alignsoftStr.split("\\s+");

				String chiTks[] = chiStrs.get(j).split("\\s+");
				String engTks[] = engStrs.get(j).split("\\s+");

				// create units
				for (int m = 0; m < chiTks.length; m++) {
					int id = Integer.parseInt(chiIDs[m]);
					String tk = chiTks[m];
					Unit unit = new Unit(id, tk);
					chiSent.addUnit(unit);
				}

				for (int m = 0; m < engTks.length; m++) {
					int id = Integer.parseInt(engIDs[m]);
					String tk = engTks[m];
					Unit unit = new Unit(id, tk);
					engSent.addUnit(unit);
				}

				// create map
				for (int m = 0; m < alignsofts.length; m++) {
					// chi-eng-prob
					Matcher matcher = baPattern.matcher(alignsofts[m]);
					if (matcher.find()) {
						Unit chiUnit = chiSent.units.get(Integer
								.parseInt(matcher.group(1)));
						Unit engUnit = engSent.units.get(Integer
								.parseInt(matcher.group(2)));

						chiUnit.addMapUnit(engUnit,
								Double.valueOf(matcher.group(3)));
					}
				}
			}
		}
	}

	public static ArrayList<SentForAlign[]> loadRealBAAlignResult(
			String alignFolder) {
		if (initialized) {
			Common.bangErrorPOS("Already inited!!!");
		}
		ArrayList<SentForAlign[]> alignCache = new ArrayList<SentForAlign[]>();
		initialized = true;
		// ArrayList<String> lines = Common.getLines(alignFolder +
		// "/parallelMap");
		// for (int i = 0; i < lines.size(); i++) {
		// String parallel = lines.get(i);
		int i = 0;
		DocumentMap documentMap = new DocumentMap("", i);

		// ArrayList<String> alignContent = Common.getLines(alignFolder
		// + File.separator + i + ".align");
		// ArrayList<String> alignIDs = Common.getLines(alignFolder
		// + File.separator + i + ".id");

		ArrayList<String> alignsoftContent = Common.getLines(alignFolder
				+ File.separator + "training.alignsoft");

		ArrayList<String> engStrs = Common.getLines(alignFolder
				+ File.separator + "training.e");
		ArrayList<String> chiStrs = Common.getLines(alignFolder
				+ File.separator + "training.f");

		// assign the most confidence map, other than map with confidence
		// greater than 0.5
		for (int j = 0; j < alignsoftContent.size(); j++) {
			SentForAlign[] pair = new SentForAlign[2];

			SentForAlign engSent = new SentForAlign(j);
			SentForAlign chiSent = new SentForAlign(j);

			pair[0] = chiSent;
			pair[1] = engSent;

			engSent.mapSentence = chiSent;
			chiSent.mapSentence = engSent;

			documentMap.engDoc.addSent(engSent);
			documentMap.chiDoc.addSent(chiSent);

			// String chiIDStr = alignIDs.get(j * 3 + 1).trim();
			// String chiIDs[] = chiIDStr.split("\\s+");

			// String engIDStr = alignIDs.get(j * 3 + 2).trim();
			// String engIDs[] = engIDStr.split("\\s+");

			String alignsoftStr = alignsoftContent.get(j);
			String alignsofts[] = alignsoftStr.split("\\s+");

			String chiTks[] = chiStrs.get(j).split("\\s+");
			String engTks[] = engStrs.get(j).split("\\s+");

			alignCache.add(pair);

			// create units
			try {
				for (int m = 0; m < chiTks.length; m++) {
					int id = m;
					String tk = chiTks[m];
					Unit unit = new Unit(id, tk);
					chiSent.addUnit(unit);
				}
			} catch (Exception e) {
				Common.bangErrorPOS(alignFolder + " # " + i + " $ " + j);
			}

			for (int m = 0; m < engTks.length; m++) {
				int id = m;
				String tk = engTks[m];
				Unit unit = new Unit(id, tk);
				engSent.addUnit(unit);
			}

			// create map
			for (int m = 0; m < alignsofts.length; m++) {
				// chi-eng-prob
				Matcher matcher = baPattern.matcher(alignsofts[m]);
				if (matcher.find()) {
					Unit chiUnit = chiSent.units.get(Integer.parseInt(matcher
							.group(1)));
					Unit engUnit = engSent.units.get(Integer.parseInt(matcher
							.group(2)));

					chiUnit.addMapUnit(engUnit,
							Double.valueOf(matcher.group(3)));
				}
			}
		}
		// }
		return alignCache;
	}

	public static void loadRealGizaAlignResult(String alignFolder) {
		if (initialized) {
			Common.bangErrorPOS("Already inited!!!");
		}
		initialized = true;
		ArrayList<String> lines = Common.getLines("parallelMap");
		for (int i = 0; i < lines.size(); i++) {
			String parallel = lines.get(i);
			DocumentMap documentMap = new DocumentMap(parallel, i);
			ArrayList<String> alignContent = Common.getLines(alignFolder
					+ File.separator + i + ".align");
			ArrayList<String> alignIDs = Common.getLines(alignFolder
					+ File.separator + i + ".id");

			for (int j = 0; j < alignContent.size() / 3; j++) {
				SentForAlign engSent = new SentForAlign(j);
				SentForAlign chiSent = new SentForAlign(j);
				engSent.mapSentence = chiSent;
				chiSent.mapSentence = engSent;

				documentMap.engDoc.addSent(engSent);
				documentMap.chiDoc.addSent(chiSent);

				String chiStr = alignContent.get(j * 3 + 1).trim();
				String chiIDStr = alignIDs.get(j * 3 + 1).trim();
				String chiTks[] = chiStr.split("\\s+");
				String chiIDs[] = chiIDStr.split("\\s+");

				if (chiTks.length != chiIDs.length) {
					System.out.println(chiStr);
					System.out.println(chiIDStr);
					System.out.println(chiTks.length);
					System.out.println(chiIDs.length);
					System.out.println(i + ":" + j);
					Common.bangErrorPOS("");
				}
				for (int m = 0; m < chiTks.length; m++) {
					String tk = chiTks[m];
					int id = Integer.parseInt(chiIDs[m]);
					Unit unit = new Unit(id, tk);
					chiSent.addUnit(unit);
				}

				String engStr = alignContent.get(j * 3 + 2).trim();
				String engIDStr = alignIDs.get(j * 3 + 2).trim();
				ArrayList<String[]> engTks = parseGizaEngSide(engStr);
				String engIDs[] = engIDStr.split("\\s+");

				if (engTks.size() != engIDs.length) {
					System.out.println(engStr);
					System.out.println(engIDStr);
					System.out.println(engTks.size());
					System.out.println(engIDs.length);
					System.out.println(i + ":" + j);
					Common.bangErrorPOS("");
				}

				for (int m = 0; m < engTks.size(); m++) {
					String tk = engTks.get(m)[0];
					int id = Integer.parseInt(engIDs[m]);

					Unit unit = new Unit(id, tk);
					engSent.addUnit(unit);

					// construct map
					String maps[] = engTks.get(m)[1].split("\\s+");
					for (String map : maps) {
						if (!map.isEmpty()) {
							int index = Integer.parseInt(map) - 1;
							Unit chiUnit = chiSent.units.get(index);
							unit.addMapUnit(chiUnit);
						}
					}
				}
			}
		}

	}

	private static ArrayList<String[]> parseGizaEngSide(String str) {
		String tokens[] = str.trim().split("\\s+");

		ArrayList<String[]> ret = new ArrayList<String[]>();

		int a = str.indexOf("({");
		// skip NULL
		int b = str.indexOf("})", a + 2);

		while ((a = str.indexOf("({", b + 2)) != -1) {
			String word = str.substring(b + 2, a).trim();
			b = str.indexOf("})", a + 2);
			String map = str.substring(a + 2, b).trim();
			String arr[] = new String[2];
			arr[0] = word;
			arr[1] = map;
			ret.add(arr);
		}
		return ret;
	}

	public static void loadSentenceAlignResult() {
		if (initialized) {
			Common.bangErrorPOS("Already inited!!!");
		}
		initialized = true;
		ArrayList<String> lines = Common.getLines("parallelMap");
		for (int i = 0; i < lines.size(); i++) {
			String parallel = lines.get(i);
			DocumentMap documentMap = new DocumentMap(parallel, i);
			ArrayList<String> alignContent = Common.getLines(Util.modifyAlign
					+ i + ".align");
			int chID = 0;
			int enID = 0;
			for (int j = 0; j < alignContent.size() / 3; j++) {
				String eng = alignContent.get(j * 3);
				SentForAlign engSent = new SentForAlign(j);
				SentForAlign chiSent = new SentForAlign(j);
				engSent.mapSentence = chiSent;
				chiSent.mapSentence = engSent;

				documentMap.engDoc.addSent(engSent);
				documentMap.chiDoc.addSent(chiSent);

				for (String tk : eng.split("\\s+")) {
					Unit unit = new Unit(enID++, tk);
					engSent.addUnit(unit);
				}
				String chi = alignContent.get(j * 3 + 1);
				for (String tk : chi.split("\\s+")) {
					Unit unit = new Unit(chID++, tk);
					chiSent.addUnit(unit);
				}
			}
		}
	}

	public static void outputAlignFormatForBA(String outputBase, boolean gold) {
		ArrayList<String> chiDocStrs = new ArrayList<String>();
		ArrayList<String> engDocStrs = new ArrayList<String>();
		ArrayList<String> engTreeStrs = new ArrayList<String>();

		double ch = 0;
		double en = 0;
		for (int i = 0; i < documentMaps.size(); i++) {

			ArrayList<String> alignWordIDs = new ArrayList<String>();
			DocumentMap documentMap = documentMaps.get(i);
			DocForAlign chiDoc = documentMap.chiDoc;
			DocForAlign engDoc = documentMap.engDoc;

			CoNLLDocument engCoNLLDoc = new CoNLLDocument(Util.getFullPath(
					engDoc.docID, "eng", gold));
			int engWordID = 0;
			for (int m = 0; m < chiDoc.sentences.size(); m++) {
				alignWordIDs.add("===========================");
				SentForAlign s = chiDoc.sentences.get(m);
				StringBuilder sbStr = new StringBuilder();
				StringBuilder sbID = new StringBuilder();
				for (Unit u : s.units) {
					if (u.visable != -1) {
						ch++;
						sbStr.append(u.token).append(" ");
						sbID.append(u.id).append(" ");
					}
				}
				chiDocStrs.add(sbStr.toString().trim());
				alignWordIDs.add(sbID.toString().trim());

				s = engDoc.sentences.get(m);

				int enTs = s.units.size();
				CoNLLWord firstEnWord = engCoNLLDoc.getWord(engWordID);
				CoNLLWord lastEnWord = engCoNLLDoc
						.getWord(engWordID + enTs - 1);

				sbStr = new StringBuilder();
				sbID = new StringBuilder();
				for (Unit u : s.units) {
					if (u.visable != -1) {
						en++;
						sbStr.append(u.token).append(" ");
						sbID.append(u.id).append(" ");
					} else {
						// change the parse tree
						CoNLLWord w = engCoNLLDoc.getWord(u.id);
						CoNLLSentence conllSent = w.sentence;
						MyTreeNode root = conllSent.syntaxTree.root;
						MyTreeNode leaf = root.getLeaves().get(
								w.indexInSentence);
						leaf.toRemove = true;
						if (!leaf.value.equalsIgnoreCase(w.orig)) {
							Common.bangErrorPOS("");
						}
					}
				}

				MyTreeNode root = null;
				if (firstEnWord.sentence == lastEnWord.sentence) {
					root = firstEnWord.sentence.getSyntaxTree().root;
					root.removeToRemoves();
					root.value = "S";
				} else {
					root = new MyTreeNode("S");
					CoNLLSentence ss = firstEnWord.getSentence();
					ss.getSyntaxTree().root.value = "S";
					ss.getSyntaxTree().root.removeToRemoves();
					root.addChild(ss.getSyntaxTree().root);
					for (int t = engWordID; t < engWordID + enTs; t++) {
						CoNLLSentence tmp = engCoNLLDoc.getWord(t).sentence;
						if (tmp != ss) {
							tmp.getSyntaxTree().root.value = "S";
							tmp.getSyntaxTree().root.removeToRemoves();
							root.addChild(tmp.getSyntaxTree().root);
							ss = tmp;
						}
					}
				}
				engTreeStrs.add(root.getTreeBankStyle(true).trim());

				engDocStrs.add(sbStr.toString().trim());
				alignWordIDs.add(sbID.toString().trim());
				engWordID += enTs;
			}

			// for(CoNLLPart p : engCoNLLDoc.getParts()) {
			// for(CoNLLSentence s : p.getCoNLLSentences()) {
			// for(MyTreeNode leaf : s.getSyntaxTree().root.getLeaves() ) {
			// if(leaf.toRemove) {
			// System.out.println(leaf.value);
			// }
			// }
			// }
			// }

			Common.outputLines(alignWordIDs, outputBase + "/align/"
					+ File.separator + i + ".id");
		}
		Common.outputLines(chiDocStrs, outputBase + "docs/docs.f");
		Common.outputLines(engDocStrs, outputBase + "docs/docs.e");
		Common.outputLines(engTreeStrs, outputBase + "docs/docs.etrees");

		System.out.println(ch + " # " + en + ":" + (ch / en));
	}

	public static void outputAlignFormatForGiza(String chiDocPath,
			String engDocPath, String alignFolder) {
		ArrayList<String> chiDocStrs = new ArrayList<String>();
		ArrayList<String> engDocStrs = new ArrayList<String>();
		double ch = 0;
		double en = 0;
		for (int i = 0; i < documentMaps.size(); i++) {

			ArrayList<String> alignWordIDs = new ArrayList<String>();
			DocumentMap documentMap = documentMaps.get(i);
			DocForAlign chiDoc = documentMap.chiDoc;
			DocForAlign engDoc = documentMap.engDoc;

			for (int m = 0; m < chiDoc.sentences.size(); m++) {
				alignWordIDs.add("===========================");
				SentForAlign s = chiDoc.sentences.get(m);
				StringBuilder sbStr = new StringBuilder();
				StringBuilder sbID = new StringBuilder();
				for (Unit u : s.units) {
					if (u.visable != -1) {
						ch++;
						sbStr.append(u.token).append(" ");
						sbID.append(u.id).append(" ");
					}
				}
				chiDocStrs.add(sbStr.toString().trim());
				alignWordIDs.add(sbID.toString().trim());

				s = engDoc.sentences.get(m);
				sbStr = new StringBuilder();
				sbID = new StringBuilder();
				for (Unit u : s.units) {
					if (u.visable != -1) {
						en++;
						sbStr.append(u.token).append(" ");
						sbID.append(u.id).append(" ");
					}
				}
				engDocStrs.add(sbStr.toString().trim());
				alignWordIDs.add(sbID.toString().trim());
			}
			Common.outputLines(alignWordIDs, alignFolder + File.separator + i
					+ ".id");
		}
		Common.outputLines(chiDocStrs, chiDocPath);
		Common.outputLines(engDocStrs, engDocPath);
		System.out.println(ch + " # " + en + ":" + (ch / en));
	}

	public static void main(String args[]) {
		// loadSentenceAlignResult();
		// outputAlignFormatForGiza(Util.headAlignBase + "chiDocs",
		// Util.headAlignBase + "engDocs", Util.headAlignBase + "/align/");
		loadRealGizaAlignResult(Util.headAlignBaseGold + "/align/");
	}
}

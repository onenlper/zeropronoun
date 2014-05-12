package model.CoNLL;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import util.Common;
import align.DocumentMap;

/*
 * CoNLL-2012 Format Document
 */
public class CoNLLDocument {

	public boolean addZero;

	public enum DocType {
		Article, Conversation
	}

	public DocType type;

	public DocType getType() {
		return type;
	}

	public void setType(DocType type) {
		this.type = type;
	}

	private ArrayList<String> rawLines;

	private String documentID;

	private String filePath;

	private String filePrefix;

	public String language;

	private ArrayList<CoNLLPart> parts;

	public CoNLLDocument() {
		this.parts = new ArrayList<CoNLLPart>();
	}

	public CoNLLDocument(String path, String lang) {
		path = path.replace("v6", "v4").replace("_gold_parse_conll",
				"_auto_conll");
		if (!(new File(path).exists())) {
			path = path.replace("v5", "v4").replace("v6", "v4");
		}

		if (!(new File(path).exists())) {
			path = path.replace("_auto_conll", "_gold_conll");
		}
		this.setType(DocType.Article);
		this.filePath = path;
		this.language = lang;
		this.rawLines = Common.getLines(path);
		int i = path.lastIndexOf(".");
		if (i != -1) {
			this.filePrefix = path.substring(0, i);
		}
		this.parts = new ArrayList<CoNLLPart>();
		this.parseFile();
	}
	
	public CoNLLDocument(String path) {
		path = path.replace("v6", "v4").replace("_gold_parse_conll",
				"_auto_conll");
		if (!(new File(path).exists())) {
			path = path.replace("v5", "v4").replace("v6", "v4");
		}

		if (!(new File(path).exists())) {
			path = path.replace("_auto_conll", "_gold_conll");
		}
		this.setType(DocType.Article);
		this.filePath = path;
		if (this.language==null && (filePath.contains("chinese") || filePath.contains("chi"))) {
			this.language = "chinese";
		}
		if (this.language==null && (filePath.contains("english") || filePath.contains("eng"))) {
			this.language = "english";
		}
		this.rawLines = Common.getLines(path);
		int i = path.lastIndexOf(".");
		if (i != -1) {
			this.filePrefix = path.substring(0, i);
		}
		this.parts = new ArrayList<CoNLLPart>();
		this.parseFile();
	}
	
	public CoNLLWord getWord(int id) {
		for(CoNLLPart part : this.getParts()) {
			if(id>=part.getWordCount()) {
				id -= part.getWordCount();
			} else {
				return part.getWord(id);
			}
		}
		return null;
	}

	/*
	 * parse CoNLL format file
	 */
	private void parseFile() {
		CoNLLPart part = null;
		CoNLLSentence sentence = null;
		StringBuilder parseBits = null;
		StringBuilder sentenceStr = null;
		int wordIdx = 0;
		String previousSpeaker = "";
		for (String line : rawLines) {
			// System.out.println(line);
			if (line.startsWith("#end document")) {
				part.postProcess();
				continue;
			}
			// new part
			if (line.startsWith("#begin document")) {
				part = new CoNLLPart(this);
				part.label = line;
				int a = line.indexOf("(");
				int b = line.indexOf(")");
				part.docName = line.substring(a+1, b);
				wordIdx = 0;
				part.setDocument(this);
				this.parts.add(part);
				sentence = null;
				
				if(DocumentMap.isInited()) {
					part.documentMap = DocumentMap.getDocumentMap(part.docName, part.lang);

					if (part.documentMap != null) {
						if (part.lang.equalsIgnoreCase("eng")) {
							part.itself = part.documentMap.engDoc;
							part.counterpart = part.documentMap.chiDoc;
						} else if (part.lang.equalsIgnoreCase("chi")) {
							part.itself = part.documentMap.chiDoc;
							part.counterpart = part.documentMap.engDoc;
						} else {
							Common.bangErrorPOS("Not Supported Language");
						}
					}
				}
				continue;
			}
			// end of one sentence
			if (line.trim().isEmpty()) {
				sentence.setSentence(sentenceStr.toString());
				sentence.setWordsCount(sentence.words.size());
				String parseTree = parseBits.toString().replace("(", " (")
						.trim();
				if (parseTree.startsWith("( (")) {
					parseTree = "(TOP " + parseTree.substring(1);
				}
				sentence.addSyntaxTree(parseTree);
				part.addSentence(sentence);
				sentence = null;
				continue;
			}
			if (sentence == null) {
				sentence = new CoNLLSentence();
				parseBits = new StringBuilder();
				sentenceStr = new StringBuilder();
			}
			String tokens[] = line.split("\\s+");
			CoNLLWord word = new CoNLLWord();

			word.sourceLine = line;

			// 1 Document ID
			int a = tokens[0].indexOf("/");
			part.folder = tokens[0].substring(0, a);
			
			this.documentID = tokens[0];
			// 2 Part number
			part.setPartID(Integer.valueOf(tokens[1]));
			part.setPartName(part.getDocument().getDocumentID()
					.replace("/", "-")
					+ "_" + part.getPartID());
			// 3 Word number
			// 5 Part-of-Speech
			String pos = tokens[4];
			word.setPosTag(pos);
			// 4 Word itself
			String wordStr = tokens[3];
			String stem = wordStr;
			// if language is english, use wordnet to do stem
			word.setOrig(wordStr);
			word.setWord(stem.toLowerCase());

			// 6 Parse bit
			parseBits.append(tokens[5].replace("*",
					" (" + pos + " " + wordStr.toLowerCase() + ")").replace(
					"(", " ("));
			// 7 Predicate lemma
			word.setPredicateLemma(tokens[6]);
			// 8 Predicate Frameset ID
			word.setPredicateFramesetID(tokens[7]);
			// 9 Word sense
			word.setWordSense(tokens[8]);
			// 10 Speaker/Author
			if (!previousSpeaker.isEmpty()
					&& !tokens[9].equalsIgnoreCase(previousSpeaker)) {
				this.setType(DocType.Conversation);
			}
			previousSpeaker = tokens[9];
			sentence.setSpeaker(tokens[9]);
			word.speaker = tokens[9];
			// 11 Named Entities
			word.setRawNamedEntity(tokens[10]);
			// 12 Predicate Arguments
			StringBuilder argument = new StringBuilder();
			for (int i = 11; i < tokens.length - 1; i++) {
				argument.append(tokens[i]).append(" ");
			}
			word.setPredicateArgument(argument.toString().trim());
			// 13 Coreference
			word.setRawCoreference(tokens[tokens.length - 1]);
			sentence.addWord(word);
			word.setIndex(wordIdx);
			sentenceStr.append(wordStr).append(" ");
			wordIdx++;
		}
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public ArrayList<String> getRawLines() {
		return rawLines;
	}

	public void setRawLines(ArrayList<String> rawLines) {
		this.rawLines = rawLines;
	}

	public String getDocumentID() {
		return documentID;
	}

	public void setDocumentID(String documentID) {
		this.documentID = documentID;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getFilePrefix() {
		return filePrefix;
	}

	public void setFilePrefix(String filePrefix) {
		this.filePrefix = filePrefix;
	}

	public ArrayList<CoNLLPart> getParts() {
		return parts;
	}

	public void setParts(ArrayList<CoNLLPart> parts) {
		this.parts = parts;
	}

	/*
	 * Test CoNLL format converter
	 */
	public static void main(String args[]) {
		String conllPath = "";
		// conllPath =
		// "/users/yzcchen/CoNLL-2012/conll-2012/v1/data/train/data/chinese/annotations/nw/xinhua/00/chtb_0001.v1_gold_conll";
//		conllPath = "/users/yzcchen/CoNLL-2012/conll-2012/v1/data/train/data/english/annotations/bc/cctv/00/cctv_0001.v1_auto_conll";
//		CoNLLDocument document = new CoNLLDocument(conllPath);
//		System.out.println("Document ID: " + document.getDocumentID());
//		for (CoNLLPart part : document.parts) {
//			System.out.println("Part ID: " + part.getPartID());
//			System.out
//					.println("===================sentences===================");
//			for (CoNLLSentence sentence : part.getCoNLLSentences()) {
//				System.out.println(sentence.getSentence());
//			}
//		}
//		for (CoNLLPart part : document.parts) {
//			System.out.println("Part ID: " + part.getPartID());
//			System.out
//					.println("===================named entities================");
//			for (Element element : part.getNameEntities()) {
//				System.out.println(element);
//			}
//		}
//		for (CoNLLPart part : document.parts) {
//			System.out.println("Part ID: " + part.getPartID());
//			System.out
//					.println("===================coreference chains================");
//			for (Entity entity : part.getChains()) {
//				StringBuilder sb = new StringBuilder();
//				for (Mention em : entity.mentions) {
//					sb.append(em).append(" ");
//				}
//				System.out.println(sb.toString());
//			}
//		}
		HashMap<String, Integer> pnCount = new HashMap<String, Integer>();
		int overall = 0;
		CoNLLDocument d = new CoNLLDocument("train_auto_conll");
		for(CoNLLPart part : d.getParts()) {
			for(CoNLLSentence s : part.getCoNLLSentences()) {
				for(CoNLLWord w : s.getWords()) {
					if(w.getPosTag().equals("PN")) {
						Integer c = pnCount.get(w.getWord());
						if(c==null) {
							pnCount.put(w.getWord(), 1);
						} else {
							pnCount.put(w.getWord(), 1 + c.intValue());
						}
						overall++;
					}
				}
			}
		}
		for(String key : pnCount.keySet()) {
			if(pnCount.get(key)>300) {
				double percent = pnCount.get(key)/(overall * 1.0);
				System.out.println(key + ":" + pnCount.get(key) + "\t" + percent);
			}
		}
		System.out.println(overall);
	}
}

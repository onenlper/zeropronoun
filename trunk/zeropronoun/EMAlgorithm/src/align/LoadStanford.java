package align;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import model.stanford.StanfordResult;
import model.stanford.StanfordSentence;
import model.stanford.StanfordToken;
import model.stanford.StanfordXMLReader;
import util.Common;

public class LoadStanford {

	static String base = "/users/yzcchen/chen3/ijcnlp2013/wordAlignXXX/";
	static String sourceLang;

	static String targetLang;

	public static void main(String args[]) {

//		outputCoNLL3();
	}

	static int sentenceNumber = 0;

	static HashSet<String> nes = new HashSet<String>();
	static ArrayList<String> parallelMaps;

	static ArrayList<String> loadSpeaker(String path) {
		ArrayList<String> speakerLines = Common.getLines(path);
		ArrayList<String> speakers = new ArrayList<String>();
		for (String line : speakerLines) {
			speakers.addAll(Arrays.asList(line.trim().split("\\s+")));
		}
		return speakers;
	}

	private static void outputCoNLL3() {
		String base = "/users/yzcchen/chen3/zeroEM/EMAlgorithm/src/";

		String docName = "setting3";
		base = base + docName + "/";
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("#begin document (" + docName + "); part 000");

		
		StanfordResult sr = StanfordXMLReader.read(base + "/std/3.eng.aa.xml");
		run(sr, docName, lines);
		
		sr = StanfordXMLReader.read(base + "/std/3.eng.ab.xml");
		run(sr, docName, lines);
		
		sr = StanfordXMLReader.read(base + "/std/3.eng.ac.xml");
		run(sr, docName, lines);
		
		lines.add("#end document");
		// lines.add("");
		Common.outputLines(lines, base + "/conll/eng.conll");

	}
	
	private static void outputCoNLL2() {
		String base = "/users/yzcchen/chen3/zeroEM/EMAlgorithm/src/";

		String docName = "setting2";
		base = base + docName + "/";
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("#begin document (" + docName + "); part 000");

		
		StanfordResult sr = StanfordXMLReader.read(base + "/std/2.eng.aa.xml");
		run(sr, docName, lines);
		
		sr = StanfordXMLReader.read(base + "/std/2.eng.ab.xml");
		run(sr, docName, lines);
		
		sr = StanfordXMLReader.read(base + "/std/2.eng.ac.xml");
		run(sr, docName, lines);
		
		sr = StanfordXMLReader.read(base + "/std/2.eng.ad.xml");
		run(sr, docName, lines);
		
		lines.add("#end document");
		// lines.add("");
		Common.outputLines(lines, base + "/conll/eng.conll");

	}
	
	private static void outputCoNLL1() {
		String base = "/users/yzcchen/chen3/zeroEM/EMAlgorithm/src/";

		String docName = "setting1";
		base = base + docName + "/";
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("#begin document (" + docName + "); part 000");

		
		StanfordResult sr = StanfordXMLReader.read(base + "/std/1.eng.aa.xml");
		run(sr, docName, lines);
		
		sr = StanfordXMLReader.read(base + "/std/1.eng.ab.xml");
		run(sr, docName, lines);
		
		sr = StanfordXMLReader.read(base + "/std/1.eng.ac.xml");
		run(sr, docName, lines);
		
		lines.add("#end document");
		// lines.add("");
		Common.outputLines(lines, base + "/conll/eng.conll");

	}

	private static void run(StanfordResult sr, String docName,
			ArrayList<String> lines) {
		for (StanfordSentence ss : sr.sentences) {
			sentenceNumber++;
			ss.parseTree.root.setAllMark(true);
			String parseTree = ss.parseTree.root.getTreeBankStyle(true)
					.replace("ROOT", "TOP").replace("NP-TMP", "NP");
			
			int from = 0;
			int to = 0;

			for (int i = 0; i < ss.tokens.size(); i++) {
				StanfordToken token = ss.tokens.get(i);
				StringBuilder sb = new StringBuilder();

				// documentID
				sb.append(docName).append("\t");
				// partID
				sb.append("0").append("\t");
				// word number
				sb.append(token.id).append("\t");
				// word itself
				sb.append(token.word).append("\t");
				// Part-of-Speech
				sb.append(token.POS).append("\t");
				// Parse bit
				String key = "(" + token.POS + " " + token.word + ")";
				to = parseTree.indexOf(key, from);
				if (to == -1) {
					System.out.println(parseTree + "#" + key);
					System.out.println(token.POS);
					System.out.println(token.word);
					Common.bangErrorPOS(parseTree);
				}
				// find next (
				String segTree = parseTree.substring(0, to).trim() + "*";

				int till = parseTree.indexOf("(", to + key.length());
				if (till == -1) {
					till = parseTree.length();
				}
				segTree += parseTree.substring(to + key.length(), till);
				sb.append(segTree.replaceAll("\\s+", "")).append("\t");
				parseTree = parseTree.substring(till);
				// Predicate lemma
				String lemma = "-";
				if (!token.lemma.isEmpty()) {
					lemma = token.lemma;
				}
				sb.append(lemma).append("\t");
				// Predicate Frameset ID
				sb.append("-").append("\t");
				// Word sense
				sb.append("-").append("\t");
				// TODO
				// Speaker/Author
				String speaker = "-";
				sb.append(speaker).append("\t");
				// Named Entities
				String ne = token.ner.replace("ORGANIZATION", "ORG")
						.replace("ORDINAL", "CARDINAL")
						.replace("LOCATION", "LOC");
				nes.add(ne);
				String preNE = "O";
				String nextNE = "O";
				if (i > 0) {
					preNE = ss.tokens.get(i - 1).ner;
				}
				if (i < ss.tokens.size() - 1) {
					nextNE = ss.tokens.get(i + 1).ner;
				}

				String neToken = "*";
				if (!ne.equalsIgnoreCase("O") && !preNE.equals(ne)
						&& !nextNE.equals(ne)) {
					neToken = "(" + ne + ")";
				} else if (!ne.equalsIgnoreCase("O") && !preNE.equals(ne)
						&& nextNE.equals(ne)) {
					neToken = "(" + ne + "*";
				} else if (!ne.equalsIgnoreCase("O") && preNE.equals(ne)
						&& !nextNE.equals(ne)) {
					neToken = "*)";
				}
				sb.append(neToken).append("\t");

				// semantic role labeling
				String label = "*";
				sb.append(label).append("\t");

				String corefTk = "-";
				sb.append(corefTk).append("\t");
				lines.add(sb.toString());

			}
			lines.add("");
		}
	}

}

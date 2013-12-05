package model.stanford;

import java.util.ArrayList;

public class StanfordToCoNLL {
	
	public static ArrayList<String> convert(StanfordResult result) {

		ArrayList<String> lines = new ArrayList<String>();
		lines.add("#begin document " + result.documentID);
		for(StanfordSentence ss : result.sentences) {
			
			String parseTree = ss.parseTree.toString();
			int from = 0;
			int to = 0;
			for(StanfordToken token : ss.tokens) {
				StringBuilder sb = new StringBuilder();
				// documentID
				sb.append(result.documentID).append("\t");
				// partID
				sb.append("0").append("\t");
				// word number
				sb.append(token.id).append("\t");
				// word itself
				sb.append(token.word).append("\t");
				// Part-of-Speech
				sb.append(token.POS).append("\t");
				// Parse bit
				String key = " (" + token.POS + " " + token.word + ")";
				to = parseTree.indexOf(key, from);
				sb.append(parseTree.substring(from, to).trim());
				to = from + key.length();
				// Predicate lemma
				sb.append("-").append("\t");
				// Predicate Frameset ID
				sb.append("-").append("\t");
				// Word sense
				sb.append("-").append("\t");
				// Speaker/Author
				sb.append("-").append("\t");
				// Named Entities
				String ne = token.ner;
				String neToken = "*";
				if(!ne.equalsIgnoreCase("other")) {
					
				}
				
				lines.add(sb.toString());
			}
			
			
			
			lines.add("\n");
		}
		lines.add("#end document");
		
		return lines;
	}
	
	public static void main(String args[]) {
		ArrayList<String> lines = convert(StanfordXMLReader.read("/users/yzcchen/chen3/NAACL/corpus/EECB1.0/data/1/1.stanford")); 
		for(String line : lines) {
			System.out.println(line);
		}
	}
}

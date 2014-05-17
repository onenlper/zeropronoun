package translate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import model.stanford.StanfordResult;
import model.stanford.StanfordSentence;
import model.stanford.StanfordToken;
import model.stanford.StanfordXMLReader;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import util.Common;

public class GetTokenization {

	public static void main(String args[]) {
//		ArrayList<String> lines = new ArrayList<String>();
//		lines.addAll(parse("std/3/3.eng.aa.xml"));
//		lines.addAll(parse("std/3/3.eng.ab.xml"));
//		lines.addAll(parse("std/3/3.eng.ac.xml"));
////		lines.addAll(parse("std/3/3.eng.ad.xml"));
//		
//		Common.outputLines(lines, "setting3.eng.tok");
	}

	private static ArrayList<String> parse(String file) {
		ArrayList<String> lines = new ArrayList<String>();
		try {
			StanfordResult stanfordResult = new StanfordResult();
			InputStream inputStream = new FileInputStream(new File(file));
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			StanfordXMLReader reader = new StanfordXMLReader(stanfordResult);
			sp.parse(new InputSource(inputStream), reader);
			System.out.println(stanfordResult.sentences.size());
			for(StanfordSentence ss : stanfordResult.sentences) {
				StringBuilder sb = new StringBuilder();
				for(StanfordToken st : ss.getTokens()) {
					sb.append(st.word).append(" ");
				}
				lines.add(sb.toString().trim());
			}
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return lines;
	}
	
}

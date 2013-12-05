package gigaword;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import em.EMUtil;

import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import util.Common;

public class ParseToCoNLL {

	public static void main(String args[]) throws Exception {
		
		String folder = "/users/yzcchen/chen3/zeroEM/parser/";
		int i = 0;
		CoNLLDocument doc = null;
		for (File subFolder : (new File(folder)).listFiles()) {
			if (subFolder.isDirectory() && !subFolder.getName().contains("cna")) {
				for (File file : subFolder.listFiles()) {
					String filename = file.getAbsolutePath();
					System.out.println(filename + " " + (i++));
					buildCoNLL(filename);
				}
			}
		}
		
	}

	private static void buildCoNLL(String filename) throws Exception{
		BufferedReader br = new BufferedReader(new FileReader(filename));
		
		CoNLLPart part = new CoNLLPart();
		int wID = 0;
		String line = "";
		while((line=br.readLine())!=null) {
			if(line.trim().isEmpty()) {
//				part.setDocument(doc);
//				doc.getParts().add(part);
				part = new CoNLLPart();
				wID=0;
				continue;
			}
			MyTree tree = Common.constructTree(line);
			CoNLLSentence s = new CoNLLSentence();
			part.addSentence(s);
			s.syntaxTree = tree;
			ArrayList<MyTreeNode> leaves = tree.leaves;
			System.out.println(leaves.size());
			for(int i=0;i<leaves.size();i++) {
				MyTreeNode leaf = leaves.get(i);
				CoNLLWord word = new CoNLLWord();
				word.orig = leaf.value;
				word.word = leaf.value;
				
				System.out.println(word.word);
				if(EMUtil.pronouns.contains(word.word)) {
					Common.bangErrorPOS("");
				}
				
				word.sentence = s;
				word.indexInSentence = i;
				word.index = wID++;
				word.posTag = leaf.parent.value;
				s.addWord(word);
			}
		}
		br.close();
	}
	
}

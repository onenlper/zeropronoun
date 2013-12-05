package gigaword;

import java.io.File;
import java.util.ArrayList;

import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import util.Common;

public class ConvertZYParser {

	
	/*
	 * (IP (IP (PP (P#t (P#z (P#b 为) (P#i 了))) (IP (VP (VV#t (VV#y (VV#b 协) (VV#i 助))) (NP (CP (IP (VP (VV#t (VV#x (VV#b 缺) (VV#i 乏))) (NP (NN#t (NN#x (NN#b 
资) (NN#i 金)))))) (DEC#t (DEC#b 的))) (NP (NN#t (NN#y (NN#b 网) (NN#i 路))) (NN#t (NN#y (NN#b 公) (NN#i 司))))) (IP (VV#t (VV#z (VV#b 脱) (VV#i 困))))))) (P
U#t (PU#b ,)) (NP (NR#t (NR#y (NR#b 中) (NR#i 国)))) (VP (PP (P#t (P#b 于)) (NP (NT#t (NT#z (NT#b 周) (NT#i 五))))) (VP (VV#t (VV#z (VV#b 举) (VV#i 行))) (AS
#t (AS#b 了)) (NP (DNP (QP (ADJP (JJ#t (JJ#y (JJ#b 历) (JJ#i 来)))) (QP (OD#t (OD#b 首)) (CLP (M#t (M#b 见))))) (DEG#t (DEG#b 的))) (NP (NN#t (NN#y (NN#z (NN
#b 拍) (NN#i 卖)) (NN#i 会)))))))) (PU#t (PU#b ;)) (IP (PP (PP (P#t (P#b 从)) (NP (NN#t (NN#y (NN#b 网) (NN#i 路))) (NN#t (NN#y (NN#b 内) (NN#i 容))) (PU#t (
PU#b 、)) (NN#t (NN#y (NN#b 网) (NN#i 路))) (NN#t (NN#x (NN#b 技) (NN#i 术))))) (PP (P#t (P#b 到)) (NP (DP (DT#t (DT#b 整)) (CLP (M#t (M#b 个)))) (NP (NR#t (
NR#x (NR#b 达) (NR#i 康))) (NN#t (NN#y (NN#b 公) (NN#i 司))))))) (VP (ADVP (AD#t (AD#b 都))) (VP (PP (P#t (P#b 在)) (NP (DNP (NP (NN#t (NN#z (NN#b 拍) (NN#i 
卖)))) (DEG#t (DEG#b 之))) (NP (NN#t (NN#b 列)))))))) (PU#t (PU#b 。)))
	 */
	public static void main(String args[]) {
		String folder = "/users/yzcchen/chen2/zeroEM/zyparser";
		int f = 0;
		for (File subFolder : (new File(folder)).listFiles()) {
//			if(!subFolder.getName().endsWith("cna_simple")) {
//				continue;
//			}
			if (subFolder.isDirectory()) {
				for (File file : subFolder.listFiles()) {
					if(file.getAbsolutePath().endsWith(".text")) {
						System.out.println(file.getAbsolutePath() + " " + (f++));
						convert(file.getAbsolutePath());
					}
				}
			}
		}
	}

	private static void convert(String zyParseFn) {
		ArrayList<String> lines = Common
				.getLines(zyParseFn);
		ArrayList<String> output = new ArrayList<String>();
		for (String line : lines) {
			convert(output, line);
		}
		Common.outputLines(output, zyParseFn.replace("chen2", "chen3").replace("zeroEM", "Winograd"));
	}

	public static void convert(ArrayList<String> output, String line) {
		MyTree tree = Common.constructTree(line);
		MyTreeNode root = tree.root;
		ArrayList<MyTreeNode> fronties = new ArrayList<MyTreeNode>();
		
		fronties.add(root);
		while (fronties.size() > 0) {
			MyTreeNode tn = fronties.remove(0);
			
			if(tn.value.endsWith("#t")) {
				ArrayList<MyTreeNode> leaves = tn.getLeaves();
				StringBuilder sb = new StringBuilder();
				for(MyTreeNode node : leaves) {
					if(node.value.isEmpty()) {
						Common.bangErrorPOS("");
					}
					sb.append(node.value.replace("(", "-LBR-").replace(")", "-RBR-"));
				}
				tn.value = tn.value.substring(0, tn.value.length()-2);
				MyTreeNode leaf = new MyTreeNode();
				leaf.value = sb.toString();
				tn.children.clear();
				tn.children.add(leaf);
			}
			fronties.addAll(tn.children);
		}
		root.setAllMark(true);
		String text = root.getPlainText(true).trim();
//		if(!text.equals("(NP (NR xxxx))")) {
		output.add(text);
//		} else {
//			output.add("");
//		}
	}
}

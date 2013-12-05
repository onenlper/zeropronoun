package gigaword;

import java.io.File;
import java.util.ArrayList;

import util.Common;

public class CheckParse {

	public static void main(String args[]) {
		String folder = "/users/yzcchen/chen3/zeroEM/xxx-separate/";
		int f = 0;
		ArrayList<String> toDo = new ArrayList<String>();
		for (File subFolder : (new File(folder)).listFiles()) {
			if (subFolder.isDirectory() && !subFolder.getName().contains("cna")) {
				for (File file : subFolder.listFiles()) {
					boolean good = check(file.getAbsolutePath());
					System.out.println(file.getAbsolutePath() + " " + (f++)
							+ " " + good);
					if(!good) {
						toDo.add(file.getAbsolutePath());
					}
				}
			}
		}
		Common.outputLines(toDo, "toDoList");
		System.out.println(toDo.size());
	}

	private static boolean check(String zyParseFn) {
		ArrayList<String> rawText = Common.getLines(zyParseFn);

		if (!(new File(zyParseFn.replace("xxx-separate", "parser")).exists())) {
			return false;
		}

		ArrayList<String> parseLines = Common.getLines(zyParseFn.replace(
				"xxx-separate", "parser"));

		if (parseLines.size() < rawText.size()) {
			return false;
		}

//		int parseIndex = 0;
//		for (String text : rawText) {
//			if (text.equals("xxxx")) {
//				parseIndex++;
//				continue;
//			}
//
//			text = text.replaceAll("\\s+", "").replace(")", "");
//			StringBuilder sb = new StringBuilder();
//
//			while (!text.equals(sb.toString())) {
//				if (sb.length() > text.length()) {
//					System.out.println(sb.toString());
//					System.out.println(text);
//					return false;
//				}
//				MyTree tree = Common
//						.constructTree(parseLines.get(parseIndex++));
//				if (tree == null || tree.root == null) {
//					System.out.println(sb.toString());
//					System.out.println(text);
//					return false;
//				}
//				String subText = tree.root.toString().replaceAll("\\s+", "").replace(")", "");
//				sb.append(subText);
//			}
//
//		}

		return true;
	}
}

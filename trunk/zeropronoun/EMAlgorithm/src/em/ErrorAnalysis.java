package em;

import java.util.ArrayList;

import util.Common;

public class ErrorAnalysis {

	public static void main(String args[]) {
		
		ArrayList<String> lines = Common.getLines("errorAnalysis2");
		int error = 0;
		int select = 0;
		boolean sel = true;
		for(String line : lines) {
			if(line.startsWith("Error???")) {
				error ++;
				if(error%9==1) {
					select ++;
					sel = true;
				} else {
					sel = false;
				}
			}
			if(sel) {
				System.out.println(line);
			}
		}
		System.out.println(error);
		System.out.println(select);
	}
	
}

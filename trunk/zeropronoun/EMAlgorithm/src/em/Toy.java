package em;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import util.Common;

public class Toy {
	public static void main(String args[]) {
		
		String g = "0 @ @ 1 1004_0 1 1039_0 1 1044_0 1 1072_0 1 1092_0 1 1112_0 1 1217_0 1 1242_0 1 1759_0 1 3540_0 1 3574_0 1 3760_0 1 8293_0 1 11217_0 1 14354_0 1 14355_0 1 14356_0 1 18350_0 1 18355_0 1 18649_0 1 19056_0 1 20752_0 1 26328_0 1 30062_0 1 66585_0 1 66604_0 1 # @ 0 1004_1 1 1039_1 1 1044_1 1 1072_1 1 1092_1 1 1112_1 1 1217_1 1 1242_1 1 1759_1 1 3540_1 1 3574_1 1 3760_1 1 8293_1 1 11217_1 1 14354_1 1 14355_1 1 14356_1 1 18350_1 1 18355_1 1 18649_1 1 19056_1 1 20752_1 1 26328_1 1 30062_1 1 66585_1 1 66604_1 1 # @ 0 1004_2 1 1039_2 1 1044_2 1 1072_2 1 1092_2 1 1112_2 1 1217_2 1 1242_2 1 1759_2 1 3540_2 1 3574_2 1 3760_2 1 8293_2 1 11217_2 1 14354_2 1 14355_2 1 14356_2 1 18350_2 1 18355_2 1 18649_2 1 19056_2 1 20752_2 1 26328_2 1 30062_2 1 66585_2 1 66604_2 1 # ";
		
		System.out.println(g.split("@").length);
		if(true) {
			return;
		}
		
		
		String lineStr = "";
		String cmd = "/users/yzcchen/tool/YASMET/./a.out /users/yzcchen/tool/YASMET/animacy.model";

		Runtime run = Runtime.getRuntime();
		try {
			Process p = run.exec(cmd);

			String str = "3\n";
			str += "0 @ @ 1 1004_0 1 1007_0 1 1008_0 1 1012_0 1 1016_0 1 1020_0 1 1025_0 1 1026_0 1 1027_0 1 1033_0 1 1034_0 1 1112_0 1 1139_0 1 1206_0 1 1226_0 1 1406_0 1 1490_0 1 1512_0 1 1922_0 1 1954_0 1 1966_0 1 2288_0 1 2290_0 1 2298_0 1 2559_0 1 3245_0 1 3253_0 1 4170_0 1 4398_0 1 11643_0 1 11645_0 1 15635_0 1 16617_0 1 17079_0 1 17082_0 1 17730_0 1 17737_0 1 18054_0 1 18059_0 1 27679_0 1 27680_0 1 28335_0 1 38413_0 1 48372_0 1 48374_0 1 48637_0 1 53746_0 1 53749_0 1 54314_0 1 56996_0 1 57002_0 1 62583_0 1 77151_0 1 77155_0 1 81737_0 1 86019_0 1 # @ 0 1004_1 1 1007_1 1 1008_1 1 1012_1 1 1016_1 1 1020_1 1 1025_1 1 1026_1 1 1027_1 1 1033_1 1 1034_1 1 1112_1 1 1139_1 1 1206_1 1 1226_1 1 1406_1 1 1490_1 1 1512_1 1 1922_1 1 1954_1 1 1966_1 1 2288_1 1 2290_1 1 2298_1 1 2559_1 1 3245_1 1 3253_1 1 4170_1 1 4398_1 1 11643_1 1 11645_1 1 15635_1 1 16617_1 1 17079_1 1 17082_1 1 17730_1 1 17737_1 1 18054_1 1 18059_1 1 27679_1 1 27680_1 1 28335_1 1 38413_1 1 48372_1 1 48374_1 1 48637_1 1 53746_1 1 53749_1 1 54314_1 1 56996_1 1 57002_1 1 62583_1 1 77151_1 1 77155_1 1 81737_1 1 86019_1 1 # @ 0 1004_2 1 1007_2 1 1008_2 1 1012_2 1 1016_2 1 1020_2 1 1025_2 1 1026_2 1 1027_2 1 1033_2 1 1034_2 1 1112_2 1 1139_2 1 1206_2 1 1226_2 1 1406_2 1 1490_2 1 1512_2 1 1922_2 1 1954_2 1 1966_2 1 2288_2 1 2290_2 1 2298_2 1 2559_2 1 3245_2 1 3253_2 1 4170_2 1 4398_2 1 11643_2 1 11645_2 1 15635_2 1 16617_2 1 17079_2 1 17082_2 1 17730_2 1 17737_2 1 18054_2 1 18059_2 1 27679_2 1 27680_2 1 28335_2 1 38413_2 1 48372_2 1 48374_2 1 48637_2 1 53746_2 1 53749_2 1 54314_2 1 56996_2 1 57002_2 1 62583_2 1 77151_2 1 77155_2 1 81737_2 1 86019_2 1 # ";
			BufferedOutputStream out = new BufferedOutputStream(
					p.getOutputStream());
			out.write(str.getBytes());
			out.flush();
			out.close();
			BufferedInputStream in = new BufferedInputStream(p.getInputStream());
			BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
			lineStr = inBr.readLine();
			if (p.waitFor() != 0) {
				System.out.println("There");
				if (p.exitValue() == 1) {
					System.err.println("ERROR YASMET");
					Common.bangErrorPOS("");
				}
			}
			System.out.println(lineStr);
			inBr.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String tks[] = lineStr.split("\\s+");
		double ret[] = new double[tks.length - 1];
		for (int i = 1; i < tks.length; i++) {
			ret[i - 1] = Double.parseDouble(tks[i]);
		}
	}
}

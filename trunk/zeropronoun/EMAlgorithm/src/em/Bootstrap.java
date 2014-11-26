package em;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import util.Common;

public class Bootstrap {

	public static class Group {
		int highRank;
		ArrayList<Atom> atoms;
		int validAtom = 0;

		public Group(int highRank, ArrayList<Atom> atoms) {
			this.highRank = highRank;
			this.atoms = atoms;
		}

		boolean validGroup = true;

		// public static Group readGroup(String s) {
		// if(s.startsWith(" @")) {
		// // bad
		// }
		//
		// String tks[] = s.split("@");
		//
		// }
	}

	public static class Atom {
		double label;
		String fea;
		boolean valid = true;

		public Atom(double label, String fea) {
			this.label = label;
			this.fea = fea;
			if (this.fea.contains("NOCLASS")) {
				this.valid = false;
			}
		}
	}

	public static void main(String args[]) {

		ArrayList<String> yasmetOvert_ZeroFea = Common
				.getLines("yasmetOvert_ZeroFea");
		ArrayList<String> yasmetZero_ZeroFea = Common
				.getLines("yasmetZero_ZeroFea");

		ArrayList<String> yasmetOvert_OvertFea = Common
				.getLines("yasmetOvert_OvertFea");
		ArrayList<String> yasmetZero_OvertFea = Common
				.getLines("yasmetZero_OvertFea");

		ArrayList<String> yasmetZeroTest_OvertFea = Common
				.getLines("yasmetZeroTest_OvertFea");

		ArrayList<String> overtFeaTrains = new ArrayList<String>();

		for (int i = 0; i < 10; i++) {
			if (!yasmetOvert_OvertFea.get(0).startsWith(" @")) {
				overtFeaTrains.add(yasmetOvert_OvertFea.remove(0));
			}
			if (!yasmetZero_OvertFea.get(0).startsWith(" @")) {
				overtFeaTrains.add(yasmetZero_OvertFea.remove(0));
				yasmetZeroTest_OvertFea.remove(0);
			}
		}

		ArrayList<String> zeroFeaTrains = new ArrayList<String>();
		for (int i = 0; i < 10; i++) {
			if (!yasmetOvert_ZeroFea.get(0).startsWith(" @")) {
				zeroFeaTrains.add(yasmetOvert_ZeroFea.remove(0));
			}
			if (!yasmetZero_ZeroFea.get(0).startsWith(" @")) {
				zeroFeaTrains.add(yasmetZero_ZeroFea.remove(0));
			}
		}

		int iteration = 0;

		while (true) {

			trainYasmet(overtFeaTrains, "overtM" + iteration, 100);
			trainYasmet(zeroFeaTrains, "zeroM" + iteration, 100);

			ArrayList<Integer> active1 = testYasmet(yasmetZeroTest_OvertFea,
					"overtM" + iteration, 1000);
			for (Integer act : active1) {
				yasmetZeroTest_OvertFea.remove(act);
				overtFeaTrains.add(yasmetZero_OvertFea.remove(act.intValue()));
			}

			ArrayList<Integer> active2 = testYasmet(yasmetOvert_OvertFea,
					"overtM" + iteration, 1000);
			for (Integer act : active2) {
				overtFeaTrains.add(yasmetOvert_OvertFea.remove(act.intValue()));
			}

			ArrayList<Integer> active3 = testYasmet(yasmetZero_ZeroFea, "zeroM"
					+ iteration, 1000);
			for (Integer act : active3) {
				zeroFeaTrains.add(yasmetZero_ZeroFea.remove(act.intValue()));
			}

			ArrayList<Integer> active4 = testYasmet(yasmetOvert_ZeroFea,
					"zeroM" + iteration, 1000);
			for (Integer act : active4) {
				zeroFeaTrains.add(yasmetOvert_ZeroFea.remove(act.intValue()));
			}
			
			iteration++;
		}
	}

	public static ArrayList<Integer> testYasmet(ArrayList<String> lines,
			String model, int size) {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		HashMap<Integer, Double> map = new HashMap<Integer, Double>();
		String lineStr = "";
		System.out.println("Test: " + model);
		String cmd = "/users/yzcchen/tool/YASMET/./a.out /dev/shm/" + model;

		Runtime run = Runtime.getRuntime();
		try {
			Process p = run.exec(cmd);

			BufferedOutputStream out = new BufferedOutputStream(
					p.getOutputStream());
			out.write((Integer.toString(size) + "\n").getBytes());
			for (String line : lines) {
				if (line.trim().startsWith("@")) {
					continue;
				}
				out.write((line + "\n").getBytes());
			}
			out.flush();
			out.close();

			BufferedInputStream in = new BufferedInputStream(p.getInputStream());
			BufferedReader inBr = new BufferedReader(new InputStreamReader(in));

			int maxSelect = 10;
			int qid = 0;
			while ((lineStr = inBr.readLine()) != null) {
				String tks[] = lineStr.split("\\s+");
				double maxDouble = 0;
				for (int i = 0; i < tks.length; i++) {
					maxDouble = Math.max(maxDouble, Double.parseDouble(tks[i]));
				}
				map.put(qid, maxDouble);

				if (map.size() > maxSelect) {
					double max = 0;
					int removeKey = -1;
					for (int key : map.keySet()) {
						if (map.get(key) > max) {
							removeKey = key;
							max = map.get(key);
						}
					}
					map.remove(removeKey);
				}
				qid++;
			}
			if (p.waitFor() != 0) {
				if (p.exitValue() == 1) {
					System.err.println("ERROR YASMET");
					Common.bangErrorPOS("");
				}
			}
			inBr.close();
			in.close();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		ret.addAll(map.keySet());
		Collections.sort(ret);
		Collections.reverse(ret);
		return ret;
	}

	public static void trainYasmet(ArrayList<String> lines, String model,
			int size) {
		ArrayList<String> tmp = new ArrayList<String>(lines);
		tmp.add(0, Integer.toString(size));
		Common.outputLines(tmp, "tmp");
		System.out.println("Train: " + model);
		String cmd = "./trainBoots.sh " + model;
		Runtime run = Runtime.getRuntime();
		try {
			Process p = run.exec(cmd);

			BufferedInputStream in = new BufferedInputStream(p.getInputStream());
			BufferedReader inBr = new BufferedReader(new InputStreamReader(in));
			if (p.waitFor() != 0) {
				if (p.exitValue() == 1) {
					System.err.println("ERROR YASMET");
					// Common.bangErrorPOS("");
				}
			}
			inBr.close();
			in.close();

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}

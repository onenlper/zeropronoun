package gigaword;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

public class CreateMiniStat {

	public static void main(String args[]) throws Exception{
		HashSet<String> miniS = Common.readFile2Set("miniS");
		HashSet<String> miniV = Common.readFile2Set("miniV");
		
		try {
			FileWriter fw = new FileWriter("unigram.mini");
			
			BufferedReader br = new BufferedReader(new FileReader(
					"unigram.giga"));
			String line = "";
			while ((line = br.readLine()) != null) {
				int k = line.lastIndexOf(' ');
				String tks[] = line.split("\\s+");
				if(miniS.contains(tks[0])) {
					fw.write(line + "\n");
				}
			}
			br.close();
			fw.close();
			
			fw = new FileWriter("svo.mini");
			
			br = new BufferedReader(new FileReader("svo.giga"));

			while ((line = br.readLine()) != null) {
				int k = line.lastIndexOf(' ');
				int count = Integer.parseInt(line.substring(k + 1));
				String key = line.substring(0, k).trim();
				String tks[] = key.split("\\s+");
				String s = tks[0];
				String v = tks[1];

				if(miniV.contains(v)) {
					fw.write(line + "\n");
				}

			}
			br.close();
			fw.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

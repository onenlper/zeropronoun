package em;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

public class SVOStat implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public HashMap<String, Integer> unigrams;
	public int unigramAll = 1246070771;

	public HashMap<String, Integer> svCounts;
	public HashMap<String, Integer> svoCounts;

	public HashMap<String, Integer> vCounts;
	public HashMap<String, Integer> voCounts;
	public int svoAll = 66724995;
	
	public void loadMIInfo() {
		try {
			unigrams = new HashMap<String, Integer>();
			BufferedReader br = new BufferedReader(new FileReader(
					"unigram.giga"));
			String line = "";
			while ((line = br.readLine()) != null) {
				int k = line.lastIndexOf(' ');
				int count = Integer.parseInt(line.substring(k + 1));
				String key = line.substring(0, k).trim();
				addMap(unigrams, key, count);
			}
			br.close();

			br = new BufferedReader(new FileReader("svo.giga"));
			svCounts = new HashMap<String, Integer>();
			vCounts = new HashMap<String, Integer>();
			svoCounts = new HashMap<String, Integer>();
			voCounts = new HashMap<String, Integer>();

			while ((line = br.readLine()) != null) {
				int k = line.lastIndexOf(' ');
				int count = Integer.parseInt(line.substring(k + 1));
				String key = line.substring(0, k).trim();
				String tks[] = key.split("\\s+");
				String s = tks[0];
				String v = tks[1];

				addMap(svCounts, s + " " + v, count);
				addMap(vCounts, v, count);

				if (tks.length == 3) {
					String o = tks[2];
					addMap(svoCounts, s + " " + v + " " + o, count);
					addMap(voCounts, v + " " + o, count);
				}

			}
			br.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public void loadMIInfo2() {
		try {
			unigrams = new HashMap<String, Integer>();
			BufferedReader br = new BufferedReader(new FileReader(
					"unigram.giga"));
			String line = "";
			while ((line = br.readLine()) != null) {
				int k = line.lastIndexOf(' ');
				int count = Integer.parseInt(line.substring(k + 1));
				String tks[] = line.split("\\s+");
				String key = EMUtil.getAntAnimacy2(tks[0]).name();
//				System.out.println(key);
				addMap(unigrams, key, count);
			}
			br.close();

			br = new BufferedReader(new FileReader("svo.giga"));
			svCounts = new HashMap<String, Integer>();
			vCounts = new HashMap<String, Integer>();
			svoCounts = new HashMap<String, Integer>();
			voCounts = new HashMap<String, Integer>();

			while ((line = br.readLine()) != null) {
				int k = line.lastIndexOf(' ');
				int count = Integer.parseInt(line.substring(k + 1));
				String key = line.substring(0, k).trim();
				String tks[] = key.split("\\s+");
				String s = tks[0];
				
				s = EMUtil.getAntAnimacy2(tks[0]).name();				
				
				String v = tks[1];

				addMap(svCounts, s + " " + v, count);
				addMap(vCounts, v, count);

				if (tks.length == 3) {
					String o = tks[2];
					addMap(svoCounts, s + " " + v + " " + o, count);
					addMap(voCounts, v + " " + o, count);
				}

			}
			br.close();

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addMap(HashMap<String, Integer> map, String key, int val) {
		Integer count = map.get(key);
		if (count == null) {
			map.put(key, val);
		} else {
			map.put(key, count.intValue() + val);
		}
	}
}

package em;

import java.util.ArrayList;
import java.util.HashMap;

import util.Common;

public class NaiveBaysian {

	int trainSize;

	HashMap<String, Double> priorCount;
	HashMap<String, Double> priorProb;

	HashMap<String, Double> likelihoodCount;
	HashMap<String, Double> likelihoodProb;
	
	public NaiveBaysian() {
		this.trainSize = 0;
		this.priorCount = new HashMap<String, Double>();
		this.priorProb = new HashMap<String, Double>();
		
		this.likelihoodCount = new HashMap<String, Double>();
		this.likelihoodProb = new HashMap<String, Double>();
	}

	public void addMap(HashMap<String, Double> map, String key) {
		Double d = map.get(key);
		if(d==null) {
			map.put(key, 1.0);
		} else {
			map.put(key, d.doubleValue() + 1.0);
		}
	}
	
	public void addInstance(String text) {
		int k = text.indexOf(" ");
		String label = text.substring(0, k);
		this.addMap(priorCount, label);
		
		ArrayList<String> feas = this.getFeas(text);
		
		for(String fea : feas) {
			this.addMap(this.likelihoodCount, label + "_" + fea);
		}
		this.trainSize++;
	}

	
	
	public void train() {
		// build prior
		for(String key : this.priorCount.keySet()) {
			this.priorProb.put(key, this.priorCount.get(key)/this.trainSize);
		}
		
		//build likelihood
//		for(String key : )
		
		
	}

	public ArrayList<String> getFeas(String str) {
		ArrayList<String> feas = new ArrayList<String>();
		String tks[] = str.split("\\s+");
		for (int i = 1; i < tks.length; i++) {
			int k = tks[i].indexOf(":");
			if (!tks[i].substring(k + 1).equals("1")) {
				Common.bangErrorPOS("Only accept binary feature value...");
			}
			feas.add(tks[i].substring(0, k));
		}

		return feas;
	}

	public double[] test(String str) {
		ArrayList<String> instance = this.getFeas(str);
		
		
		return null;
	}

}

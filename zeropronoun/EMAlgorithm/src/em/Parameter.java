package em;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

public class Parameter implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	HashMap<String, HashMap<String, Double>> values;

	HashMap<String, HashMap<String, Double>> fracCounts;
	HashMap<String, Double> keyCounts;

	double defaultV;

	HashSet<String> subKeys;

	boolean init;

	double theta = 1;

	public Parameter() {
		values = new HashMap<String, HashMap<String, Double>>();
		fracCounts = new HashMap<String, HashMap<String, Double>>();
		keyCounts = new HashMap<String, Double>();
		subKeys = new HashSet<String>();
		this.init = true;
	}

	public Parameter(double defaultV) {
		values = new HashMap<String, HashMap<String, Double>>();
		fracCounts = new HashMap<String, HashMap<String, Double>>();
		keyCounts = new HashMap<String, Double>();
		subKeys = new HashSet<String>();
		this.defaultV = defaultV;
		this.init = true;
	}

	public void setVals() {
		values.clear();
		for (String key : keyCounts.keySet()) {
			HashMap<String, Double> subMap = fracCounts.get(key);
			Double count = keyCounts.get(key);

			HashMap<String, Double> subValMap = new HashMap<String, Double>();
			values.put(key, subValMap);

			for (String subKey : subMap.keySet()) {
				subValMap.put(subKey, subMap.get(subKey) / count);
			}
		}
		this.init = false;
	}

	public void resetCounts() {
		this.fracCounts.clear();
		this.keyCounts.clear();
	}

	public void addFracCount(String key, String subKey, double val) {
		subKeys.add(subKey);
		HashMap<String, Double> subMap = fracCounts.get(key);
		if (subMap == null) {
			subMap = new HashMap<String, Double>();
			fracCounts.put(key, subMap);
		}
		Double d = subMap.get(subKey);
		if (d == null) {
			subMap.put(subKey, val);
		} else {
			subMap.put(subKey, val + d.doubleValue());
		}
		// add count
		Double c = keyCounts.get(key);
		if (c == null) {
			keyCounts.put(key, val);
		} else {
			keyCounts.put(key, val + c.doubleValue());
		}
	}

	public double getVal(String key, String subKey) {
		if (this.init) {
			return this.defaultV;
		} else {
			double numerator = theta;
			double denominator = theta * subKeys.size();

			if (keyCounts.containsKey(key)) {
				denominator += keyCounts.get(key);
			}
			HashMap<String, Double> subMap = fracCounts.get(key);
			if (subMap != null && subMap.containsKey(subKey)) {
				numerator += subMap.get(subKey);
			}
			double val = numerator / denominator;
			// System.out.println(val + " " + numerator + " " + denominator +
			// " " + subKeys.size() + " " + theta);
			return val;
		}
	}

	public void printParameter(String fn) {
		ArrayList<String> output = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		sb.append("Antecedent:");
		for (String key : this.subKeys) {
			sb.append("\t").append(key);
		}
		output.add(sb.toString());

		for (String key : this.values.keySet()) {
			sb = new StringBuilder();
			sb.append(key);

			for (String subKey : this.subKeys) {
				if (this.values.get(key).containsKey(subKey)) {
					sb.append("\t").append(
							String.format("%.3f",
									this.values.get(key).get(subKey)));
				} else {
					sb.append("\t").append(0.000);
				}
			}
			output.add(sb.toString());
		}
		Common.outputLines(output, fn);
	}

	public String round(double p, int digits) {
		String str = Double.toString(p);
		if (str.length() < digits) {
			while (str.length() < digits) {
				str += '0';
			}
		} else {
			str = str.substring(0, digits);
		}
		return str;
	}
}

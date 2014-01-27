package em;

/* demo.java */

import java.util.HashMap;

import util.Common;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

public class ILP {

	// static double a = 0.001;
	// static double b = 0.001;
	// static double c = 0.001;
	// static double d = 0.001;

	// static double a = 0.005;
	// static double b = 0.005;
	// static double c = 0.005;
	// static double d = 0.005;

	// static double a = 0.008;
	// static double b = 0.008;
	// static double c = 0.008;
	// static double d = 0.008;

	// public static double a_num = 0.008;
	// public static double b_gen = 0.008;
	// public static double c_per = 0.06;
	// public static double d_ani = 0.008;

	// 0.008 0.01 0.04 0.008 = 47.06
	// 0.008 0.01 0.06 0.008 = 47.30 806
	// 0.006 0.01 0.06 0.008 = 47.00 801
	// 0.009 0.01 0.06 0.008 = 47.30 806
	// 0.009 0.01 0.07 0.008 = 46.77 797
	// 0.009 0.01 0.05 0.008 = 47.06 802
	// 0.008 0.01 0.06 0.01 = 47.47

	// 0.008 0.01 0.06 0.012 R:0.47285464098073554 P: 0.4778761061946903 F:
	// 0.47535211267605637

	
	// norm 0.02 0.008 0.08 0.06
	public static double a_num = 0.008;
	public static double b_gen = 0.01;
	public static double c_per = 0.06;
	public static double d_ani = 0.012;

	int numberOfAnts = 0;

	double proAnte[];
	double proNum[];
	double proGen[];
	double proPer[];
	double proAni[];

	public ILP(int numberOfAnts, double[] proAnte, double[] proNum,
			double[] proGen, double[] proPer, double[] proAni) {
		this.proAnte = proAnte;
		this.proNum = proNum;
		this.proGen = proGen;
		this.proPer = proPer;
		this.proAni = proAni;
		this.numberOfAnts = numberOfAnts;
	}

	public int execute() throws LpSolveException {
		LpSolve lp;
		int Ncol, j, ret = 0;

		/*
		 * We will build the model row by row So we start with creating a model
		 * with 0 rows and 2 columns
		 */
		Ncol = numberOfAnts * EMUtil.pronounList.size()
				+ EMUtil.Person.values().length
				+ (EMUtil.Animacy.values().length - 1)
				+ (EMUtil.Gender.values().length-1) + EMUtil.Number.values().length; /*
																				 * number
																				 * of
																				 * variables
																				 * in
																				 * the
																				 * model
																				 */

		/* create space large enough for one row */
		int[] colno = new int[Ncol];
		double[] row = new double[Ncol];

		lp = LpSolve.makeLp(0, Ncol);
		if (lp.getLp() == 0)
			ret = 1; /* couldn't construct a new model... */

		// set binary
		for (int i = 1; i < Ncol; i++) {
			lp.setBinary(i, true);
		}
		HashMap<String, Integer> nameMap = new HashMap<String, Integer>();
		HashMap<String, Double> probMap = new HashMap<String, Double>();
		if (ret == 0) {
			/*
			 * let us name our variables. Not required, but can be usefull for
			 * debugging
			 */
			int vNo = 1;
			for (int i = 0; i < EMUtil.pronounList.size(); i++) {
				for (int k = 0; k < numberOfAnts; k++) {
					String name = "X(" + k + "," + EMUtil.pronounList.get(i)
							+ ")";
					nameMap.put(name, vNo);
					probMap.put(name, this.proAnte[vNo - 1]);
					lp.setColName(vNo++, name);
				}
			}
			for (int i = 0; i < EMUtil.Number.values().length; i++) {
				String name = "Y_num(" + EMUtil.Number.values()[i].name() + ")";
				nameMap.put(name, vNo);
				probMap.put(name, this.proNum[i]);
				lp.setColName(vNo++, name);
			}
			for (int i = 0; i < EMUtil.Gender.values().length-1; i++) {
				String name = "Y_gen(" + EMUtil.Gender.values()[i].name() + ")";
				nameMap.put(name, vNo);
				probMap.put(name, this.proGen[i]);
				lp.setColName(vNo++, name);
			}
			for (int i = 0; i < EMUtil.Person.values().length; i++) {
				String name = "Y_per(" + EMUtil.Person.values()[i].name() + ")";
				nameMap.put(name, vNo);
				probMap.put(name, this.proPer[i]);
				lp.setColName(vNo++, name);
			}
			for (int i = 0; i < EMUtil.Animacy.values().length - 1; i++) {
				String name = "Y_ani(" + EMUtil.Animacy.values()[i].name()
						+ ")";
				nameMap.put(name, vNo);
				probMap.put(name, this.proAni[i]);
				lp.setColName(vNo++, name);
			}

			lp.setAddRowmode(true); /*
									 * makes building the model faster if it is
									 * done rows by row
									 */

			// constraint 1: sum over all x to 1
			int m = 0;
			if (ret == 0) {
				for (int i = 0; i < EMUtil.pronounList.size(); i++) {
					for (int k = 0; k < numberOfAnts; k++) {
						String name = "X(" + k + ","
								+ EMUtil.pronounList.get(i) + ")";
						int x_a_pro = nameMap.get(name);
						colno[m] = x_a_pro;
						row[m++] = 1;
					}
				}
			}
			/* add the row to lp_solve */
			lp.addConstraintex(m, row, colno, LpSolve.EQ, 1);

			// constraint 2: sum over all y_num to 1
			if (ret == 0) {
				m = 0;
				for (int i = 0; i < EMUtil.Number.values().length; i++) {
					String name = "Y_num(" + EMUtil.Number.values()[i].name()
							+ ")";
					int y_num = nameMap.get(name);
					colno[m] = y_num;
					row[m++] = 1;
				}
			}
			/* add the row to lp_solve */
			lp.addConstraintex(m, row, colno, LpSolve.EQ, 1);

			// constraint 3: sum over all y_gen to 1
			if (ret == 0) {
				m = 0;
				for (int i = 0; i < EMUtil.Gender.values().length-1; i++) {
					String name = "Y_gen(" + EMUtil.Gender.values()[i].name()
							+ ")";
					int y_gen = nameMap.get(name);
					colno[m] = y_gen;
					row[m++] = 1;
				}
			}
			/* add the row to lp_solve */
			lp.addConstraintex(m, row, colno, LpSolve.EQ, 1);

			// constraint 4: sum over all y_ani to 1
			if (ret == 0) {
				m = 0;
				for (int i = 0; i < EMUtil.Animacy.values().length - 1; i++) {
					String name = "Y_ani(" + EMUtil.Animacy.values()[i].name()
							+ ")";
					int y_ani = nameMap.get(name);
					colno[m] = y_ani;
					row[m++] = 1;
				}
			}
			/* add the row to lp_solve */
			lp.addConstraintex(m, row, colno, LpSolve.EQ, 1);

			// constraint 5: sum over all y_per to 1
			if (ret == 0) {
				m = 0;
				for (int i = 0; i < EMUtil.Person.values().length; i++) {
					String name = "Y_per(" + EMUtil.Person.values()[i].name()
							+ ")";
					int y_per = nameMap.get(name);
					colno[m] = y_per;
					row[m++] = 1;
				}
			}
			/* add the row to lp_solve */
			lp.addConstraintex(m, row, colno, LpSolve.EQ, 1);

			// constraint 6: pronoun number consistency
			for (int n = 0; n < EMUtil.Number.values().length; n++) {
				EMUtil.Number num = EMUtil.Number.values()[n];

				m = 0;
				for (int i = 0; i < EMUtil.pronounList.size(); i++) {
					for (int k = 0; k < this.numberOfAnts; k++) {
						String pronoun = EMUtil.pronounList.get(i);
						EMUtil.Number num2 = EMUtil.getNumber(pronoun);
						if (num == num2) {
							String name = "X(" + k + ","
									+ EMUtil.pronounList.get(i) + ")";
							colno[m] = nameMap.get(name);
							row[m++] = 1;
						}
					}
				}
				String name = "Y_num(" + num.name() + ")";
				colno[m] = nameMap.get(name);
				row[m++] = -1;

				/* add the row to lp_solve */
				lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
			}

			// constraint 7: pronoun gender consistency
			for (int n = 0; n < EMUtil.Gender.values().length-1; n++) {
				EMUtil.Gender gen = EMUtil.Gender.values()[n];
				m = 0;
				for (int i = 0; i < EMUtil.pronounList.size(); i++) {
					for (int k = 0; k < this.numberOfAnts; k++) {
						String pronoun = EMUtil.pronounList.get(i);
						EMUtil.Gender gen2 = EMUtil.getGender(pronoun);
						if (gen == gen2) {
							String name = "X(" + k + ","
									+ EMUtil.pronounList.get(i) + ")";
							colno[m] = nameMap.get(name);
							row[m++] = 1;
						}
					}
				}
				String name = "Y_gen(" + gen.name() + ")";
				colno[m] = nameMap.get(name);
				row[m++] = -1;

				/* add the row to lp_solve */
				lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
			}

			// constraint 8: pronoun person consistency
			for (int n = 0; n < EMUtil.Person.values().length; n++) {
				EMUtil.Person per = EMUtil.Person.values()[n];
				m = 0;
				for (int i = 0; i < EMUtil.pronounList.size(); i++) {
					for (int k = 0; k < this.numberOfAnts; k++) {
						String pronoun = EMUtil.pronounList.get(i);
						EMUtil.Person per2 = EMUtil.getPerson(pronoun);
						if (per == per2) {
							String name = "X(" + k + ","
									+ EMUtil.pronounList.get(i) + ")";
							colno[m] = nameMap.get(name);
							row[m++] = 1;
						}
					}
				}
				String name = "Y_per(" + per.name() + ")";
				colno[m] = nameMap.get(name);
				row[m++] = -1;

				/* add the row to lp_solve */
				lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
			}

			// constraint 9: pronoun animacy consistency
			for (int n = 0; n < EMUtil.Animacy.values().length - 1; n++) {
				EMUtil.Animacy ani = EMUtil.Animacy.values()[n];
				m = 0;
				for (int i = 0; i < EMUtil.pronounList.size(); i++) {
					for (int k = 0; k < this.numberOfAnts; k++) {
						String pronoun = EMUtil.pronounList.get(i);
						EMUtil.Animacy ani2 = EMUtil.getAnimacy(pronoun);
						if (ani == ani2) {
							String name = "X(" + k + ","
									+ EMUtil.pronounList.get(i) + ")";
							colno[m] = nameMap.get(name);
							row[m++] = 1;
						}
					}
				}
				String name = "Y_ani(" + ani.name() + ")";
				colno[m] = nameMap.get(name);
				row[m++] = -1;

				/* add the row to lp_solve */
				lp.addConstraintex(m, row, colno, LpSolve.EQ, 0);
			}

			// constraint 10: pronoun gender/animacy consistency
			// if gender=male/female, then animacy= animate
			// male + female - animate <= 0
			m = 0;
			colno[m] = nameMap.get("Y_gen(" + EMUtil.Gender.male.name() + ")");
			row[m++] = 1;
			colno[m] = nameMap.get("Y_gen(" + EMUtil.Gender.female.name() + ")");
			row[m++] = 1;
			colno[m] = nameMap.get("Y_ani(" + EMUtil.Animacy.animate.name() + ")");
			row[m++] = -1;
			lp.addConstraintex(m, row, colno, LpSolve.LE, 0);
		} else {
			Common.bangErrorPOS("!!!");
		}

		if (ret == 0) {
			lp.setAddRowmode(false); /*
									 * rowmode should be turned off again when
									 * done building the model
									 */

			/* set the objective function */
			int m = 0;
			for (int i = 0; i < EMUtil.pronounList.size(); i++) {
				for (int k = 0; k < this.numberOfAnts; k++) {
					String name = "X(" + k + "," + EMUtil.pronounList.get(i)
							+ ")";
					int x_a_pro = nameMap.get(name);
					colno[m] = x_a_pro;
					row[m++] = probMap.get(name);
				}
			}

			for (int i = 0; i < EMUtil.Number.values().length; i++) {
				String name = "Y_num(" + EMUtil.Number.values()[i].name() + ")";
				int y_num = nameMap.get(name);
				colno[m] = y_num;
				row[m++] = probMap.get(name) * a_num;
			}

			for (int i = 0; i < EMUtil.Gender.values().length-1; i++) {
				String name = "Y_gen(" + EMUtil.Gender.values()[i].name() + ")";
				int y_gen = nameMap.get(name);
				colno[m] = y_gen;
				row[m++] = probMap.get(name) * b_gen;
			}

			for (int i = 0; i < EMUtil.Person.values().length; i++) {
				String name = "Y_per(" + EMUtil.Person.values()[i].name() + ")";
				int y_per = nameMap.get(name);
				colno[m] = y_per;
				row[m++] = probMap.get(name) * c_per;
			}

			for (int i = 0; i < EMUtil.Animacy.values().length - 1; i++) {
				String name = "Y_ani(" + EMUtil.Animacy.values()[i].name()
						+ ")";
				int y_ani = nameMap.get(name);
				colno[m] = y_ani;
				row[m++] = probMap.get(name) * d_ani;
			}

			/* set the objective in lp_solve */
			lp.setObjFnex(m, row, colno);
		} else {
			Common.bangErrorPOS("!!!");
		}

		if (ret == 0) {
			/* set the object direction to maximize */
			lp.setMaxim();

			/*
			 * just out of curioucity, now generate the model in lp format in
			 * file model.lp
			 */
			lp.writeLp("model.lp");

			/* I only want to see importand messages on screen while solving */
			lp.setVerbose(LpSolve.IMPORTANT);

			/* Now let lp_solve calculate a solution */
			ret = lp.solve();
			if (ret == LpSolve.OPTIMAL)
				ret = 0;
			else
				ret = 5;
		} else {
			Common.bangErrorPOS("!!!");
		}

		if (ret == 0) {
			/* a solution is calculated, now lets get some results */

			/* objective value */
			System.out.println("Objective value: " + lp.getObjective());

			/* variable values */
			lp.getVariables(row);
			for (j = 0; j < Ncol; j++) {
				// System.out.println(lp.getColName(j + 1) + ": " + row[j]);
			}

			System.out.println("---");
			EMUtil.Number num = null;
			EMUtil.Gender gen = null;
			EMUtil.Person per = null;
			EMUtil.Animacy ani = null;

			for (int i = 0; i < EMUtil.Number.values().length; i++) {
				String name = "Y_num(" + EMUtil.Number.values()[i].name() + ")";
				double val = row[nameMap.get(name) - 1];
				if (val != 0) {
					num = EMUtil.Number.values()[i];
					break;
				}
			}

			for (int i = 0; i < EMUtil.Gender.values().length-1; i++) {
				String name = "Y_gen(" + EMUtil.Gender.values()[i].name() + ")";
				double val = row[nameMap.get(name) - 1];
				if (val != 0) {
					gen = EMUtil.Gender.values()[i];
					break;
				}
			}

			for (int i = 0; i < EMUtil.Person.values().length; i++) {
				String name = "Y_per(" + EMUtil.Person.values()[i].name() + ")";
				double val = row[nameMap.get(name) - 1];
				if (val != 0) {
					per = EMUtil.Person.values()[i];
					break;
				}
			}

			for (int i = 0; i < EMUtil.Animacy.values().length - 1; i++) {
				String name = "Y_ani(" + EMUtil.Animacy.values()[i].name()
						+ ")";
				double val = row[nameMap.get(name) - 1];
				if (val != 0) {
					ani = EMUtil.Animacy.values()[i];
					break;
				}
			}

			for (int i = 0; i < EMUtil.pronounList.size(); i++) {
				for (int k = 0; k < this.numberOfAnts; k++) {
					String name = "X(" + k + "," + EMUtil.pronounList.get(i)
							+ ")";
					// System.out.println(name + ":" + row[nameMap.get(name) -
					// 1]);
					double val = row[nameMap.get(name) - 1];
					if (val == 1.0) {
						String pronoun = EMUtil.pronounList.get(i);
						System.out.println(pronoun + ":" + num.name() + ","
								+ gen.name() + "," + per.name() + ","
								+ ani.name());
						if (EMUtil.getNumber(pronoun) != num
								|| EMUtil.getGender(pronoun) != gen
								|| EMUtil.getPerson(pronoun) != per
								|| EMUtil.getAnimacy(pronoun) != ani) {
							Common.pause("GEEE!!");
						}
						return k;
					}
				}
			}
			/* we are done now */
		} else {
			System.out.println(ret);
			Common.bangErrorPOS("!!!");
		}

		/* clean up such that all used memory by lp_solve is freeed */
		if (lp.getLp() != 0)
			lp.deleteLp();

		return -1;
	}

	public static void main(String[] args) {
		int numberOfAnts = 1;
		double[] proAnte = { 0.15, 0.19, 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.05,
				0.1 };
		double[] proNum = { 0.873753, 0.126247 };
		double[] proGen = { 0.646696, 0.0429363, 0.310368 };
		double[] proPer = { 0.197287, 0.150615, 0.652099 };
		double[] proAni = { 0.85597, 0.0106964, 0.133334 };

		try {
			new ILP(numberOfAnts, proAnte, proNum, proGen, proPer, proAni)
					.execute();
		} catch (LpSolveException e) {
			e.printStackTrace();
		}
	}
}

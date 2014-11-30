package em;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.Mention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.Common;
import em.EMUtil.Grammatic;
import em.EMUtil.MentionType;

public class ContextNAACL implements Serializable {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;
        // short antSenPos; // 3 values
        // short antHeadPos; //
        // short antGram; //
        // short proPos; //
        // short antType;// pronoun, proper, common

        String feaL;

        private static ArrayList<int[]> subContext;

    	public static ArrayList<Integer> normConstant = new ArrayList<Integer>();

    	public static ArrayList<int[]> getSubContext() {
    		if (subContext != null) {
    			return subContext;
    		}
    		subContext = new ArrayList<int[]>();
    		int[] a = { 0, 8 };
    		subContext.add(a);
    		
//    		int[] b = { 1, 7 };
//    		subContext.add(b);
//    		int[] c = { 2, 3 };
//    		subContext.add(c);
//    		int[] d = { 3, 4 };
//    		subContext.add(d);
//    		int[] e = { 4, 5 };
//    		subContext.add(e);
//    		int[] f = { 5, 6 };
//    		subContext.add(f);
//    		int[] g = { 6, 7 };
//    		subContext.add(g);
    		
//    		int[] h = { 7, 8 };
//    		subContext.add(h);
//    		int[] i = { 6, 7 };
//    		subContext.add(i);
    		
    		
//    		normConstant.add(2);
//    		normConstant.add(3);
//    		normConstant.add(3);
//    		normConstant.add(2);
//    		normConstant.add(2);
//    		normConstant.add(3);
//    		normConstant.add(2);
//    		normConstant.add(2);
    		normConstant.add(100);

//    		normConstant.add((int) cap);
//    		 int[] g = {6, 7};
//    		 subContext.add(g);
//    		 normConstant.add(2);

    		return subContext;
    	}
        
        public static HashMap<String, ContextNAACL> contextCache = new HashMap<String, ContextNAACL>();

        public static ContextNAACL getContext(short[] feas) {
                // long feaL = 0;
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < feas.length; i++) {
                        if (feas[i] >= 10) {
                                Common.bangErrorPOS("Can't larger than 10:" + feas[i]
                                                + "  Fea:" + i);
                        }
                        // feaL += Math.pow(10, i) * feas[i];
                        sb.append(feas[i]).append("#");
                }
                if (contextCache.containsKey(sb.toString())) {
                        return contextCache.get(sb.toString());
                } else {
                        ContextNAACL c = new ContextNAACL(sb.toString());
                        contextCache.put(sb.toString(), c);
                        return c;
                }
        }

        private ContextNAACL(String feaL) {
                this.feaL = feaL;
        }

        public int hashCode() {
                return this.toString().hashCode();
        }

        public boolean equals(Object obj) {
                ContextNAACL c2 = (ContextNAACL) obj;
                return (this.feaL == c2.feaL);
        }

        public String toString() {
                return this.feaL;
        }

        public static SVOStatNAACL svoStat;
        static short[] feas = new short[18];
        public static ContextNAACL buildContext(Mention ant, Mention pronoun,
                        CoNLLPart part, boolean isFS) {
                MI = calMI(ant, pronoun);

                int antID = ant.start;
                int pronounID = pronoun.start;

                CoNLLSentence pronounS = part.getWord(pronounID).sentence;
                CoNLLSentence antS = part.getWord(antID).sentence;

                int antSID = antS.getSentenceIdx();
                int proSID = pronounS.getSentenceIdx();

                short senDis = (short) (proSID - antSID);

                if (senDis > 2) {
                        senDis = 2;
                }

                short antPos = 0;
                if (senDis == 0) {
                        antPos = (short) (pronounID - antID);
                        antPos = (short) EMUtil.getBucket(antPos,
                                        part.getWord(pronounID).indexInSentence, 4);
                } else {
                        antPos = (short) part.getWord(antID).indexInSentence;
                        antPos = (short) EMUtil.getBucket(antPos,
                                        part.getWord(antID).sentence.words.size(), 4);
                }

                short proPos = (short) EMUtil.getBucket(
                                part.getWord(pronounID).indexInSentence,
                                part.getWord(pronounID).sentence.words.size(), 4);
                short antSynactic = (short) Grammatic.subject.ordinal();
                short antType = 0;
                if (ant.mType == MentionType.tmporal) {
                        antType = 1;
                }
                // = (short) ant.mType.ordinal();

                short antNP = 0;
                MyTreeNode antNP_AncNP = ant.NP.getFirstXAncestor("NP");
                if (antNP_AncNP != null) {
                        antNP = 1;
                        if (ant.NP.getFirstXAncestor("IP") == antNP_AncNP
                                        .getFirstXAncestor("IP")) {
                                antNP = 2;
                        }
                }

                short antVP = 0;
                MyTreeNode antNP_AncVP = ant.NP.getFirstXAncestor("VP");
                if (antNP_AncVP != null) {
                        antVP = 1;
                        if (ant.NP.getFirstXAncestor("IP") == antNP_AncVP
                                        .getFirstXAncestor("IP")) {
                                antVP = 2;
                        }
                }

                short proStart = 0;
                if (part.getWord(pronoun.start).indexInSentence == 0) {
                        proStart = 1;
                } else {
                        MyTreeNode leaf = pronounS.syntaxTree.root.getLeaves().get(
                                        part.getWord(pronounID).indexInSentence);

                        MyTreeNode firstIP = leaf.getFirstXAncestor("IP");
                        if (firstIP != null && firstIP.getLeaves().get(0) == leaf) {
                                proStart = 2;
                        }
                }

                short nearest = 1;
                for (int i = ant.end + 1; i < pronoun.start; i++) {
                        CoNLLWord w = part.getWord(i);
                        MyTreeNode leaf = w.sentence.syntaxTree.leaves
                                        .get(w.indexInSentence);
                        if (leaf.getFirstXAncestor("NP") != null) {
                                nearest = 0;
                                break;
                        }
                }

                short proNP = 0;
                if (pronoun.V.getFirstXAncestor("NP") != null) {
                        proNP = 1;
                }

                short proVP = 0;
                if (pronoun.V.getFirstXAncestor("VP") != null) {
                        proVP = 1;
                }

                short NPClause = getClauseType(ant.NP, antS.syntaxTree.root);
                short VPClause = getClauseType(pronoun.V, pronounS.syntaxTree.root);

                short sameVerb = 0;
                if (ant.gram == EMUtil.Grammatic.subject && ant.V != null
                                && pronoun.V != null) {
                        String antV = EMUtil.getFirstVerb(ant.V);
                        String proV = EMUtil.getFirstVerb(pronoun.V);
                        if (antV.equals(proV)) {
                                if (!antV.equals("是") && !proV.equals("要") && !proV.equals("会")) {
                                        sameVerb = 1;
                                }
                        }
                }
                // maximum 18 features because of the restriction of Long
                feas[0] = senDis;
//               moreFea(antPos, proPos, antSynactic, antType, nearest, NPClause,
//               VPClause, feas);

                feas[1] = antNP;
                feas[2] = antVP;
                
                feas[3] = proNP;
                feas[4] = proVP;
                
                feas[5] = proStart;
                feas[6] = sameVerb;
                
                
                if(ant.salienceID==0 && MI>0) {
                	feas[7] = 0;
                } else{
                	feas[7] = 1;
                } 
                
//                if(MI>0) {
//                	feas[8] = 0;
//                } else {
//                	feas[8] = 1;
//                }
//                if (isFS && MI > 0) {
//                        feas[7] = 1;
//                } else if (ant.isBest) {
//                        feas[7] = 2;
//                } else if (ant.s == ant.s && ant.gram == Grammatic.object
//                                && ant.end + 2 == pronoun.start
//                                && part.getWord(ant.end + 1).word.equals("，") && pronoun.MI > 0) {
////                      feas[0] = 3;
////                      Common.bangErrorPOS("!!");
//                } else {
//                        feas[7] = 0;
//                }

                return ContextNAACL.getContext(feas);
        }

        
        
        
        
        private static void moreFea(short antPos, short proPos, short antSynactic,
                        short antType, short nearest, short NPClause, short VPClause,
                        short[] feas) {
//              feas[1] = nearest;
                feas[2] = antPos;
                feas[3] = antSynactic;
                 feas[4] = proPos;
                feas[5] = antType;
//              feas[11] = NPClause;
//              feas[12] = VPClause;
        }

        public static double voP = 0;
        public static double svoP = 0;
        public static double MI = 0;

        public static String message;

        public static HashSet<String> ss = new HashSet<String>();
        public static HashSet<String> vs = new HashSet<String>();

        public static double calMI2(Mention ant, Mention pronoun) {
                if (svoStat == null) {
                        svoStat = new SVOStatNAACL();
                        svoStat.loadMIInfo();
                }
                String v = EMUtil.getFirstVerb(pronoun.V);
                String o = EMUtil.getObjectNP(pronoun.V);

                String s = EMUtil.getAntAnimacy(ant).name();
                double subjC = getValue(svoStat.unigrams, s);
                // System.out.println(subjC + "##" + s + "###" +
                // svoStat.unigrams.size());

                double subjP = (subjC + 1)
                                / (svoStat.unigramAll + svoStat.unigrams.size());

                // if (o != null && svoStat.voCounts.containsKey(v + " " + o)) {
                // double voC = getValue(svoStat.voCounts, v + " " + o);
                // voP = (voC) / (svoStat.svoAll);
                //
                // double svoC = getValue(svoStat.svoCounts, s + " " + v + " " + o);
                // svoP = (svoC) / (svoStat.svoAll);
                //
                // } else {
                if (!svoStat.vCounts.containsKey(v) || svoStat.vCounts.get(v) < 1000) {
                        return 1;
                }

                double voC = getValue(svoStat.vCounts, v);
                voP = (voC) / (svoStat.svoAll);

                double svoC = getValue(svoStat.svCounts, s + " " + v);
                svoP = (svoC) / (svoStat.svoAll);
                // }

                // }

                double MI = Math.log(svoP / (voP * subjP));
                // System.out.println(subjP + " " + voP + " " + svoP);
                // System.out.println(MI + s + " " + v + " " + o);
                // System.out.println("======");

                message = subjP + " " + voP + " " + svoP + '\n' + MI + s + " " + v
                                + " " + o + '\n' + "======";
                return MI;
        }

        public static double calMI(Mention ant, Mention pronoun) {
//               if(true)
//               return 1;
                if (svoStat == null) {
                        long start = System.currentTimeMillis();
                        ObjectInputStream modelInput;
//                      try {
//                              modelInput = new ObjectInputStream(new FileInputStream(
//                                              "/dev/shm/svoStat"));
//                              svoStat = (SVOStatNAACL) modelInput.readObject();
                                 svoStat = new SVOStatNAACL();
                                 svoStat.loadMIInfo();
//                      } catch (FileNotFoundException e) {
//                              // TODO Auto-generated catch block
//                              e.printStackTrace();
//                      } catch (IOException e) {
//                              // TODO Auto-generated catch block
//                              e.printStackTrace();
//                      } catch (ClassNotFoundException e) {
//                              // TODO Auto-generated catch block
//                              e.printStackTrace();
//                      }
//                      System.out.println(System.currentTimeMillis() - start);
                }
                String s = ant.head;
                String pos = ant.s.getWord(ant.headInS).posTag;
                String v = EMUtil.getFirstVerb(pronoun.V);
                String o = EMUtil.getObjectNP(pronoun.V);

                // System.out.println(s + " " + v + " " + o);
                String NE = ant.NE;
                if (ant.NE.equals("OTHER") && EMUtil.NEMap != null
                                && EMUtil.NEMap.containsKey(ant.head)) {
                        NE = EMUtil.NEMap.get(ant.head);
                }

                if (NE.equals("PERSON")) {
                        s = "他";
                        pos = "PN";
                } else if (NE.equals("LOC") || NE.equals("GPE") || NE.equals("ORG")) {
                        s = "它";
                        pos = "PN";
                }
                // else if(NE.equals("ORG")) {
                // s = "公司";
                // pos = "NN";
                // }

                if (!svoStat.unigrams.containsKey(s + " " + pos)
                                || svoStat.unigrams.get(s + " " + pos) < 15000) {
                        return 1;
                }

                if (EMUtil.train) {
                        ss.add(s);
                        vs.add(v);
                } else if (!ss.contains(s) || vs.contains(v)) {
                        // return 1;
                }

                double subjC = getValue(svoStat.unigrams, s + " " + pos);
                double subjP = (subjC + 1)
                                / (svoStat.unigramAll + svoStat.unigrams.size());

                // if (o != null && svoStat.voCounts.containsKey(v + " " + o)) {
                // double voC = getValue(svoStat.voCounts, v + " " + o);
                // voP = (voC) / (svoStat.svoAll);
                //
                // double svoC = getValue(svoStat.svoCounts, s + " " + v + " " + o);
                // svoP = (svoC) / (svoStat.svoAll);
                // } else {
                if (!svoStat.vCounts.containsKey(v) || svoStat.vCounts.get(v) < 1000) {
                        return 1;
                }

                double voC = getValue(svoStat.vCounts, v);
                voP = (voC) / (svoStat.svoAll);

                double svoC = getValue(svoStat.svCounts, s + " " + v);
                svoP = (svoC) / (svoStat.svoAll);
                // }

                double MI = Math.log(svoP / (voP * subjP));
                // System.out.println(subjP + " " + voP + " " + svoP);
                // System.out.println(MI + s + " " + v + " " + o);
                // System.out.println("======");

                message = subjP + " " + voP + " " + svoP + '\n' + MI + s + " " + NE
                                + " " + v + " " + o + '\n' + "======";
                return MI;
        }

        public static double getValue(HashMap<String, Integer> map, String key) {
                if (map.containsKey(key)) {
                        return map.get(key);
                } else {
                        return 0.00000001;
                }
        }

        public static short getClauseType(MyTreeNode node, MyTreeNode root) {
                int IPCounts = node.getXAncestors("IP").size();
                if (IPCounts > 1) {
                        // subordinate clause
                        return 2;
                } else {
                        int totalIPCounts = 0;
                        ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
                        frontie.add(root);
                        while (frontie.size() > 0) {
                                MyTreeNode tn = frontie.remove(0);
                                if (tn.value.toLowerCase().startsWith("ip")) {
                                        totalIPCounts++;
                                }
                                frontie.addAll(tn.children);
                        }
                        if (totalIPCounts > 1) {
                                // matrix clause
                                return 1;
                        } else {
                                // independent clause
                                return 0;
                        }
                }
        }
        
        public String getKey(int i) {
    		// return this.toString().substring(subContext.get(i)[0],
    		// subContext.get(i)[1]);
    		String tks[] = this.toString().split("#");
    		StringBuilder sb = new StringBuilder();
    		for (int m = subContext.get(i)[0]; m < subContext.get(i)[1]; m++) {
    			sb.append(tks[m]).append("#");
    		}
    		return sb.toString();
    	}
        
}

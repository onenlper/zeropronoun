package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import util.Common;

import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.syntaxTree.MyTreeNode;
import align.DocumentMap.Unit;
import em.EMUtil;
import em.EMUtil.MentionType;

public class Mention implements Comparable<Mention>, Serializable {

        /**
         *
         */
        double th = 0.0;

        public boolean isSub;
        
        public boolean isSubject;
        
        public boolean isObject;
        
        public int salienceID = 0;
        
        public boolean isFake = false;
       
        public ArrayList<Unit> units = new ArrayList<Unit>();

        public int PRONOUN_TYPE;

        public MentionType mentionType;

        public boolean isNNP = false;

        public ArrayList<String> modifyList = new ArrayList<String>();

        public boolean isProperNoun = false;

        public boolean isPronoun = false;

        public boolean generic = false;

        public static int assignMode = 0;

        public int xSpanType = 0;

        public double alignProb = 0;

        public boolean isAZP = false;

        private static final long serialVersionUID = 1L;
        public int start = -1;
        public int end = -1;
        public String extent = "";

        public Entity entity;

        public Entity sysEntity;
        
        public Mention antecedent;

        public String msg;

        public double MI;

        public boolean notInChainZero;

        public int sentenceID;

        public CoNLLSentence s;

        public String head = "";

        public int entityIndex;

        public int startInS;
        public int endInS;

        public int headInS;

        public int headID;

        public EMUtil.Grammatic gram;
        public EMUtil.MentionType mType;

        public EMUtil.Number number;
        public EMUtil.Gender gender;
        public EMUtil.Person person;
        public EMUtil.Animacy animacy;

        public EMUtil.PersonEng personEng;

        public MyTreeNode V;

        public MyTreeNode NP;

        public String NE = "OTHER";

        public boolean isFS = false;

        public boolean isBest = false;

        // TODO
        public boolean isQuoted = false;

        public int getSentenceID() {
                return sentenceID;
        }

        public void setSentenceID(int sentenceID) {
                this.sentenceID = sentenceID;
        }

        public int hashCode() {
                if (this.s != null && this.s.part != null) {
                        String str = this.s.part.getPartName() + "#" + this.start + ","
                                        + this.end;
                        return str.hashCode();
                } else {
                        String str = this.start + "," + this.end;
                        return str.hashCode();
                }

        }

        public boolean equals(Object em2) {
                if (this.start == ((Mention) em2).start
                                && this.end == ((Mention) em2).end) {
                        return true;
                } else {
                        return false;
                }
        }

        public int getStart() {
                return start;
        }

        public void setStart(int start) {
                this.start = start;
        }

        public int getEnd() {
                return end;
        }

        public void setEnd(int end) {
                this.end = end;
        }

        public String getExtent() {
                return extent;
        }

        public void setExtent(String extent) {
                this.extent = extent;
        }

        public String getHead() {
                return head;
        }

        public void setHead(String head) {
                this.head = head;
        }

        public Mention() {

        }

        public Mention(int start, int end) {
                this.start = start;
                this.end = end;
        }

        // (14, 15) (20, -1) (10, 20)
        public int compareTo(Mention emp2) {
                int diff = this.start - emp2.start;
                if (diff == 0)
                        return emp2.end - this.end;
                else
                        return diff;
                // if(this.getE()!=-1 && emp2.getE()!=-1) {
                // int diff = this.getE() - emp2.getE();
                // if(diff==0) {
                // return this.getS() - emp2.getS();
                // } else
                // return diff;
                // } else if(this.getE()==-1 && emp2.headEnd!=-1){
                // int diff = this.getS() - emp2.getE();
                // if(diff==0) {
                // return -1;
                // } else
                // return diff;
                // } else if(this.headEnd!=-1 && emp2.headEnd==-1){
                // int diff = this.getE() - emp2.getS();
                // if(diff==0) {
                // return 1;
                // } else
                // return diff;
                // } else {
                // return this.getS()-emp2.getS();
                // }
        }

        public String toName() {
                String str = this.start + "," + this.end;
                return str;
        }

        public String toString() {
                String str = this.start + "," + this.end;
                return str;
        }

        // find mapped span
        public Mention getXSpan() {
                Mention xSpan = this.getXSpanFromCache();
                if (xSpan == null && assignMode >= 1 && assignMode <= 4) {
                        assignXSpan();
                }

                if (xSpan != null && this.s.part.lang.equals("chi")) {
                        // mappedChiMs.add(this.getMK());
                }

                return xSpan;
        }

        // enforce one-one map
        public static HashMap<String, Mention> chiSpanMaps = new HashMap<String, Mention>();
        public static HashMap<String, Mention> engSpanMaps = new HashMap<String, Mention>();

        public static HashMap<String, HashSet<String>> headMaps = new HashMap<String, HashSet<String>>();

        public String getReadName() {
                return this.s.part.getPartName() + ":" + this.s.part.lang + ":"
                                + this.start + "," + this.end;
        }

        private Mention getXSpanFromCache() {
                if (this.s.part.lang.equals("chi")) {
                        Mention xSpan = chiSpanMaps.get(this.getReadName());
                        if (xSpan != null) {
                                this.xSpanType = xSpan.xSpanType;
                                this.alignProb = xSpan.alignProb;
                        }
                        return xSpan;
                } else {
                        Mention xSpan = engSpanMaps.get(this.getReadName());
                        if (xSpan != null) {
                                this.xSpanType = xSpan.xSpanType;
                                this.alignProb = xSpan.alignProb;
                        }
                        return xSpan;
                }
        }

        private Mention assignXSpan() {
                Mention xSpan = null;
                if (assignMode == 1) {
                        xSpan = this.getExactMatchXSpan();
                        if (xSpan != null) {
                                // xSpan.xSpanType = 1;
                                this.xSpanType = xSpan.xSpanType;
                                this.alignProb = xSpan.alignProb;
                        }
                }
                if (assignMode == 2) {
                        // xSpan = this.getPartialMatchXSpan();
                        if (xSpan != null) {
                                xSpan.xSpanType = 5;
                                this.xSpanType = xSpan.xSpanType;
                                this.alignProb = xSpan.alignProb;
                        }
                }
                if (assignMode == 3) {
                        // xSpan = this.getSameTextMapSpan();
                        if (xSpan != null) {
                                xSpan.xSpanType = 6;
                                this.xSpanType = xSpan.xSpanType;
                                this.alignProb = xSpan.alignProb;
                        }
                }
                if (assignMode == 4) {
                        xSpan = this.getCreatedSpan();
                        if (xSpan != null) {
                                xSpan.xSpanType = 7;
                                this.xSpanType = xSpan.xSpanType;
                                this.alignProb = xSpan.alignProb;
                        }
                }

                if (xSpan != null) {
                        if (this.s.part.lang.equalsIgnoreCase("chi")) {
                                // System.out.println(this.getText() + "#" + xSpan.getText() +
                                // "#");
                                // System.out.println(this.s.toString());
                                // System.out.println(xSpan.s.toString());
                                // System.out.println("==" + (a++) +"==");
                        }

                        boolean put = false;
                        if (this.s.part.lang.equals("eng")
                                        && (chiSpanMaps.get(xSpan.getReadName()) == null || chiSpanMaps
                                                        .get(xSpan.getReadName()).getReadName()
                                                        .equals(this.getReadName()))) {
                                put = true;
                        } else if (this.s.part.lang.equals("chi")
                                        && (engSpanMaps.get(xSpan.getReadName()) == null || engSpanMaps
                                                        .get(xSpan.getReadName()).getReadName()
                                                        .equals(this.getReadName()))) {
                                put = true;
                        }
                        if (put) {
                                if (this.s.part.lang.equals("eng")) {
                                        engSpanMaps.put(this.getReadName(), xSpan);
                                        chiSpanMaps.put(xSpan.getReadName(), this);
                                } else {
                                        chiSpanMaps.put(this.getReadName(), xSpan);
                                        engSpanMaps.put(xSpan.getReadName(), this);
                                }
                                String head = this.head;
                                HashSet<String> xHeads = headMaps.get(head);
                                if (xHeads == null) {
                                        xHeads = new HashSet<String>();
                                        headMaps.put(head, xHeads);
                                }
                                String xHead = xSpan.head.toLowerCase();
                                xHeads.add(xHead);
                        }
                }
                return xSpan;
        }

        private Mention getExactMatchXSpan() {
                // match head id
                Mention xSpan = null;
                Unit headUnit = null;
                for (Unit u : this.units) {
                        if (u.getToken().equalsIgnoreCase(this.head)) {
//                              System.out.println(this.head + "#" + this.extent);
                                headUnit = u;
                                break;
                        }
                }

                if (headUnit != null) {
                        // ordered
                        ArrayList<Unit> xUnits = headUnit.getMapUnit();
                        loop: for (int i = 0; i < xUnits.size(); i++) {
                                Unit xUnit = xUnits.get(i);
                                double prob = 1;
                                if (headUnit.getMapProb().size() != 0) {
                                        prob = headUnit.getMapProb().get(i);
                                        if (prob < th) {
                                                continue;
                                        }
                                }
                                // System.out.println("HEE?" + xUnit.mentions.size());
                                for (Mention xs : xUnit.mentions) {
                                        String head = xs.head;
                                        if (head.equals(xUnit.getToken())
                                                        && xs.ccStruct() == this.ccStruct()) {
                                                xSpan = xs;
                                                xSpan.xSpanType = (int) Math.ceil((prob / 0.25));
                                                xSpan.alignProb = prob;
                                                // TODO
                                                break loop;
                                        }
                                }
                        }
                }
                return xSpan;
        }

        private boolean ccStruct() {
                boolean cc = false;
                for (int i = this.startInS; i <= this.endInS; i++) {
                        String tag = this.s.getWord(i).posTag;
                        if (tag.equalsIgnoreCase("CC")) {
                                cc = true;
                                break;
                        }
                }
                return cc;
        }

        public Mention getCreatedSpan() {
                Mention xSpan = null;
                // int hdID = this.s.ids[this.hd];

                int leftID = this.start;
                int rightID = this.end;

                Unit xStartU = null;
                // System.out.println(this.s);
                // System.out.println(this.s.part);
                // System.out.println(this.s.part.itself);
                Unit leftUnit = this.units.get(0);
                ArrayList<Unit> xUnits = leftUnit.getMapUnit();
                for (int i = 0; i < xUnits.size(); i++) {
                        Unit xUnit = xUnits.get(i);
                        if (xUnit.sentence == null) {
                                continue;
                        }
                        double prob = leftUnit.getMapProb().get(i);
                        if (prob < th) {
                                continue;
                        }
                        xStartU = xUnit;
                        break;
                }

                if (xStartU != null) {
                        Unit rightUnit = this.units.get(this.units.size() - 1);
                        xUnits = rightUnit.getMapUnit();
                        for (int i = 0; i < xUnits.size(); i++) {
                                Unit xUnit = xUnits.get(i);
                                if (xUnit.sentence == null
                                                || xUnit.sentence != xStartU.sentence) {
                                        continue;
                                }
                                double prob = rightUnit.getMapProb().get(i);
                                if (prob < th) {
                                        continue;
                                }
                                xSpan = xUnit.sentence.getSpan(xStartU.indexInSentence,
                                                xUnit.indexInSentence);
                                xSpan.alignProb = prob;
                                break;
                        }
                }
                return xSpan;
        }

}


package em;

import java.util.ArrayList;
import java.util.HashSet;

import model.Mention;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTreeNode;
import util.Common;
import util.Common.Feature;
import util.YYFeature;

public class SuperviseFea extends YYFeature {

        public SuperviseFea(boolean train, String name) {
                super(train, name);
                // TODO Auto-generated constructor stub
        }

        Context context;

        public static boolean plusNumberGenderPersonAnimacy = true;

        String personStr;
        String numberStr;
        String genderStr;
        String animacyStr;

        Mention cand;
        Mention zero;
        CoNLLPart part;

        public void configure(String pStr, String nStr, String gStr, String aStr,
                        Context context, Mention ant, Mention pronoun, CoNLLPart part) {
                this.personStr = pStr;
                this.numberStr = nStr;
                this.genderStr = gStr;
                this.animacyStr = aStr;
                this.context = context;

                this.cand = ant;
                this.zero = pronoun;
                this.part = part;
        }

        @Override
        public ArrayList<Feature> getCategoryFeatures() {
                ArrayList<Feature> feas = new ArrayList<Feature>();
                String str = "";
                feas.addAll(this.getEMNLPCate());
                if (this.context != null) {
                        str = this.context.feaL;
                        for (int i = 0; i < str.length(); i++) {
                                int idx = Integer.parseInt(str.substring(i, i + 1));
                                Feature feature = new Feature(idx, 1, 10);
                                feas.add(feature);
                        }
                }
                return feas;
        }

        public ArrayList<String> getFeas() {
                ArrayList<String> strs = new ArrayList<String>();

                if (plusNumberGenderPersonAnimacy) {
                        strs.add(personStr);
                        strs.add(numberStr);
                        strs.add(genderStr);
                        strs.add(animacyStr);
                }

                // strs.add(this.part.folder);
                // strs.add(context.feaL);
                strs.addAll(getEMLNLPStrFeatures());
                return strs;
        }

        @Override
        public ArrayList<HashSet<String>> getStrFeatures() {
                ArrayList<HashSet<String>> ret = new ArrayList<HashSet<String>>();
                for (String str : getFeas()) {
                        HashSet<String> set = new HashSet<String>();
                        set.add(str);
                        ret.add(set);
                }
                return ret;
        }

        public ArrayList<Feature> getEMNLPCate() {
                ArrayList<Feature> feas = new ArrayList<Feature>();
                feas.addAll(this.getZeroFeature());
                
                if(!this.cand.isFake) {
                        feas.addAll(this.getNPFeature());
                        feas.addAll(this.getZeroAnaphorFeature());
                        feas.addAll(this.getCFeatures());
                }
                return feas;
        }

        private ArrayList<Feature> getNPFeature() {
                ArrayList<Feature> features = new ArrayList<Feature>();

                CoNLLSentence s = part.getCoNLLSentences().get(cand.sentenceID);
                MyTreeNode root = s.syntaxTree.root;

                MyTreeNode NPNode = cand.NP;
                // 0. A_HAS_ANC_NP
                if (NPNode.getFirstXAncestor("NP") != null) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 5. A_HAS_ANC_NP_IN_IP
                if (NPNode.getFirstXAncestor("NP") != null
                                && Common.isAncestor(NPNode.getFirstXAncestor("NP"),
                                                NPNode.getFirstXAncestor("IP"))) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 6. HAS_ANC_VP
                if (NPNode.getFirstXAncestor("VP") != null) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 7. A_HAS_ANC_VP_IN_IP
                if (NPNode.getFirstXAncestor("VP") != null
                                && Common.isAncestor(NPNode.getFirstXAncestor("VP"),
                                                NPNode.getFirstXAncestor("IP"))) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 8. HAS_ANC_CP
                if (NPNode.getFirstXAncestor("CP") != null) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                boolean object = false;
                boolean subject = false;
                ArrayList<MyTreeNode> rightSisters = NPNode.getRightSisters();
                ArrayList<MyTreeNode> leftSisters = NPNode.getLeftSisters();
                for (MyTreeNode node : rightSisters) {
                        if (node.value.equalsIgnoreCase("VP")) {
                                subject = true;
                                break;
                        }
                }

                for (MyTreeNode node : leftSisters) {
                        if (node.value.equalsIgnoreCase("VV")) {
                                object = true;
                                break;
                        }
                }

                // 9. A_GRAMMATICAL_ROLE
                if (subject) {
                        features.add(new Feature(0, 1, 3));
                } else if (object) {
                        features.add(new Feature(1, 1, 3));
                } else {
                        features.add(new Feature(2, 1, 3));
                }
                // 10. A_CLAUSE
                int IPCounts = NPNode.getXAncestors("IP").size();
                if (IPCounts > 1) {
                        // subordinate clause
                        features.add(new Feature(0, 1, 3));
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
                                features.add(new Feature(1, 1, 3));
                        } else {
                                // independent clause
                                features.add(new Feature(2, 1, 3));
                        }
                }

                // A is an adverbial NP
                // if (NP.value.toLowerCase().contains("adv")) {
                // fea[7] = 0;
                // } else {
                // fea[7] = 1;
                // }

                // 12. A is a temporal NP
                if (NPNode.getLeaves().size() != 0
                                && NPNode.getLeaves().get(NPNode.getLeaves().size() - 1).parent.value
                                                .equals("NT")) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // is pronoun
                if (EMUtil.pronouns.contains(cand.extent)) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 14. A is a named entity
                if (!cand.NE.equalsIgnoreCase("other")) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 15. if in headline
                if (cand.sentenceID == 0) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }
                return features;
        }

        private ArrayList<Feature> getZeroFeature() {
                ArrayList<Feature> features = new ArrayList<Feature>();
                CoNLLSentence s = part.getCoNLLSentences().get(zero.sentenceID);
                MyTreeNode root = s.syntaxTree.root;

                MyTreeNode V = zero.V;

                // 0. Z_Has_anc_NP
                if (V.getXAncestors("NP") == null) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }
                // if(!ancestors.get(0).value.contains("IP")) {
                // System.out.println();
                // }

                // 1. Z_Has_Anc_NP_In_IP
                if (V.getFirstXAncestor("NP") != null
                                && Common.isAncestor(V.getFirstXAncestor("NP"),
                                                V.getFirstXAncestor("IP"))) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 2. Z_Has_Anc_VP
                if (V.getXAncestors("VP") == null) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 3. Z_Has_Anc_VP_In_IP
                if (V.getFirstXAncestor("VP") != null
                                && Common.isAncestor(V.getFirstXAncestor("VP"),
                                                V.getFirstXAncestor("IP"))) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 4. Z_Has_Anc_CP
                if (V.getXAncestors("CP") == null) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 10. SUBJECT
                if (V.parent.value.equalsIgnoreCase("ip")) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }
                // 11. Clause
                int IPCounts = 0;
                MyTreeNode temp = V;
                while (temp != root) {
                        if (temp.value.toLowerCase().startsWith("ip")) {
                                IPCounts++;
                        }
                        temp = temp.parent;
                }
                if (IPCounts > 1) {
                        // subordinate clause
                        features.add(new Feature(2, 1, 3));
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
                                features.add(new Feature(0, 1, 3));
                        } else {
                                features.add(new Feature(1, 1, 3));
                        }
                }
                // headline feature
                if (zero.sentenceID == 0) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // 7. IS_FIRST_ZP
                // int zeroIdx = this.zeros.indexOf(zero);
                // if (zeroIdx == 0 || this.zeros.get(zeroIdx - 1).sentenceID !=
                // zero.sentenceID) {
                // features.add(new Feature(0, 1, 2));
                // } else {
                // features.add(new Feature(1, 1, 2));
                // }
                //
                // if (zeroIdx == this.zeros.size() - 1 || this.zeros.get(zeroIdx +
                // 1).sentenceID != zero.sentenceID) {
                // features.add(new Feature(0, 1, 2));
                // } else {
                // features.add(new Feature(1, 1, 2));
                // }

                return features;
        }

        public ArrayList<Feature> getZeroAnaphorFeature() {
                ArrayList<Feature> features = new ArrayList<Feature>();
                int sentenceDis = zero.sentenceID - cand.sentenceID;
                sentenceDis = sentenceDis > 30 ? 30 : sentenceDis;
                features.add(new Feature(sentenceDis, 1, 31));

                int segmentDis = 0;
                for (int i = cand.start; i <= zero.start; i++) {
                        String word = part.getWord(i).word;
                        if (word.equals("，") || word.equals("；") || word.equals("。")
                                        || word.equals("！") || word.equals("？")) {
                                segmentDis++;
                        }
                }
                segmentDis = segmentDis > 30 ? 30 : segmentDis;
                features.add(new Feature(segmentDis, 1, 31));

                // sibling
                if (cand.end != -1) {
                        CoNLLSentence s = part.getCoNLLSentences().get(zero.sentenceID);
                        MyTreeNode root = s.syntaxTree.root;
                        CoNLLWord word = part.getWord(zero.start);
                        MyTreeNode V = zero.V;
                        boolean sibling = false;

                        if (sentenceDis == 0) {
                                MyTreeNode leftLeaf = root.getLeaves().get(
                                                part.getWord(cand.start).indexInSentence);
                                MyTreeNode rightLeaf = root.getLeaves().get(
                                                part.getWord(cand.end).indexInSentence);
                                MyTreeNode NPNode = Common.getLowestCommonAncestor(leftLeaf,
                                                rightLeaf);
                                if (V.parent == NPNode.parent) {
                                        if (V.childIndex - 1 == NPNode.childIndex) {
                                                sibling = true;
                                        }
                                        if (V.childIndex - 2 == NPNode.childIndex
                                                        && V.parent.children.get(V.childIndex - 2).children
                                                                        .get(0).value.equalsIgnoreCase("，")) {
                                                sibling = true;
                                        }
                                }
                        }
                        if (sibling) {
                                features.add(new Feature(0, 1, 2));
                        } else {
                                features.add(new Feature(1, 1, 2));
                        }
                } else {
                        features.add(new Feature(1, 0, 2));
                }

                // closet np
                short nearest = 1;
                for (int i = cand.end + 1; i < zero.start; i++) {
                        CoNLLWord w = part.getWord(i);
                        MyTreeNode leaf = w.sentence.syntaxTree.leaves
                                        .get(w.indexInSentence);
                        if (leaf.getFirstXAncestor("NP") != null) {
                                nearest = 0;
                                break;
                        }
                }
                if (nearest == 1) {
                        features.add(new Feature(0, 1, 2));
                } else {
                        features.add(new Feature(1, 1, 2));
                }

                // int npIndex = this.candidates.indexOf(cand);
                // if (npIndex == this.candidates.size() - 1 ||
                // this.candidates.get(npIndex + 1).compareTo(zero) > 0) {
                // features.add(new Feature(0, 1, 2));
                // } else {
                //
                // }
                return features;
        }

        public ArrayList<Feature> getCFeatures() {
                ArrayList<Feature> features = new ArrayList<Feature>();

                // if (cand.compareTo(zero) > 0) {
                // features.add(new Feature(0, 1, 2));
                // } else {
                // features.add(new Feature(1, 1, 2));
                // }
                // if (cand.end == -1) {
                // features.add(new Feature(0, 1, 2));
                // } else {
                // features.add(new Feature(1, 1, 2));
                // }
                // same VV
                CoNLLSentence sentence = part.getWord(zero.start).sentence;

                // end with ? or .
                String lastWord = sentence.words.get(sentence.words.size() - 1).word;
                String zeroSpeaker = part.getWord(zero.start).speaker;
                String candSpeaker = part.getWord(cand.start).speaker;

                if (lastWord.equals("?")) {
                        if ((zeroSpeaker.equals(candSpeaker) && cand.extent.equals("你"))
                                        || (!zeroSpeaker.equals(candSpeaker) && cand.extent
                                                        .equals("我"))) {
                                features.add(new Feature(0, 1, 4));
                        } else if ((zeroSpeaker.equals(candSpeaker) && cand.extent
                                        .equals("我"))
                                        || (!zeroSpeaker.equals(candSpeaker) && cand.extent
                                                        .equals("你"))) {
                                features.add(new Feature(1, 1, 4));
                        } else {
                                features.add(new Feature(2, 1, 4));
                        }
                } else {
                        features.add(new Feature(3, 1, 4));
                }

                if (lastWord.equals(".")) {
                        if ((zeroSpeaker.equals(candSpeaker) && cand.extent.equals("你"))
                                        || (!zeroSpeaker.equals(candSpeaker) && cand.extent
                                                        .equals("我"))) {
                                features.add(new Feature(0, 1, 4));
                        } else if ((zeroSpeaker.equals(candSpeaker) && cand.extent
                                        .equals("我"))
                                        || (!zeroSpeaker.equals(candSpeaker) && cand.extent
                                                        .equals("你"))) {
                                features.add(new Feature(1, 1, 4));
                        } else {
                                features.add(new Feature(2, 1, 4));
                        }
                } else {
                        features.add(new Feature(3, 1, 4));
                }
                return features;
        }

        public ArrayList<String> getEMLNLPStrFeatures() {
                String canHead = cand.head;

                // if (!zeroSpeaker.equals(candSpeaker)) {
                // if (canHead.equals("我")) {
                // canHead = "你";
                // } else if (canHead.equals("你")) {
                // canHead = "我";
                // }
                // }
                ArrayList<String> strFeas = new ArrayList<String>();
                strFeas.add(canHead);
                strFeas.add(canHead + "#" + EMUtil.getPredicateNode(zero.V));
                strFeas.add(canHead + "#" + EMUtil.getPredicateNode(zero.V) + "#"
                                + EMUtil.getObjectNP(zero.V));
                MyTreeNode v1 = cand.V;
                MyTreeNode v2 = zero.V;
                if (v1 != null & v2 != null) {
                        String pred1N = EMUtil.getPredicateNode(v1);
                        String pred2N = EMUtil.getPredicateNode(v2);
                        String pred1 = "";
                        if (pred1N != null) {
                                pred1 = pred1N;
                        }
                        String pred2 = "";
                        if (pred2N != null) {
                                pred2 = pred2N;
                        }
                        strFeas.add(pred1 + "#" + pred2);
                } else {
                        strFeas.add("#");
                }
                // strFeas.add(this.semStr);
                // strFeas.clear();
                return strFeas;
        }

}
	package em;
	
	import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
	
	import model.Element;
import model.Entity;
import model.GraphNode;
import model.Mention;
import model.CoNLL.CoNLLDocument;
import model.CoNLL.CoNLLPart;
import model.CoNLL.CoNLLSentence;
import model.CoNLL.CoNLLWord;
import model.syntaxTree.MyTree;
import model.syntaxTree.MyTreeNode;
import util.Common;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
	
	public class EMUtil {
	
		public static boolean train;
	
		public static double alpha = Math.pow(10, -5);
//		public static double alpha = 1;
		// 0.0000001;
		// -7
	
		public static HashSet<String> location = Common
				.readFile2Set("location_suffix");
	
		public static HashSet<String> pronouns = new HashSet<String>(Arrays.asList(
				"你", "我", "他", "她", "它", "你们", "我们", "他们", "她们", "它们"
		// , "这", "这里", "那", "那里"
				));
	
		public static ArrayList<String> engPronounList = new ArrayList<String>(
				Arrays.asList("you", "i", "he", "she", "it", "we", "they"));
	
		public static HashSet<String> engPronouns = new HashSet<String>(
				Arrays.asList("you", "i", "he", "she", "it", "we", "they"));
	
		public static HashMap<String, Integer> pronounArr = new HashMap<String, Integer>() {
			/**
					 * 
					 */
			public static final long serialVersionUID = 1L;
			{
				put("你", 1);
				put("我", 2);
				put("他", 3);
				put("她", 4);
				put("它", 5);
				put("你们", 1);
				put("我们", 2);
				put("他们", 3);
				put("她们", 4);
				put("它们", 5);
			}
		};
	
		public static enum Person {
			first, second, third
		};
	
		public static enum Number {
			single, plural
		};
	
		public static enum Gender {
			male, female, neuter, unknown
		};
	
		public static enum PersonEng {
			I, YOU, HE, SHE, WE, THEY, IT, UNKNOWN, YOUS
		}
	
		public static enum Animacy {
			animate, unanimate, unknown
		}
	
		public static enum Grammatic {
			subject, object, other
		};
	
		public static enum MentionType {
			pronoun, proper, common, tmporal
		}
	
		public static HashSet<String> firsts = new HashSet<String>(Arrays.asList(
				"我", "我们"));
	
		public static HashSet<String> seconds = new HashSet<String>(Arrays.asList(
				"你", "你们"));
	
		public static HashSet<String> thirds = new HashSet<String>(Arrays.asList(
				"他", "她", "它", "他们", "她们", "它们"));
	
		public static HashSet<String> singles = new HashSet<String>(Arrays.asList(
				"你", "我", "他", "她", "它"));
	
		public static HashSet<String> plurals = new HashSet<String>(Arrays.asList(
				"你们", "我们", "他们", "她们", "它们"));
	
		public static HashSet<String> males = new HashSet<String>(Arrays.asList(
				"他", "他们"));
	
		public static HashSet<String> females = new HashSet<String>(Arrays.asList(
				"她", "她们"));
	
		public static HashSet<String> neuters = new HashSet<String>(Arrays.asList(
				"它", "它们", "你", "我", "我们", "你们"));
	
		public static HashSet<String> animates = new HashSet<String>(Arrays.asList(
				"你", "我", "他", "她", "你们", "我们", "他们", "她们"));
	
		public static HashSet<String> unanimates = new HashSet<String>(
				Arrays.asList("它", "它们"));
	
		public final static Set<String> removeChars = new HashSet<String>(
				Arrays.asList(new String[] { "什么的", "哪", "什么", "谁", "啥", "哪儿",
						"哪里", "人们", "年", "原因", "啥时", "问题", "情况", "未来", "战争", "人",
						"时候", "可能" }));
	
		public static ArrayList<String> pronounList = new ArrayList<String>(
				Arrays.asList("你", "我", "他", "她", "它", "你们", "我们", "他们", "她们", "它们"));
	
		public static void addEmptyCategoryNode(Mention zero) {
			MyTreeNode V = zero.V;
			MyTreeNode newNP = new MyTreeNode();
			newNP.value = "NP";
			int VIdx = V.childIndex;
			V.parent.addChild(VIdx, newNP);
	
			MyTreeNode empty = new MyTreeNode();
			empty.value = "-NONE-";
			newNP.addChild(empty);
	
			MyTreeNode child = new MyTreeNode();
			child.value = "*pro*";
			empty.addChild(child);
			child.emptyCategory = true;
			zero.NP = newNP;
		}
	
		public static HashMap<String, CoNLLSentence> loadTranslateEngCoNLL(
				CoNLLDocument doc, String setting) {
			ArrayList<String> chiLines = Common.getLines(setting + "/docs/train.f");
			HashMap<String, CoNLLSentence> sMap = new HashMap<String, CoNLLSentence>();
			for (int i = 0; i < chiLines.size(); i++) {
				String chi = chiLines.get(i);
				CoNLLSentence s = doc.getParts().get(0).getCoNLLSentences().get(i);
				sMap.put(chi, s);
			}
			return sMap;
		}
	
		public static short getProIdx(String extent) {
			int idx = pronounList.indexOf(extent);
			return (short) idx;
		}
	
		public static EMUtil.Number getNumber(String pro) {
			if (singles.contains(pro)) {
				return Number.single;
			} else if (plurals.contains(pro)) {
				return Number.plural;
			} else {
				Common.bangErrorPOS("Not support number: " + pro);
				return null;
			}
		}
	
		public static EMUtil.Gender getGender(String pro) {
			if (males.contains(pro)) {
				return Gender.male;
			} else if (females.contains(pro)) {
				return Gender.female;
			} else if (neuters.contains(pro)) {
				return Gender.neuter;
			} else {
				Common.bangErrorPOS("Not support gender: " + pro);
				return null;
			}
		}
	
		public static EMUtil.Person getPerson(String pro) {
			if (firsts.contains(pro)) {
				return Person.first;
			} else if (seconds.contains(pro)) {
				return Person.second;
			} else if (thirds.contains(pro)) {
				return Person.third;
			} else {
				Common.bangErrorPOS("Not support person: " + pro);
				return null;
			}
		}
	
		public static Person getAntPerson(String pro) {
			if (firsts.contains(pro)) {
				return Person.first;
			} else if (seconds.contains(pro)) {
				return Person.second;
			} else {
				return Person.third;
			}
		}
	
		public static EMUtil.Animacy getAnimacy(String pro) {
			if (animates.contains(pro)) {
				return Animacy.animate;
			} else if (unanimates.contains(pro)) {
				return Animacy.unanimate;
			} else {
				Common.bangErrorPOS("Not support animacy: " + pro);
				return null;
			}
		}
	
		public static ArrayList<Mention> getAnaphorZeros(ArrayList<Entity> chains) {
			ArrayList<Mention> zeros = new ArrayList<Mention>();
			for (Entity entity : chains) {
				for (int i = 0; i < entity.mentions.size(); i++) {
					Mention m2 = entity.mentions.get(i);
					if (m2.end != -1) {
						continue;
					}
					for (int j = 0; j < i; j++) {
						Mention m1 = entity.mentions.get(j);
						if (m1.end != -1) {
							zeros.add(m2);
							break;
						}
					}
				}
			}
			return zeros;
		}
	
		public static ArrayList<Mention> getZeros(ArrayList<Entity> chains) {
			ArrayList<Mention> zeros = new ArrayList<Mention>();
			for (Entity entity : chains) {
				for (int i = 0; i < entity.mentions.size(); i++) {
					Mention m2 = entity.mentions.get(i);
					if (m2.end == -1) {
						zeros.add(m2);
					}
				}
			}
			return zeros;
		}
	
		public static void setPronounAttri(Mention m, CoNLLPart part) {
			if (m.isAZP) {
				ArrayList<Mention> corefs = new ArrayList<Mention>();
				Mention represent = null;
				for (Mention t : m.entity.mentions) {
					if ((!t.equals(m)) && t.end != -1) {
						corefs.add(t);
					}
					if (EMUtil.pronouns.contains(t.extent)) {
						represent = t;
					}
				}
				if (represent != null) {
					setPronounAttri(represent, part);
					m.number = represent.number;
					m.gender = represent.gender;
	
					if (part.getWord(m.start).speaker.equals(part
							.getWord(represent.start).speaker)) {
						m.person = represent.person;
					} else if (!part.getWord(m.start).speaker.equals(part
							.getWord(represent.start).speaker)) {
						if (represent.person.equals(Person.first)) {
							m.person = Person.second;
						} else if (represent.person.equals(Person.second)) {
							m.person = Person.first;
						} else {
							m.person = Person.third;
						}
					}
					m.animacy = represent.animacy;
				} else {
					int[] numbers = new int[Number.values().length];
					int[] genders = new int[Gender.values().length];
					int[] persons = new int[Person.values().length];
					int[] animacys = new int[Animacy.values().length];
					for (Mention t : corefs) {
						// TODO
						t.animacy = EMUtil.getAntAnimacy(t);
						animacys[t.animacy.ordinal()]++;
	
						t.gender = EMUtil.getAntGender(t);
						genders[t.gender.ordinal()]++;
						t.number = EMUtil.getAntNumber(t);
						numbers[t.number.ordinal()]++;
	
						t.person = EMUtil.getAntPerson(t.head);
						if (part.getWord(m.start).speaker.equals(part
								.getWord(t.start).speaker)) {
							persons[t.person.ordinal()]++;
						} else {
							if (t.person == Person.first) {
								persons[Person.second.ordinal()]++;
							} else if (t.person.equals(Person.second)) {
								persons[Person.first.ordinal()]++;
							} else {
								persons[Person.third.ordinal()]++;
							}
						}
					}
	
					m.number = Number.single;
					int max = 0;
					for (int i = 0; i < numbers.length; i++) {
						if (numbers[i] > max) {
							max = numbers[i];
							m.number = Number.values()[i];
						}
					}
	
					m.gender = Gender.male;
					max = 0;
					for (int i = 0; i < genders.length; i++) {
						if (genders[i] > max) {
							max = genders[i];
							m.gender = Gender.values()[i];
						}
					}
	
					m.person = Person.third;
					max = 0;
					for (int i = 0; i < persons.length; i++) {
						if (persons[i] > max) {
							max = persons[i];
							m.person = Person.values()[i];
						}
					}
	
					m.animacy = Animacy.animate;
					max = 0;
					for (int i = 0; i < animacys.length; i++) {
						if (animacys[i] > max) {
							max = animacys[i];
							m.animacy = Animacy.values()[i];
						}
					}
				}
	
				// find most reprsentive
				// for(Mention m)
				return;
			}
	
			// assign number, gender, person, animacy
			if (EMUtil.singles.contains(m.head)) {
				m.number = EMUtil.Number.single;
			} else if (EMUtil.plurals.contains(m.head)) {
				m.number = EMUtil.Number.plural;
			} else {
				Common.bangErrorPOS("");
			}
	
			if (EMUtil.males.contains(m.head)) {
				m.gender = EMUtil.Gender.male;
			} else if (EMUtil.females.contains(m.head)) {
				m.gender = EMUtil.Gender.female;
			} else if (EMUtil.neuters.contains(m.head)) {
				m.gender = EMUtil.Gender.neuter;
			} else {
				Common.bangErrorPOS(m.head);
			}
	
			if (EMUtil.firsts.contains(m.head)) {
				m.person = EMUtil.Person.first;
			} else if (EMUtil.seconds.contains(m.head)) {
				m.person = EMUtil.Person.second;
			} else if (EMUtil.thirds.contains(m.head)) {
				m.person = EMUtil.Person.third;
			} else {
				Common.bangErrorPOS(m.head);
			}
	
			if (EMUtil.animates.contains(m.head)) {
				m.animacy = EMUtil.Animacy.animate;
			} else if (EMUtil.unanimates.contains(m.head)) {
				m.animacy = EMUtil.Animacy.unanimate;
			} else {
				Common.bangErrorPOS(m.head);
			}
		}
	
		public static void assignVNode(Mention zero, CoNLLPart part) {
			MyTreeNode V = null;
			zero.sentenceID = part.getWord(zero.start).sentence.getSentenceIdx();
			CoNLLSentence s = part.getCoNLLSentences().get(zero.sentenceID);
			MyTreeNode root = s.syntaxTree.root;
			CoNLLWord word = part.getWord(zero.start);
			MyTreeNode leaf = root.getLeaves().get(word.indexInSentence);
	
			for (MyTreeNode node : leaf.getAncestors()) {
				if (node.value.toLowerCase().startsWith("vp")
						&& node.getLeaves().get(0) == leaf) {
					V = node;
				}
			}
	
			if (V == null) {
				for (MyTreeNode node : leaf.getAncestors()) {
					if (node.value.startsWith("DFL")
							&& node.getLeaves().get(0) == leaf) {
						V = node;
					}
				}
			}
	
			if (V == null) {
				int offset = 1;
				while (true) {
					word = part.getWord(zero.start + (offset++));
					leaf = root.getLeaves().get(word.indexInSentence);
					for (MyTreeNode node : leaf.getAncestors()) {
						if (node.value.toLowerCase().startsWith("vp")
								&& node.getLeaves().get(0) == leaf) {
							V = node;
						}
					}
					if (V != null) {
						break;
					}
					if (zero.start + offset == part.getWordCount()) {
						break;
					}
				}
			}
	
			if (V == null) {
				leaf = root.getLeaves().get(
						part.getWord(zero.start).indexInSentence);
				for (MyTreeNode node : leaf.getAncestors()) {
					if (node.value.startsWith("NP")
							&& node.getLeaves().get(0) == leaf) {
						V = node;
					}
				}
			}
			zero.V = V;
		}
	
		public static Number getAntNumber(String str) {
			boolean plura = false;
	
			if (str.contains("和")) {
				plura = true;
			}
			if (str.contains("些")) {
				plura = true;
			}
			if (str.contains("多")) {
				plura = true;
			}
	
			if (str.endsWith("们")) {
				plura = true;
			}
	
			if (plura) {
				return Number.plural;
			} else {
				return Number.single;
			}
		}
	
		public static Number getAntNumber(Mention mention) {
			MyTreeNode np = mention.NP;
			boolean plura = false;
			for (MyTreeNode leaf : np.getLeaves()) {
				if (leaf.parent.value.equals("CD") && !leaf.value.equals("一")) {
					plura = true;
				}
				if (leaf.value.equals("和")) {
					plura = true;
				}
				if (leaf.value.contains("些")) {
					plura = true;
				}
				if (leaf.value.contains("多")) {
					plura = true;
				}
			}
			if (mention.extent.endsWith("们")) {
				plura = true;
			}
	
			if (plura) {
				mention.number = Number.plural;
				return Number.plural;
			} else {
				mention.number = Number.single;
				return Number.single;
			}
		}
	
		public static void assignNE(ArrayList<Mention> mentions,
				ArrayList<Element> elements) {
			for (Mention mention : mentions) {
				int end = mention.end;
				for (Element element : elements) {
					if (element.start <= end && end <= element.end) {
						// if (headStart == element.end) {
						mention.NE = element.content;
						// System.out.println(mention.extent + " : " + mention.NE);
					}
				}
			}
		}
	
		public static double getPrior(String key, HashMap<String, Double> map,
				double overall, int space) {
			double denominator = overall + space;
			double numerator = 1;
			if (map.containsKey(key)) {
				numerator += map.get(key);
			}
			return numerator / denominator;
		}
	
		public static Mention formPhrase(MyTreeNode treeNode, CoNLLSentence sentence) {
			ArrayList<MyTreeNode> leaves = treeNode.getLeaves();
	
			int startIdx = leaves.get(0).leafIdx;
			int endIdx = leaves.get(leaves.size() - 1).leafIdx;
			int start = sentence.getWord(startIdx).index;
			int end = sentence.getWord(endIdx).index;
			StringBuilder sb = new StringBuilder();
			for (int i = startIdx; i <= endIdx; i++) {
				sb.append(sentence.getWord(i).word).append(" ");
			}
			Mention em = new Mention();
			em.start = start;
			em.end = end;
			em.extent = sb.toString().trim();
	
			em.startInS = startIdx;
			em.endInS = endIdx;
			em.sentenceID = sentence.getSentenceIdx();
			em.s = sentence;
	
			MyTreeNode head = treeNode.getHeadLeaf();
			// head = treeNode.getLeaves().get(treeNode.getLeaves().size());
			em.headID = sentence.getWord(head.leafIdx).index;
			em.headInS = head.leafIdx;
			em.head = head.value;
			em.NP = treeNode;
	
			// em.headInS = em.endInS;
			// em.head = sentence.getWord(em.headInS).word;
	
			if (em.head.equals(",")) {
				sentence.syntaxTree.root.setAllMark(true);
				// Common.bangErrorPOS(sentence.syntaxTree.root.getPlainText(true));
			}
	
			if (head.parent.value.equals("NR") || head.parent.value.equals("NNP")
					|| head.parent.value.equals("NNPS")) {
				em.mType = EMUtil.MentionType.proper;
			} else if (head.parent.value.equals("PN")
					|| head.parent.value.equals("PRP")
					|| head.parent.value.equals("PRP$")) {
				em.mType = EMUtil.MentionType.pronoun;
			} else if (head.parent.value.equals("NN")
					|| head.parent.value.equals("NNS")) {
				em.mType = EMUtil.MentionType.common;
			} else {
				em.mType = EMUtil.MentionType.tmporal;
			}
			// check subject or object
			boolean subject = false;
	
			if (treeNode.parent == null) {
				em.gram = EMUtil.Grammatic.other;
			} else {
				for (int i = treeNode.childIndex + 1; i < treeNode.parent.children
						.size(); i++) {
					MyTreeNode sibling = treeNode.parent.children.get(i);
					if (sibling.value.equals("VP")) {
						subject = true;
						em.V = sibling;
						break;
					}
				}
				if (subject) {
					em.gram = EMUtil.Grammatic.subject;
				} else {
					boolean object = false;
					if (treeNode.parent.value.equals("VP")) {
						for (int i = 0; i < treeNode.childIndex; i++) {
							MyTreeNode sibling = treeNode.parent.children.get(i);
							if (sibling.value.startsWith("V")) {
								object = true;
								break;
							}
						}
					}
					if (object) {
						em.gram = EMUtil.Grammatic.object;
					} else {
						em.gram = EMUtil.Grammatic.other;
					}
				}
			}
	
			CoNLLSentence s = em.s;
			// changeStr(em);
			return em;
		}
	
		public static void changeStr(Mention em) {
			if (em.extent.equals("这些")) {
				em.extent = "它们";
				em.head = "它们";
			}
	
			if (em.extent.equals("这") || em.extent.equals("那")
					|| em.extent.equals("这个")) {
				em.extent = "它";
				em.head = "它";
			}
	
			if (em.extent.equals("您")) {
				em.extent = "你";
				em.head = "你";
			}
	
			if (em.extent.equals("双方")) {
				em.extent = "他们";
				em.head = "他们";
			}
		}
	
		public static String getFirstVerb(MyTreeNode vp) {
			ArrayList<MyTreeNode> leaves = vp.getLeaves();
			for (MyTreeNode leaf : leaves) {
				if (leaf.parent.value.startsWith("V")) {
					return leaf.value;
				}
			}
			return "";
		}
	
		public static ArrayList<Mention> extractMention(CoNLLPart part) {
			ArrayList<Mention> ms = new ArrayList<Mention>();
			for (CoNLLSentence s : part.getCoNLLSentences()) {
				ms.addAll(extractMention(s));
			}
			return ms;
		}
	
		public static ArrayList<Mention> extractMention(CoNLLSentence sentence) {
			ArrayList<Mention> nounPhrases = new ArrayList<Mention>();
			MyTree tree = sentence.getSyntaxTree();
			MyTreeNode root = tree.root;
			ArrayList<MyTreeNode> frontie = new ArrayList<MyTreeNode>();
			frontie.add(root);
			while (frontie.size() > 0) {
				MyTreeNode tn = frontie.remove(0);
				if (tn.value.toUpperCase().startsWith("NP")) {
					Mention element = formPhrase(tn, sentence);
					if (element != null) {
						nounPhrases.add(element);
					}
				}
				ArrayList<MyTreeNode> tns = tn.children;
				frontie.addAll(tns);
			}
	
			// remove inner NP
			ArrayList<Mention> removes = new ArrayList<Mention>();
			for (Mention s : nounPhrases) {
				for (Mention l : nounPhrases) {
					if (s.headInS == l.headInS && l.end - l.start > s.end - s.start) {
						removes.add(s);
					}
				}
			}
			nounPhrases.removeAll(removes);
			Collections.sort(nounPhrases);
			return nounPhrases;
		}
	
		// overall should be added 1
		public static int getBucket(int current, int overall, int buckets) {
			double perBucket = (double) overall / (double) buckets;
			int bucket = (int) (current * 1.0 / perBucket);
	
			if (bucket > 4) {
				Common.bangErrorPOS(current + " " + overall);
			}
			return bucket;
		}
	
		// public static Context buildContext_deprecate(Mention ant, Mention
		// pronoun,
		// CoNLLPart part) {
		// int antID = ant.end;
		// int pronounID = pronoun.start;
		//
		// int antSID = part.getWord(antID).sentence.getSentenceIdx();
		// int proSID = part.getWord(pronounID).sentence.getSentenceIdx();
		//
		// short senDis = (short) (proSID - antSID);
		//
		// short antPos = 0;
		// if (senDis == 0) {
		// antPos = (short) (pronounID - antID);
		// antPos = (short) getBucket(antPos,
		// part.getWord(pronounID).indexInSentence, 6);
		// } else {
		// antPos = (short) part.getWord(antID).indexInSentence;
		// antPos = (short) getBucket(antPos,
		// part.getWord(antID).sentence.words.size(), 6);
		// }
		//
		// short proPos = (short) getBucket(
		// part.getWord(pronounID).indexInSentence,
		// part.getWord(antID).sentence.words.size(), 4);
		//
		// short antSynactic = (short) Grammatic.subject.ordinal();
		//
		// short antType = (short) ant.mType.ordinal();
		//
		// return Context.getContext(senDis, antPos, antSynactic, proPos, antType);
		// }
	
		// public static Context buildContext(Mention ant, Mention pronoun,
		// int totalDis, int precedDis) {
		// short antSenPos = (short) (pronoun.sentenceID - ant.sentenceID);
		// int dis = 0;
		// if (antSenPos == 0) {
		// dis += pronoun.headInS - ant.headInS;
		// } else {
		// dis += pronoun.headInS;
		// dis += ant.headInS;
		// dis += precedDis;
		// }
		//
		// short antHeadPos = (short) ((dis) / ((totalDis + 1) / 6.0));
		// // short antGram = (short)ant.gram.ordinal();
		// short antGram = (short)Grammatic.subject.ordinal();
		// // short proPos = (short) (pronoun.headInS / (pronoun.s.words.size() /
		// 4.0));
		// short proPos = 1;
		// short antType = (short) ant.mType.ordinal();
		//
		// return Context.getContext(antSenPos, antHeadPos, antGram, proPos,
		// antType);
		// }
	
		public static String getPathString(ArrayList<GraphNode> path) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < path.size() - 1; i++) {
				GraphNode n = path.get(i);
				if (i != 0) {
					sb.append(n.token).append(" ");
				}
				String dep = n.getEdgeName(path.get(i + 1));
				sb.append(dep).append(" ");
			}
	
			return sb.toString();
		}
	
		public static ArrayList<GraphNode> findPath(GraphNode from, GraphNode to) {
			ArrayList<GraphNode> path = new ArrayList<GraphNode>();
	
			HashSet<GraphNode> visited = new HashSet<GraphNode>();
			ArrayList<GraphNode> fronties = new ArrayList<GraphNode>();
			fronties.add(from);
			if (from == null) {
				return path;
			}
			if (to == null) {
				return path;
			}
	
			if (from != to) {
				loop: while (true) {
					ArrayList<GraphNode> nextLevel = new ArrayList<GraphNode>();
					for (GraphNode node : fronties) {
						if (node == null) {
							Common.bangErrorPOS("Null Dep node");
						}
						for (GraphNode next : node.nexts) {
							if (!visited.contains(next)) {
								next.backNode = node;
								if (next.backNode == next) {
									Common.bangErrorPOS("Self Dep: ");
								}
								if (next == to) {
									break loop;
								}
								nextLevel.add(next);
							}
						}
						visited.add(node);
					}
					fronties = nextLevel;
					if (fronties.size() == 0) {
						// Token t1 = Util.getTkFromDepNode(from, s);
						// Token t2 = Util.getTkFromDepNode(to, s);
						// System.out.println(t1.word + " " + t1.idInSentence);
						// System.out.println(t2.word + " " + t2.idInSentence);
						// System.out.println(s.d.fn);
						// Common.bangErrorPOS("");
						System.out.println("No Path");
						return path;
					}
				}
			}
	
			GraphNode tmp = to;
			while (true) {
				path.add(0, tmp);
				if (tmp == from) {
					break;
				}
				tmp = tmp.backNode;
				// System.out.println(fronties.size() + " " + (tmp==tmp.backNode));
			}
			return path;
		}
	
		public static String measureStr = "把  般  班  瓣  磅  帮  包  辈  杯  本  笔  柄  拨  部  餐  册  层  场  场  成  尺  重  出  处  串  幢  床  次  簇  撮  打  袋  代  担  档  道  滴  点  顶  栋  堵  度  端  段  对  堆  队  顿  吨  朵  发  番  方  分  份  封  峰  付  幅  副  服  杆  个  根  公尺  公分  公斤  公里  公顷  公升  股  挂  管  行  盒  户  壶  伙  记  级  剂  架  家  加仑  件  间  绞  角  届  截  节  斤  茎  局  具  句  居  卷  圈  卡  客  棵  颗  克  孔  口  块  捆  类  里  粒  辆  两  列  立方英尺  立方米  领  缕  轮  摞  毛  枚  门  米  面  秒  名  亩  幕  排  派  盘  泡  喷  盆  匹  批  片  篇  撇  瓶  平方公里  期  起  爿  千克  千瓦  顷  曲  圈  群  人工  扇  勺  身  升  手  首  束  双  丝  艘  所  台  摊  滩  趟  堂  套  天  条  挑  贴  挺  筒  桶  通  头  团  坨  丸  碗  位  尾  味  窝  席  线  箱  项  些  牙  眼  样  页  英亩  员  元  则  盏  丈  章  张  阵  支  枝  只  种  轴  株  幢  桩  桌  宗  组  尊  座  ";
	
		public static HashSet<String> measures = new HashSet<String>(
				Arrays.asList(measureStr.trim().split("\\s+")));
	
		public static String definitePersonStr = "帮 辈 拨 代 队 户 位 员 名";
		public static HashSet<String> definitePersonMeasures = new HashSet<String>(
				Arrays.asList(definitePersonStr.trim().split("\\s+")));
	
		public static String ge = "个";
	
		static HashMap<String, HashMap<String, Integer>> peopleness;
	
		public static Animacy getAntAnimacy_(String head) {
			if (peopleness == null) {
				loadMeassure();
			}
			if (pronouns.contains(head)) {
				return getAnimacy(head);
			}
	
			Animacy ret;
			if (head.endsWith("人") || head.equals("自己")) {
				ret = Animacy.animate;
			} else if (peopleness.containsKey(head)) {
				HashMap<String, Integer> subMap = peopleness.get(head);
				int anim = 0;
				int unanim = 0;
	
				for (String definitePerson : definitePersonMeasures) {
					if (subMap.containsKey(definitePerson)) {
						anim += subMap.get(definitePerson);
					}
				}
	
				for (String key : subMap.keySet()) {
					if (!definitePersonMeasures.contains(key) && !key.equals(ge)) {
						unanim += subMap.get(key);
					}
				}
				int geCount = 0;
				if (subMap.containsKey(ge)) {
					geCount += subMap.get(ge);
				}
				if (anim > unanim) {
					ret = Animacy.animate;
				}
				// else if (anim != 0 && anim + geCount > unanim && anim>.5*unanim)
				// {
				// ret = Animacy.animate;
				//
				// }
				else {
					ret = Animacy.unanimate;
				}
				// System.out.println(head + " " + an.name());
			} else {
				// if (location.contains(head.substring(head.length() - 1))) {
				// ret = Animacy.unanimate;
				// } else if (mention.mType == MentionType.proper) {
				// ret = Animacy.animate;
				// // System.out.println(mention.extent);
				// } else {
				ret = Animacy.unknown;
				// ret = Animacy.unanimate;
				// System.out.println(head + " " + mention.NE);
				missed.add(head);
				// }
			}
			// System.out.println(head + " " + ret.name() + " " + mention.NE);
			return ret;
		}
	
		public static Animacy getAntAnimacy(String head) {
			if (peopleness == null) {
				loadMeassure();
			}
			if (pronouns.contains(head)) {
				return getAnimacy(head);
			}
	
			// if (!peopleness.containsKey(head)
			// && mention.mType == MentionType.common) {
			// head = head.substring(head.length() - 1);
			// }
			//
			Animacy ret;
			if (head.endsWith("人") || head.equals("自己")) {
				ret = Animacy.animate;
			}
			// else if (mention.mType == MentionType.tmporal) {
			// ret = Animacy.unanimate;
			// } else if (!mention.NE.equalsIgnoreCase("OTHER")) {
			// if (mention.NE.equals("PERSON")) {
			// ret = Animacy.animate;
			// } else {
			// ret = Animacy.unanimate;
			// }
			// }
			else if (peopleness.containsKey(head)) {
				HashMap<String, Integer> subMap = peopleness.get(head);
				int anim = 0;
				int unanim = 0;
	
				for (String definitePerson : definitePersonMeasures) {
					if (subMap.containsKey(definitePerson)) {
						anim += subMap.get(definitePerson);
					}
				}
	
				for (String key : subMap.keySet()) {
					if (!definitePersonMeasures.contains(key) && !key.equals(ge)) {
						unanim += subMap.get(key);
					}
				}
				int geCount = 0;
				if (subMap.containsKey(ge)) {
					geCount += subMap.get(ge);
				}
				if (anim > unanim) {
					ret = Animacy.animate;
				}
				// else if (anim != 0 && anim + geCount > unanim && anim>.5*unanim)
				// {
				// ret = Animacy.animate;
				//
				// }
				else {
					ret = Animacy.unanimate;
				}
				// System.out.println(head + " " + an.name());
			} else {
				// if (location.contains(head.substring(head.length() - 1))) {
				// ret = Animacy.unanimate;
				// } else if (mention.mType == MentionType.proper) {
				// ret = Animacy.animate;
				// // System.out.println(mention.extent);
				// } else {
				ret = Animacy.unknown;
				// ret = Animacy.unanimate;
				// System.out.println(head + " " + mention.NE);
				missed.add(head);
				// }
			}
			// System.out.println(head + " " + ret.name() + " " + mention.NE);
			return ret;
		}
	
		public static Animacy getAntAnimacy(Mention mention) {
			if (peopleness == null) {
				loadMeassure();
			}
			String head = mention.head;
			if (pronouns.contains(head)) {
				return getAnimacy(head);
			}
	
			if (!peopleness.containsKey(head)
					&& mention.mType == MentionType.common) {
				head = head.substring(head.length() - 1);
			}
	
			Animacy ret;
			if (head.endsWith("人") || head.equals("自己")) {
				ret = Animacy.animate;
			} else if (mention.mType == MentionType.tmporal) {
				ret = Animacy.unanimate;
			} else if (!mention.NE.equalsIgnoreCase("OTHER")) {
				if (mention.NE.equals("PERSON")) {
					ret = Animacy.animate;
				} else {
					ret = Animacy.unanimate;
				}
			} else if (peopleness.containsKey(head)) {
				HashMap<String, Integer> subMap = peopleness.get(head);
				int anim = 0;
				int unanim = 0;
	
				for (String definitePerson : definitePersonMeasures) {
					if (subMap.containsKey(definitePerson)) {
						anim += subMap.get(definitePerson);
					}
				}
	
				for (String key : subMap.keySet()) {
					if (!definitePersonMeasures.contains(key) && !key.equals(ge)) {
						unanim += subMap.get(key);
					}
				}
				int geCount = 0;
				if (subMap.containsKey(ge)) {
					geCount += subMap.get(ge);
				}
				if (anim > unanim) {
					ret = Animacy.animate;
				}
				// else if (anim != 0 && anim + geCount > unanim && anim>.5*unanim)
				// {
				// ret = Animacy.animate;
				//
				// }
				else {
					ret = Animacy.unanimate;
				}
				// System.out.println(head + " " + an.name());
			} else {
				// if (location.contains(head.substring(head.length() - 1))) {
				// ret = Animacy.unanimate;
				// } else if (mention.mType == MentionType.proper) {
				// ret = Animacy.animate;
				// // System.out.println(mention.extent);
				// } else {
				ret = Animacy.unknown;
				// ret = Animacy.unanimate;
				// System.out.println(head + " " + mention.NE);
				missed.add(head);
				// }
			}
			// System.out.println(head + " " + ret.name() + " " + mention.NE);
//			if(ret==Animacy.unknown) {
//				ret = Animacy.animate;
//			}
			return ret;
		}
	
		public static HashSet<String> missed = new HashSet<String>();
	
		public static void loadMeassure() {
			peopleness = new HashMap<String, HashMap<String, Integer>>();
			HashMap<String, Integer> MCounts = new HashMap<String, Integer>();
			ArrayList<String> lines = Common.getLines("animacy.giga");
			for (String line : lines) {
				String tks[] = line.split("\\s+");
				String noun = tks[0];
				String M = tks[1];
				int count = Integer.parseInt(tks[2]);
				HashMap<String, Integer> subMap = peopleness.get(noun);
				if (subMap == null) {
					subMap = new HashMap<String, Integer>();
					peopleness.put(noun, subMap);
				}
				subMap.put(M, count);
	
				Integer mc = MCounts.get(M);
				if (mc == null) {
					MCounts.put(M, count);
				} else {
					MCounts.put(M, mc.intValue() + count);
				}
			}
			// HashSet<Integer> set = new HashSet<Integer>();
			// set.addAll(MCounts.values());
			//
			// HashSet<String> frequent = new HashSet<String>();
			//
			// for (String M : MCounts.keySet()) {
			// System.out.println(M + MCounts.get(M));
			// if (MCounts.get(M) > 500) {
			// frequent.add(M);
			// }
			// }
			// ArrayList<Integer> arr = new ArrayList<Integer>();
			// arr.addAll(set);
			// Collections.sort(arr);
			// Collections.reverse(arr);
			//
			// System.out.println("#" + arr.get(100));
			// System.out.println("@" + frequent.size());
			// System.out.println(frequent);
		}
	
		static HashMap<String, HashMap<String, Integer>> genderStat;
	
		public static void loadGender() {
			if (peopleness == null) {
				loadMeassure();
			}
			genderStat = new HashMap<String, HashMap<String, Integer>>();
			ArrayList<String> lines = Common.getLines("collectStats.giga.bak");
			for (String line : lines) {
				String tks[] = line.split("\\s+");
				String noun = tks[0];
				String M = tks[1];
				int count = Integer.parseInt(tks[2]);
				HashMap<String, Integer> subMap = peopleness.get(noun);
				if (subMap == null) {
					subMap = new HashMap<String, Integer>();
					peopleness.put(noun, subMap);
				}
				subMap.put(M, count);
			}
		}
	
		public static Gender getAntGender(String head) {
			if (genderStat == null) {
				loadGender();
			}
			if (pronouns.contains(head)) {
				return getGender(head);
			}
			Animacy anim = getAntAnimacy(head);
			if (anim == Animacy.unanimate || anim == Animacy.unknown) {
				return Gender.neuter;
			}
	
			HashMap<String, Integer> subMap = genderStat.get(head);
			int male = 0;
			int female = 0;
			if (subMap != null) {
				for (String malePronoun : EMUtil.males) {
					if (subMap.containsKey(malePronoun)) {
						male += subMap.get(malePronoun);
					}
				}
				for (String femalePronoun : EMUtil.females) {
					if (subMap.containsKey(femalePronoun)) {
						female += subMap.get(femalePronoun);
					}
				}
			}
			if (female > male) {
				// System.out.println(head + " : FEMALE");
				return Gender.female;
			} else {
				// System.out.println(head + " : MALE " + m.animacy + " " + m.NE);
				return Gender.male;
			}
		}
	
		public static Gender getAntGender(Mention m) {
			if (genderStat == null) {
				loadGender();
			}
			String head = m.head;
			if (pronouns.contains(head)) {
				return getGender(head);
			}
			Animacy anim = getAntAnimacy(m);
			if (anim == Animacy.unanimate || anim == Animacy.unknown) {
				return Gender.neuter;
			}
	
			HashMap<String, Integer> subMap = genderStat.get(head);
			int male = 0;
			int female = 0;
			if (subMap != null) {
				for (String malePronoun : EMUtil.males) {
					if (subMap.containsKey(malePronoun)) {
						male += subMap.get(malePronoun);
					}
				}
				for (String femalePronoun : EMUtil.females) {
					if (subMap.containsKey(femalePronoun)) {
						female += subMap.get(femalePronoun);
					}
				}
			}
			if (female > male) {
				// System.out.println(head + " : FEMALE");
				return Gender.female;
			} else {
				// System.out.println(head + " : MALE " + m.animacy + " " + m.NE);
				return Gender.male;
			}
		}
	
		public static HashMap<String, ArrayList<Element>> predictNEs;
	
		public static HashMap<String, String> NEMap;
	
		public static void loadPredictNE(String folder, String mode) {
			if (predictNEs == null) {
				NEMap = new HashMap<String, String>();
				predictNEs = new HashMap<String, ArrayList<Element>>();
				String fn = "chinese_" + folder + ".neresult";
				ArrayList<String> lines = Common.getLines(fn);
				for (int i = 0; i < lines.size(); i++) {
					String line = lines.get(i);
					if (line.isEmpty()) {
						continue;
					}
					String tokens[] = line.split("\\s+");
					int length = tokens.length;
					String label = tokens[length - 1];
					int wordID = Integer.valueOf(tokens[length - 3]);
					int partID = Integer.valueOf(tokens[length - 4]);
					String docID = tokens[length - 5];
					String key = docID + "_" + partID;
					if (label.startsWith("B")) {
						String content = label.substring(2);
						int k = i + 1;
						while (!lines.get(k).isEmpty()
								&& lines.get(k).trim().split("\\s+")[length - 1]
										.startsWith("I")) {
							k++;
						}
						int start = wordID;
						int end = Integer
								.valueOf(lines.get(k - 1).split("\\s+")[length - 3]);
						Element element = new Element();
						element.start = start;
						element.end = end;
						element.content = content;
	
						StringBuilder sb = new StringBuilder();
						for (int m = i; m <= k - 1; m++) {
							sb.append(lines.get(m).split("\\s+")[0]);
						}
	
						EMUtil.NEMap.put(sb.toString(), content);
						// System.out.println(sb.toString() + " " + content + " " +
						// i + " " + (k-1));
						if (predictNEs.containsKey(key)) {
							predictNEs.get(key).add(element);
						} else {
							ArrayList<Element> ems = new ArrayList<Element>();
							ems.add(element);
							predictNEs.put(key, ems);
						}
					}
				}
			}
		}
	
		public static void pruneChMentions(ArrayList<Mention> mentions,
				CoNLLPart part) {
			ArrayList<Mention> removes = new ArrayList<Mention>();
			Collections.sort(mentions);
			ArrayList<Mention> copyMentions = new ArrayList<Mention>(
					mentions.size());
			copyMentions.addAll(mentions);
	
			for (int i = 0; i < mentions.size(); i++) {
				Mention em = mentions.get(i);
				for (int j = 0; j < copyMentions.size(); j++) {
					Mention em2 = copyMentions.get(j);
					if (em.end == em2.end
							&& (em.end - em.start < em2.end - em2.start)) {
						if (!part.label.contains("nw/")) {
							// if (em.start > 0 && part.getWord(em.start -
							// 1).posTag.equalsIgnoreCase("CC")) {
							// continue;
							// }
							removes.add(em);
							break;
						}
					}
				}
			}
			mentions.removeAll(removes);
			removes.clear();
	
			for (int i = 0; i < mentions.size(); i++) {
				Mention mention = mentions.get(i);
				if (mention.NE.equalsIgnoreCase("QUANTITY")
						|| mention.NE.equalsIgnoreCase("CARDINAL")
						|| mention.NE.equalsIgnoreCase("PERCENT")
						|| mention.NE.equalsIgnoreCase("MONEY")) {
					removes.add(mention);
					continue;
				}
	
				if (mention.extent.equalsIgnoreCase("我")
						&& (mention.end + 2) < part.getWordCount()
						&& part.getWord(mention.end + 1).word.equals("啊")
						&& part.getWord(mention.end + 2).word.equals("，")) {
					removes.add(mention);
					continue;
				}
	
				if (EMUtil.removeChars.contains(mention.head)) {
					removes.add(mention);
					continue;
				}
	
				// 没 问题
				if (mention.extent.equalsIgnoreCase("问题") && mention.start > 0
						&& part.getWord(mention.start - 1).word.equals("没")) {
					removes.add(mention);
					continue;
				}
	
				// 你 知道
				if (mention.extent.equalsIgnoreCase("你") && mention.start > 0
						&& part.getWord(mention.start + 1).word.equals("知道")) {
					removes.add(mention);
					continue;
				}
	
				//
				if (mention.extent.contains("什么") || mention.extent.contains("多少")) {
					removes.add(mention);
					continue;
				}
				String lastWord = part.getWord(mention.end).word;
				if (mention.extent.endsWith("的")
						|| (mention.extent.endsWith("人")
								&& mention.start == mention.end && countries
									.contains(lastWord.substring(0,
											lastWord.length() - 1)))) {
					removes.add(mention);
					continue;
				}
				// ｑｕｏｔ
				if (removeWords.contains(mention.extent)) {
					removes.add(mention);
					continue;
				}
			}
			for (Mention remove : removes) {
				mentions.remove(remove);
			}
			HashSet<Mention> mentionsHash = new HashSet<Mention>();
			mentionsHash.addAll(mentions);
			mentions.clear();
			mentions.addAll(mentionsHash);
		}
	
		public final static HashSet<String> countries = Common
				.readFile2Set("country2");
	
		public final static Set<String> removeWords = new HashSet<String>(
				Arrays.asList(new String[] { "_", "ｑｕｏｔ", "人", "时候", "问题", "情况",
						"未来", "战争", "可能" }));
	
		public static String getPredicateNode(MyTreeNode vp) {
			ArrayList<MyTreeNode> leaves = vp.getLeaves();
			for (MyTreeNode leaf : leaves) {
				if (leaf.parent.value.startsWith("V")
				// && !leaf.value.equals("会") && !leaf.value.equals("独立")
				// && !leaf.value.equals("可以")
				) {
					return leaf.value;
				}
			}
			return null;
		}
	
		public static String getObjectNP(MyTreeNode vp) {
			ArrayList<MyTreeNode> leaves = vp.getLeaves();
			for (MyTreeNode leaf : leaves) {
				if (leaf.parent.value.startsWith("V")) {
					ArrayList<MyTreeNode> possibleNPs = leaf.parent
							.getRightSisters();
					for (MyTreeNode tmp : possibleNPs) {
						if (tmp.value.startsWith("NP")
								|| tmp.value.startsWith("QP")) {
							return tmp.getLeaves().get(tmp.getLeaves().size() - 1).value;
						}
					}
				}
			}
			return null;
		}
	
		public static String getObjectNP2(MyTreeNode vp) {
			while (true) {
				boolean haveVP = false;
				for (MyTreeNode child : vp.children) {
					if (child.value.equalsIgnoreCase("VP")) {
						haveVP = true;
						vp = child;
						break;
					}
				}
				if (!haveVP) {
					break;
				}
			}
			// System.out.println(tmp.children.get(0).value);
			ArrayList<MyTreeNode> possibleNPs = vp.children;
			for (MyTreeNode tm : possibleNPs) {
				if (tm.value.startsWith("NP") || tm.value.startsWith("QP")) {
					return tm.getLeaves().get(tm.getLeaves().size() - 1).value;
				}
			}
			return "";
		}
	
		public static Datum<String, String> svmlightToStanford(
				ArrayList<String> feas, String label) {
			return new BasicDatum<String, String>(feas, label);
		}
	
		protected static Datum<String, String> svmlightToStanford(String svmlight) {
			String tks[] = svmlight.split("\\s+");
			String label = tks[0];
			List<String> features = new ArrayList<String>();
			for (int i = 1; i < tks.length; i++) {
				int k = tks[i].indexOf(":");
				String idx = tks[i].substring(0, k);
				int val = Integer.parseInt(tks[i].substring(k + 1));
				if (val != 1) {
					Common.bangErrorPOS("Binary expected!");
				}
				features.add(idx);
			}
			return new BasicDatum<String, String>(features, label);
		}
	
		public static HashMap<String, Integer> formChainMap(
				ArrayList<Entity> entities) {
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			for (int i = 0; i < entities.size(); i++) {
				for (Mention m : entities.get(i).mentions) {
					map.put(m.toName(), i);
				}
			}
			return map;
		}
	
		public static HashMap<String, ArrayList<Mention>> formChainMap2(
				ArrayList<Entity> entities) {
			HashMap<String, ArrayList<Mention>> map = new HashMap<String, ArrayList<Mention>>();
			for (int i = 0; i < entities.size(); i++) {
				for (Mention m : entities.get(i).mentions) {
					map.put(m.toName(), entities.get(i).mentions);
				}
			}
			return map;
		}
	
		public static ArrayList<Mention> getInBetweenMention(
				ArrayList<Mention> mentions, int start, int end) {
			ArrayList<Mention> zeros = new ArrayList<Mention>();
			for (Mention m : mentions) {
				if (m.start >= start && m.start <= end
						&& (m.end == -1 || (m.end >= start && m.end <= end))) {
					zeros.add(m);
				}
			}
			return zeros;
		}
	
		public static ArrayList<ArrayList<CoNLLWord>> split(CoNLLSentence s,
				ArrayList<Mention> zeros) {
			ArrayList<ArrayList<CoNLLWord>> groups = new ArrayList<ArrayList<CoNLLWord>>();
	
			ArrayList<Integer> spliter = new ArrayList<Integer>();
			Collections.sort(zeros);
			spliter.add(0);
			for (int i = 0; i < zeros.size() - 1; i++) {
				Mention z1 = zeros.get(i);
				Mention z2 = zeros.get(i + 1);
				int split = -1;
				for (int p = z1.start; p <= z2.start; p++) {
					String word = s.part.getWord(p).word;
					if (word.equals(".") || word.equals("。") || word.equals("？")
							|| word.equals("?")) {
						split = s.part.getWord(p).indexInSentence + 1;
						break;
					}
					if (split == -1
							&& (word.equals(",") || word.equals("，")
									|| word.equals("，") || word.equals("、"))) {
						split = s.part.getWord(p).indexInSentence + 1;
					}
				}
				if (split != -1) {
					spliter.add(split);
				}
			}
			spliter.add(s.getWords().size());
	
			for (int i = 0; i < spliter.size() - 1; i++) {
				ArrayList<CoNLLWord> group = new ArrayList<CoNLLWord>();
				group.addAll(s.getWords().subList(spliter.get(i),
						spliter.get(i + 1)));
				groups.add(group);
			}
			return groups;
		}
	
		public static ArrayList<CoNLLWord> addZero(ArrayList<CoNLLWord> list,
				Mention z, String pronoun) {
			ArrayList<CoNLLWord> newList = new ArrayList<CoNLLWord>(list);
			CoNLLWord zp = new CoNLLWord();
			zp.word = pronoun;
			for (int i = 0; i < newList.size(); i++) {
				if (newList.get(i).index == z.start) {
					zp.index = z.start;
					zp.indexInSentence = newList.get(i).indexInSentence;
					zp.isZeroWord = true;
					newList.add(i, zp);
					break;
				}
			}
			return newList;
		}
	
		public static String listToString(ArrayList<CoNLLWord> tks) {
			StringBuilder sb = new StringBuilder();
			for (CoNLLWord tk : tks) {
				sb.append(tk.word).append(" ");
			}
			return sb.toString().trim();
		}
	
		public static ArrayList<ArrayList<CoNLLWord>> getAllPossibleFill(
				ArrayList<CoNLLWord> group, ArrayList<Mention> zeroInGroup) {
			ArrayList<ArrayList<CoNLLWord>> combinations = new ArrayList<ArrayList<CoNLLWord>>();
			combinations.add(group);
	
			for (Mention z : zeroInGroup) {
				ArrayList<ArrayList<CoNLLWord>> tmpCombinations = new ArrayList<ArrayList<CoNLLWord>>();
				for (String op : EMUtil.pronouns) {
					for (ArrayList<CoNLLWord> combination : combinations) {
						ArrayList<CoNLLWord> newList = EMUtil.addZero(combination,
								z, op);
						tmpCombinations.add(newList);
					}
				}
				combinations = tmpCombinations;
			}
			return combinations;
		}
	
		public static ArrayList<Mention> removeDuplcate(
				ArrayList<Mention> anaphorZeros) {
			HashSet<Mention> zeroSet = new HashSet<Mention>(anaphorZeros);
			anaphorZeros = new ArrayList<Mention>(zeroSet);
			Collections.sort(anaphorZeros);
			return anaphorZeros;
		}
	
		public static ArrayList<Mention> getPronouns(ArrayList<Mention> cluster) {
			ArrayList<Mention> pronouns = new ArrayList<Mention>();
			for (Mention m : cluster) {
				if (m.end != -1 && EMUtil.pronouns.contains(m.extent)) {
					pronouns.add(m);
				}
			}
			return pronouns;
		}
	
		public static ArrayList<Mention> getNotPronouns(ArrayList<Mention> cluster) {
			ArrayList<Mention> notPronouns = new ArrayList<Mention>();
			for (Mention m : cluster) {
				if (m.end != -1 && !EMUtil.pronouns.contains(m.extent)) {
					notPronouns.add(m);
				}
			}
			return notPronouns;
		}
	
		public static int[] getWinner(int[] vote) {
			int[] ret = new int[2];
			ret[1] = -1;
			for (int v = 0; v < vote.length; v++) {
				if (vote[v] > ret[0]) {
					ret[0] = vote[v];
					ret[1] = v;
				}
			}
			return ret;
		}
	
		public static Number inferNumber(ArrayList<Mention> cluster) {
			ArrayList<Mention> pronouns = getPronouns(cluster);
			int[] vote = new int[Number.values().length];
			for (Mention p : pronouns) {
				vote[getNumber(p.extent).ordinal()]++;
			}
			int ret[] = getWinner(vote);
			int max = ret[0];
			int win = ret[1];
			if (win != -1) {
				int best = 0;
				for (int v : vote) {
					if (v == max) {
						best++;
					}
				}
				if (best == 1) {
					return Number.values()[win];
				}
			}
			ArrayList<Mention> notPronouns = getNotPronouns(cluster);
			for (Mention m : notPronouns) {
				vote[getAntNumber2(m.extent).ordinal()]++;
			}
			ret = getWinner(vote);
			if (ret[1] == -1) {
				return Number.single;
			} else {
				return Number.values()[ret[1]];
			}
		}
	
		//
		public static Number getAntNumber2(String str) {
			boolean plura = false;
			if (str.contains("部分") || str.contains("两") || str.contains("和")
					|| str.contains("些") || str.contains("多") || str.endsWith("们")
					|| str.contains("大家") || str.contains("四")
					|| str.contains("一点") || str.contains("双") || str.contains("与")
					|| str.contains("位") || str.contains("４") || str.contains("众")
					|| str.contains("万") || str.contains("几")) {
				plura = true;
			}
			if (plura) {
				return Number.plural;
			} else {
				return Number.single;
			}
		}
	
		public static Gender inferGender(ArrayList<Mention> cluster) {
			ArrayList<Mention> pronouns = getPronouns(cluster);
			int[] vote = new int[Gender.values().length - 1];
			for (Mention p : pronouns) {
				vote[getGender(p.extent).ordinal()]++;
			}
			int ret[] = getWinner(vote);
			int max = ret[0];
			int win = ret[1];
			if (win != -1) {
				int best = 0;
				for (int v : vote) {
					if (v == max) {
						best++;
					}
				}
				if (best == 1) {
					return Gender.values()[win];
				}
			}
			ArrayList<Mention> notPronouns = getNotPronouns(cluster);
			for (Mention m : notPronouns) {
				vote[getAntGender2(m.head).ordinal()]++;
			}
			ret = getWinner(vote);
			if (ret[1] == -1) {
				return Gender.male;
			} else {
				return Gender.values()[ret[1]];
			}
		}
	
		public static Gender getAntGender2(String head) {
			if (genderStat == null) {
				loadGender();
			}
			if (pronouns.contains(head)) {
				return getGender(head);
			}
			if (head.equals("母亲") || head.endsWith("女士") || head.endsWith("太太")) {
				return Gender.female;
			}
			if (head.endsWith("嗲")) {
				return Gender.male;
			}
			Animacy anim = getAntAnimacy2(head);
			if (anim == Animacy.unanimate || anim == Animacy.unknown) {
				return Gender.neuter;
			}
			HashMap<String, Integer> subMap = genderStat.get(head);
			int male = 0;
			int female = 0;
			if (subMap != null) {
				for (String malePronoun : EMUtil.males) {
					if (subMap.containsKey(malePronoun)) {
						male += subMap.get(malePronoun);
					}
				}
				for (String femalePronoun : EMUtil.females) {
					if (subMap.containsKey(femalePronoun)) {
						female += subMap.get(femalePronoun);
					}
				}
			}
			if (female > male) {
				// System.out.println(head + " : FEMALE");
				return Gender.female;
			} else {
				// System.out.println(head + " : MALE " + m.animacy + " " + m.NE);
				return Gender.male;
			}
		}
	
		public static Animacy inferAnimacy(ArrayList<Mention> cluster) {
			ArrayList<Mention> pronouns = getPronouns(cluster);
			int[] vote = new int[Animacy.values().length - 1];
			for (Mention p : pronouns) {
				vote[getAnimacy(p.extent).ordinal()]++;
			}
			int ret[] = getWinner(vote);
			int max = ret[0];
			int win = ret[1];
			if (win != -1) {
				int best = 0;
				for (int v : vote) {
					if (v == max) {
						best++;
					}
				}
				if (best == 1) {
					return Animacy.values()[win];
				}
			}
			ArrayList<Mention> notPronouns = getNotPronouns(cluster);
			for (Mention m : notPronouns) {
				if (m.NE.startsWith("PER")) {
					vote[Animacy.animate.ordinal()]++;
				} else {
					Animacy ani = getAntAnimacy2(m.head);
					if (ani != Animacy.unknown) {
						vote[ani.ordinal()]++;
					}
				}
			}
			ret = getWinner(vote);
			if (ret[1] == -1) {
				return Animacy.animate;
			} else {
				return Animacy.values()[ret[1]];
			}
		}
	
		//
		public static Animacy getAntAnimacy2(String head) {
			if (peopleness == null) {
				loadMeassure();
			}
			if (pronouns.contains(head)) {
				return getAnimacy(head);
			}
			Animacy ret;
			if (head.equals("上帝") || head.endsWith("们") || head.contains("总统")) {
				return Animacy.animate;
			}
			if (head.endsWith("人") || head.equals("自己")) {
				ret = Animacy.animate;
			} else if (peopleness.containsKey(head)) {
				HashMap<String, Integer> subMap = peopleness.get(head);
				int anim = 0;
				int unanim = 0;
	
				for (String definitePerson : definitePersonMeasures) {
					if (subMap.containsKey(definitePerson)) {
						anim += subMap.get(definitePerson);
					}
				}
	
				for (String key : subMap.keySet()) {
					if (!definitePersonMeasures.contains(key) && !key.equals(ge)) {
						unanim += subMap.get(key);
					}
				}
				int geCount = 0;
				if (subMap.containsKey(ge)) {
					geCount += subMap.get(ge);
				}
				if (anim > unanim) {
					ret = Animacy.animate;
				}
	
				else {
					ret = Animacy.unanimate;
				}
			} else {
				ret = Animacy.unknown;
				missed.add(head);
			}
			return ret;
		}
	
		public static Person flipPerson(Person p, boolean sameSpeaker, String pro) {
			if (sameSpeaker) {
				return p;
			} else {
				if (p == Person.first && pro.equals("我")) {
					return Person.second;
				} else if (p == Person.second) {
					return Person.first;
				} else {
					return Person.third;
				}
			}
		}
	
		public static Person inferPerson(ArrayList<Mention> cluster, Mention zp,
				CoNLLPart part) {
			ArrayList<Mention> pronouns = getPronouns(cluster);
			int[] vote = new int[Person.values().length];
			for (Mention p : pronouns) {
				boolean sameSpeaker = part.getWord(zp.start).speaker.equals(part
						.getWord(p.start).speaker);
				vote[flipPerson(getPerson(p.extent), sameSpeaker, p.extent)
						.ordinal()]++;
			}
			int ret[] = getWinner(vote);
			int max = ret[0];
			int win = ret[1];
			if (win != -1) {
				int best = 0;
				for (int v : vote) {
					if (v == max) {
						best++;
					}
				}
				if (best == 1) {
					return Person.values()[win];
				}
			}
			ArrayList<Mention> notPronouns = getNotPronouns(cluster);
			for (Mention m : notPronouns) {
				boolean sameSpeaker = part.getWord(zp.start).speaker.equals(part
						.getWord(m.start).speaker);
				vote[flipPerson(getAntPerson(m.extent), sameSpeaker, m.extent)
						.ordinal()]++;
			}
			ret = getWinner(vote);
			if (ret[1] == -1) {
				return Person.third;
			} else {
				return Person.values()[ret[1]];
			}
		}
	
		public static String getOnePronoun(Person per, Number num, Gender gen,
				Animacy ani) {
			if (per == Person.first) {
				if (gen == Gender.male) {
					if (num == Number.single) {
						if (ani == Animacy.animate) {
							return "我";
						} else if (ani == Animacy.unanimate) {
							return "我";
						}
					} else if (num == Number.plural) {
						if (ani == Animacy.animate) {
							return "我们";
						} else if (ani == Animacy.unanimate) {
							return "我们";
						}
					}
				} else if (gen == Gender.female) {
					if (num == Number.single) {
						if (ani == Animacy.animate) {
							return "我";
						} else if (ani == Animacy.unanimate) {
							return "我";
						}
					} else if (num == Number.plural) {
						if (ani == Animacy.animate) {
							return "我们";
						} else if (ani == Animacy.unanimate) {
							return "我们";
						}
					}
				} else if (gen == Gender.neuter) {
					if (num == Number.single) {
						if (ani == Animacy.animate) {
							return "我";
						} else if (ani == Animacy.unanimate) {
							return "我";
						}
					} else if (num == Number.plural) {
						if (ani == Animacy.animate) {
							return "我们";
						} else if (ani == Animacy.unanimate) {
							return "我们";
						}
					}
				}
			} else if (per == Person.second) {
				if (gen == Gender.male) {
					if (num == Number.single) {
						if (ani == Animacy.animate) {
							return "你";
						} else if (ani == Animacy.unanimate) {
							return "你";
						}
					} else if (num == Number.plural) {
						if (ani == Animacy.animate) {
							return "你们";
						} else if (ani == Animacy.unanimate) {
							return "你们";
						}
					}
				} else if (gen == Gender.female) {
					if (num == Number.single) {
						if (ani == Animacy.animate) {
							return "你";
						} else if (ani == Animacy.unanimate) {
							return "你";
						}
					} else if (num == Number.plural) {
						if (ani == Animacy.animate) {
							return "你们";
						} else if (ani == Animacy.unanimate) {
							return "你们";
						}
					}
				} else if (gen == Gender.neuter) {
					if (num == Number.single) {
						if (ani == Animacy.animate) {
							return "你";
						} else if (ani == Animacy.unanimate) {
							return "你";
						}
					} else if (num == Number.plural) {
						if (ani == Animacy.animate) {
							return "你们";
						} else if (ani == Animacy.unanimate) {
							return "你们";
						}
					}
				}
			} else if (per == Person.third) {
				if (gen == Gender.male) {
					if (num == Number.single) {
						if (ani == Animacy.animate) {
							return "他";
						} else if (ani == Animacy.unanimate) {
							return "它";
						}
					} else if (num == Number.plural) {
						if (ani == Animacy.animate) {
							return "他们";
						} else if (ani == Animacy.unanimate) {
							return "它们";
						}
					}
				} else if (gen == Gender.female) {
					if (num == Number.single) {
						if (ani == Animacy.animate) {
							return "她";
						} else if (ani == Animacy.unanimate) {
							return "它";
						}
					} else if (num == Number.plural) {
						if (ani == Animacy.animate) {
							return "她们";
						} else if (ani == Animacy.unanimate) {
							return "它们";
						}
					}
				} else if (gen == Gender.neuter) {
					if (num == Number.single) {
						if (ani == Animacy.animate) {
							return "它";
						} else if (ani == Animacy.unanimate) {
							return "它";
						}
					} else if (num == Number.plural) {
						if (ani == Animacy.animate) {
							return "他们";
						} else if (ani == Animacy.unanimate) {
							return "它们";
						}
					}
				}
			}
			return null;
		}
		
		public static String decideOP(double[] pers, double[] nums, double[] gens, double[] anis) {
	//		"你", "我", "他", "她", "它", "你们", "我们", "他们", "她们", "它们"
			double max = 0;
			int win = -1;
			for(int i=0;i<EMUtil.pronounList.size();i++) {
				String pro = EMUtil.pronounList.get(i);
				double per = pers[EMUtil.getPerson(pro).ordinal()];
				double num = nums[EMUtil.getNumber(pro).ordinal()];
				double gen = gens[EMUtil.getGender(pro).ordinal()];
				double ani = anis[EMUtil.getAnimacy(pro).ordinal()];
				
				double p = per + num + gen + ani;
				if(p>max) {
					max = p;
					win = i;
				}
			}
			if(win==-1) {
				return "他";
			}
			return EMUtil.pronounList.get(win);
		}
	
		public static void main(String args[]) {
			// loadMeassure();
			System.out.println(measures.size());
		}
		
		public static double getP_C(Mention ant, Mention m, CoNLLPart part, String pronoun) {
//			if(true){
//			return 1;
//		}
			if(EMUtil.train) {
				return 1;
			}
			double mi = ContextNAACL.calMI(ant, m);
//			if(mi<-0.1) {
//				return 0.00000001;
//			}
//			if(ant.gram!=Grammatic.subject && mi<0) {
//				return 0.0001;
//			} else 
			if(ant.salienceID==0) {
				return 1;
			} else if(ant.salienceID<3) {
				return 1;
			} else if(ant.salienceID<4) {
				return .00001;
			} else if(true){
				return 0;
			}
			
			
			String animacy = EMUtil.getAntAnimacy(ant).name();
			String person = EMUtil.getAntPerson(ant.head).name();
			String gender = EMUtil.getAntGender(ant).name();
			String number = EMUtil.getAntNumber(ant).name();
			
//			if(animacy.equals(Animacy.unknown.name())) {
//				animacy = Animacy.animate.name();
//			}
//			if(gender.equals(Gender.unknown)) {
//				gender = Gender.male.name();
//			}
			
//			String animacy = EMUtil.getAnimacy(pronoun).name();
//			String person = EMUtil.getPerson(pronoun).name();
//			String gender = EMUtil.getGender(pronoun).name();
//			String number = EMUtil.getNumber(pronoun).name();
			
			String v = EMUtil.getFirstVerb(m.V);
			String o = EMUtil.getObjectNP(m.V);
			
//			HashMap<String, Double> anaphorConfNumber = ApplyEMNAACL.selectRestriction("number", 2, v, o);
//			HashMap<String, Double> anaphorConfGender = ApplyEMNAACL.selectRestriction("gender", 3, v, o);
//			HashMap<String, Double> anaphorConfPerson = ApplyEMNAACL.selectRestriction("person", 3, v, o);
//			HashMap<String, Double> anaphorConfAnimacy = ApplyEMNAACL.selectRestriction("animacy", 2, v, o);
//			
//			Double personP = anaphorConfPerson.get(person);
//			Double numberP = anaphorConfNumber.get(number);
//			Double genderP = anaphorConfGender.get(gender);
//			Double animacyP = anaphorConfAnimacy.get(animacy);
			
			return 1 
//					* numberP
//					* personP
//					* genderP 
//					* animacyP
					;
		}
	}

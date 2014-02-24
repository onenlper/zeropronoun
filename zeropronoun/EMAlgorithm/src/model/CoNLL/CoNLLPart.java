package model.CoNLL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import model.Element;
import model.Entity;
import model.Mention;
import model.SemanticRole;
import model.CoNLL.CoNLLDocument.DocType;
import model.syntaxTree.MyTreeNode;
import em.EMUtil;

/*
 * CoNLL part
 */
public class CoNLLPart {

	private int partID;
	
	public String docName;

	private CoNLLDocument document;

	private ArrayList<CoNLLSentence> sentences;

	private ArrayList<Element> nameEntities;

	private ArrayList<Entity> chains;

	public ArrayList<Mention> getMentions() {
		return mentions;
	}

	public HashMap<String, String> headExtendMap;

	private String partName;

	public String getPartName() {
		return partName;
	}

	public void setPartName(String partName) {
		this.partName = partName;
	}

	public void setMentions(ArrayList<Mention> mentions) {
		this.mentions = mentions;
	}

	private ArrayList<Mention> mentions;

	public String folder;
	
	public CoNLLPart() {
		this.sentences = new ArrayList<CoNLLSentence>();
		this.nameEntities = new ArrayList<Element>();
		this.chains = new ArrayList<Entity>();
		this.mentions = new ArrayList<Mention>();
	}

	public String rawText;

	public String label;

	public int getPartID() {
		return partID;
	}

	public CoNLLWord getWord(int wordIdx) {
		for (int i = 0; i < sentences.size(); i++) {
			CoNLLSentence sentence = sentences.get(i);
			if (wordIdx >= sentence.getStartWordIdx()
					&& wordIdx <= sentence.getEndWordIdx()) {
				return sentence.getWord(wordIdx - sentence.getStartWordIdx());
			}
		}
		return null;
	}

	public void setPartID(int partID) {
		this.partID = partID;
	}

	public CoNLLDocument getDocument() {
		return document;
	}

	public void setDocument(CoNLLDocument document) {
		this.document = document;
	}

	public ArrayList<CoNLLSentence> getCoNLLSentences() {
		return sentences;
	}

	public void setCoNLLSentences(ArrayList<CoNLLSentence> sentences) {
		this.sentences = sentences;
	}

	public ArrayList<Element> getNameEntities() {
		return nameEntities;
	}

	public void setNameEntities(ArrayList<Element> nameEntities) {
		this.nameEntities = nameEntities;
	}

	public ArrayList<Entity> getChains() {
		return chains;
	}

	public void setChains(ArrayList<Entity> chains) {
		this.chains = chains;
	}

	public void addSentence(CoNLLSentence sentence) {
		this.sentences.add(sentence);
		sentence.part = this;
	}

	public int wordCount = 0;

	public int getWordCount() {
		return wordCount;
	}

	/*
	 * generate named entities and coreference chain
	 */
	public void postProcess() {
		int wordIdx = 0;
		// parse named entities
		int start = 0;
		int end = 0;
		String neType = "";
		ArrayList<Mention> curMentions = new ArrayList<Mention>();
		for (int p = 0; p < this.sentences.size(); p++) {
			CoNLLSentence sentence = this.sentences.get(p);
			sentence.setSentenceIdx(p);
			sentence.setStartWordIdx(wordIdx);
			for (int i = 0; i < sentence.getWordsCount(); i++) {
				String ne = sentence.getWord(i).rawNamedEntity;
				if (!ne.equalsIgnoreCase("*")) {
					if (ne.charAt(0) == '(') {
						start = wordIdx;
						neType = ne.substring(1, ne.length() - 1);
					}
					if (ne.endsWith(")")) {
						end = wordIdx;
						Element element = new Element(start, end, neType);
						this.nameEntities.add(element);
					}
				}
				wordIdx++;
			}
			sentence.setEndWordIdx(wordIdx - 1);
		}
		for (Element ele : this.nameEntities) {
			for (int i = ele.start; i <= ele.end; i++) {
				this.getWord(i).rawNamedEntity = ele.content;
			}
		}
		this.wordCount = wordIdx;
		// parse mention
		for (CoNLLSentence sentence : this.sentences) {
			for (int i = 0; i < sentence.getWordsCount(); i++) {
				CoNLLWord word = sentence.getWord(i);
				String rawCoreference = word.rawCoreference;
				if (rawCoreference.equals("-")) {
					continue;
				}
				String tokens[] = rawCoreference.split("\\|");
				for (String token : tokens) {
					int leftBracket = token.indexOf('(');
					int rightBracket = token.indexOf(')');
					if (leftBracket != -1) {
						int rightEnd = token.length();
						if (rightBracket != -1) {
							rightEnd = rightBracket;
						}
						Mention em = new Mention();
						em.setStart(word.index);
						int clusterID = Integer.valueOf(token.substring(
								leftBracket + 1, rightEnd));
						em.entityIndex = clusterID;
						curMentions.add(em);
					}
					if (rightBracket != -1) {
						int leftStart = 0;
						if (leftBracket != -1) {
							leftStart = leftBracket + 1;
						}
						int clusterID = Integer.valueOf(token.substring(
								leftStart, rightBracket));
						for (int k = curMentions.size() - 1; k >= 0; k--) {
							Mention em = curMentions.get(k);
							if (em.entityIndex == clusterID) {
								StringBuilder content = new StringBuilder();
								StringBuilder orginal = new StringBuilder();
								em.setEnd(word.index);
								for (int m = em.getStart(); m <= em.getEnd(); m++) {
									content.append(this.getWord(m).word)
											.append(" ");
									orginal.append(this.getWord(m).orig)
											.append(" ");
								}
								em.extent = content.toString().trim();
								em.extent = orginal.toString().trim();
								curMentions.remove(k);
								this.mentions.add(em);
								break;
							}
						}
					}
				}
			}
		}
		// form entities
		HashMap<Integer, Entity> clusters = new HashMap<Integer, Entity>();
//		System.out.println(this.mentions.size() + "###");
		for (Mention em : this.mentions) {
			int clusterID = em.entityIndex;
			if (clusters.containsKey(clusterID)) {
				clusters.get(clusterID).addMention(em);
			} else {
				Entity entity = new Entity();
				entity.addMention(em);
				clusters.put(clusterID, entity);
			}
		}
		this.chains.addAll(clusters.values());
		processDiscourse();
		formSRLs();
	}

	public boolean isPU(String word) {
		HashSet<String> PUs = new HashSet<String>();
		PUs.addAll(Arrays.asList(".", "\"", "?", "!"));
		return PUs.contains(word);
	}

	public Map<Integer, Element> speakers = new HashMap<Integer, Element>();

	public final Set<String> reportVerb = new HashSet<String>(Arrays.asList(
			"表示", "讲起", "说话", "说"));

	private void processDiscourse() {
		if (this.getDocument().getType() == DocType.Article) {
			processDocDiscourse();
		} else {
			HashMap<Integer, String> utterHash = new HashMap<Integer, String>();
			String previousSpeaker = "";
			int utterOrder = -1;
			for (int i = 0; i < this.wordCount; i++) {
				String speaker = this.getWord(i).speaker;
				if (!speaker.equals(previousSpeaker)) {
					utterOrder++;
					previousSpeaker = speaker;
					utterHash.put(utterOrder, speaker);
				}
				this.getWord(i).utterOrder = utterOrder;
			}
			for (int i = 0; i < this.wordCount; i++) {
				int order = this.getWord(i).utterOrder;
				this.getWord(i).setPreviousSpeaker(utterHash.get(order - 1));
				this.getWord(i).setNextSpeaker(utterHash.get(order + 1));
			}
			for (int i = 0; i < this.wordCount; i++) {
				CoNLLWord word = this.getWord(i);
				int sentenceId = this.getWord(i).getSentence().getSentenceIdx();
				if (sentenceId == 0) {
					word.toSpeaker.add(word.nextSpeaker);
				} else if (sentenceId == this.sentences.size() - 1) {
					word.toSpeaker.add(word.previousSpeaker);
				} else {
					int previousSentenceStartWordIdx = this.sentences.get(
							sentenceId - 1).getStartWordIdx();
					String previousSentenceSpeaker = this
							.getWord(previousSentenceStartWordIdx).speaker;
					int nextSentenceStartWordIdx = this.sentences.get(
							sentenceId + 1).getStartWordIdx();
					String nextSentenceSpeaker = this
							.getWord(nextSentenceStartWordIdx).speaker;
					String speaker = word.speaker;
					if (speaker.equals(previousSentenceSpeaker)
							&& speaker.equalsIgnoreCase(nextSentenceSpeaker)) {
						word.toSpeaker.add(word.nextSpeaker);
						word.toSpeaker.add(word.previousSpeaker);
					} else if (!speaker.equals(previousSentenceSpeaker)
							&& speaker.equalsIgnoreCase(nextSentenceSpeaker)) {
						word.toSpeaker.add(word.previousSpeaker);
					} else if (speaker.equals(previousSentenceSpeaker)
							&& !speaker.equalsIgnoreCase(nextSentenceSpeaker)) {
						word.toSpeaker.add(word.nextSpeaker);
					} else {
						word.toSpeaker.add(word.nextSpeaker);
						word.toSpeaker.add(word.previousSpeaker);
					}
				}
			}
		}
	}

	public void processDocDiscourse() {
		int utterOrder = 1;
		for (int index = 0; index < this.wordCount; index++) {
			int start = -1;
			int end = -1;
			for (int s = index; s < this.wordCount; s++) {
				String word = this.getWord(s).word;
				if (word.equals("\"") || word.equals("``") || word.equals("“")
						|| word.equals("「")) {
					start = s;
					boolean find = false;
					for (int e = s + 1; e <= this.getWord(s).sentence
							.getEndWordIdx(); e++) {

						word = this.getWord(e).word;
						if (word.equals("\"") || word.equals("''")
								|| word.equals("”") || word.equals("」")) {

							find = true;
							end = e;
							// find speaker
							int backTrace = start - 1;
							String speaker = "";
							MyTreeNode VP = null;
							CoNLLWord reportW = null;
							while (backTrace >= 0
									&& !this.getWord(backTrace).posTag
											.equalsIgnoreCase("PU")) {
								CoNLLWord w = this.getWord(backTrace);
								if (reportVerb.contains(w.word)
										|| w.word.equalsIgnoreCase("说")) {
									MyTreeNode vv = w.getSentence()
											.getSyntaxTree().leaves
											.get(w.indexInSentence);
									while (true) {
										if (vv.value.equalsIgnoreCase("VP")) {
											VP = vv;
										}
										if (vv.getLeaves().get(0).leafIdx <= this
												.getWord(end).indexInSentence) {
											break;
										}
										vv = vv.parent;
									}
									reportW = w;
									break;
								}
								backTrace--;
							}
							if (VP == null) {
								int forwardTrace = end + 1;
								while (forwardTrace < this.wordCount
										&& !this.getWord(forwardTrace).posTag
												.equalsIgnoreCase("PU")) {
									CoNLLWord w = this.getWord(forwardTrace);
									if (reportVerb.contains(w.word)
											|| w.word.equalsIgnoreCase("说")) {
										VP = w.getSentence().getSyntaxTree().leaves
												.get(w.indexInSentence).parent.parent;
										MyTreeNode vv = w.getSentence()
												.getSyntaxTree().leaves
												.get(w.indexInSentence);
										while (true) {
											if (vv.value.equalsIgnoreCase("VP")) {
												VP = vv;
											}
											if (vv.getLeaves().get(0).leafIdx <= this
													.getWord(end).indexInSentence) {
												break;
											}
											vv = vv.parent;
										}
										reportW = w;
										break;
									}
									forwardTrace++;
								}
							}
							Mention subject = null;
							if (VP != null) {
								MyTreeNode NP = null;
								for (int m = VP.childIndex - 1; m >= 0; m--) {
									if (VP.parent.children.get(m).value
											.equalsIgnoreCase("NP")) {
										NP = VP.parent.children.get(m);
										break;
									}
								}
								if (NP == null && VP!=null && VP.parent!=null) {
									for (int m = VP.childIndex + 1; m < VP.parent.children
											.size(); m++) {
										if (VP.parent.children.get(m).value
												.equalsIgnoreCase("NP")) {
											NP = VP.parent.children.get(m);
											break;
										}
									}
								}
								if (NP != null) {
									subject = EMUtil.formPhrase(NP,
											reportW.getSentence());
									// speaker =
									// subject.source.toLowerCase();
									if(subject!=null) {
										CoNLLWord sw = this.getWord(subject.end);
										speaker = sw.word.toLowerCase();
									}
									// System.out.println("Speaker: " +
									// speaker + " " + reportW.word + " "
									// + reportW.getIndex() + " " +
									// reportW.sentence.getSentenceIdx()
									// + " " + start + " " +
									// this.getWord(start+1).word + " " +
									// end + this.getWord(end-1).word);
								}
							}
							if (speaker.isEmpty()) {
								speaker = "PER" + utterOrder;
								utterOrder++;
							}
							for (int t = start; t <= end; t++) {
								this.getWord(t).speaker = speaker;
								this.getWord(t).speakerM = subject;
							}
							// System.out.println(speaker);
							break;
						}
					}
					if (find) {
						index = end;
					} else {
						index = this.getWord(s).sentence.getEndWordIdx() + 1;
					}
					break;
				}
			}
		}
	}
	
	public void formSRLs() {
		for(CoNLLSentence s : this.sentences) {
			CoNLLWord w = s.getWords().get(0);
			
			//TODO
			int roles = w.getPredicateArgument().split("\\s+").length;
			for(int i=0;i<roles;i++) {
				SemanticRole role = new SemanticRole();
				
				for(int k=0;k<s.words.size();k++) {
					CoNLLWord word = s.words.get(k);
					String label = word.getPredicateArgument().split("\\s+")[i];
					if(label.startsWith("(V*")) {
						Mention m = new Mention();
						m.start = word.getIndex();
						m.end = word.getIndex();
						m.extent = this.getWord(m.start).word;
						role.predicate = m;
					} else if(label.startsWith("(ARG")) {
						Mention m = new Mention();
						m.start = word.getIndex();
						
						String roleName = label.substring(1, label.lastIndexOf("*"));
						ArrayList<Mention> mentions = role.args.get(roleName);
						if(mentions==null) {
							mentions = new ArrayList<Mention>();
							role.args.put(roleName, mentions);
						}
						mentions.add(m);
						
						//find end
						while(!word.getPredicateArgument().split("\\s+")[i].endsWith(")")) {
							k++;
							word = s.words.get(k);
						}
						m.end = s.words.get(k).index;
					}
					
					if(role.predicate==null) {
						System.err.println("GE");
						System.exit(1);
					}
				}

				s.roles.add(role);
			}
			
		}
	}

}

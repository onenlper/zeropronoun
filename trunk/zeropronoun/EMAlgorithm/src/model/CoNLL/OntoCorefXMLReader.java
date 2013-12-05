package model.CoNLL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import model.Entity;
import model.Mention;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import util.Common;

public class OntoCorefXMLReader extends DefaultHandler {

	ArrayList<String> tags = new ArrayList<String>();
	ArrayList<ArrayList<Entity>> chainses;
	ArrayList<Entity> entities;
	ArrayList<Mention> ems;
	ArrayList<Mention> currentMentions;
	HashMap<String, Integer> entityMap = new HashMap<String, Integer>();
	boolean includeNotAnaphor;
	int partID = 0;

	int pos = 0;

	CoNLLDocument document;

	public OntoCorefXMLReader(ArrayList<ArrayList<Entity>> chainses, CoNLLDocument document, boolean includeNotAnaphor) {
		if(document.addZero) {
			Common.bangErrorPOS("Already add zeros");
		}
		document.addZero = true;
		this.chainses = chainses;
		currentMentions = new ArrayList<Mention>();
		this.document = document;
		this.includeNotAnaphor = includeNotAnaphor;
	}

	boolean firstText = false;

	public void startElement(String uri, String name, String qName, Attributes atts) {
		tags.add(qName);
		if (qName.equalsIgnoreCase("TEXT")) {
			pos = 0;
			entities = new ArrayList<Entity>();
			this.chainses.add(entities);
			entityMap.clear();
		}
		if (qName.equalsIgnoreCase("COREF")) {
			if (atts.getValue("TYPE").equals("IDENT")) {
				Mention em = new Mention();
				em.start = pos;
				currentMentions.add(em);
				String corefID = atts.getValue("ID") + "_" + Integer.toString(this.partID);

				int entityID = 0;
				if (entityMap.containsKey(corefID)) {
					entityID = entityMap.get(corefID);
				} else {
					entityID = entityMap.size();
					entityMap.put(corefID, entityID);
					Entity entity = new Entity();
					this.entities.add(entity);
				}
//				if(this.entities.get(entityID))
				
				this.entities.get(entityID).addMention(em);
				em.entity = this.entities.get(entityID);
				em.entityIndex = entityID;
				identCoref.add(true);
			} else {
				identCoref.add(false);
			}
		}
	}

	private Stack<Boolean> identCoref = new Stack<Boolean>();

	public void characters(char ch[], int start, int length) throws SAXException {

		// if (this.entities != null) {
		// for (Entity e : this.entities) {
		// if (e.mentions.size() == 0) {
		// System.err.println("GEEE!!!");
		// System.out.println(e.entityIdx);
		// System.exit(1);
		// }
		// }
		// }

		String tag = this.tags.get(this.tags.size() - 1);
		String text = new String(ch, start, length);

		text = overFlowText + text;

		text = text.replace("\n", " ").replace("\r", " ").replace("*OP*", "").replace("*PRO*", "");
		text = text.replaceAll("\\*RNR\\*\\-[0-9]+", "").replaceAll("\\*T\\*\\-[0-9]+", "").replaceAll(
				"\\*ICH\\*-[0-9]+", "").replaceAll("\\*\\-[0-9]+", "").replace("*?*", "").replace(" *T* ", " ")
				.replace("-AMP-", "&").replace("-LAB-", "<").replace("-RAB-", ">").replace(" * ", " ").trim();

		if (text.trim().equalsIgnoreCase("*pro*")) {
			zero = true;
		} else {
			// text = text.replace("*pro*", "").trim();
			// @做 起来 了 。 正 因为 *p@
			// text = text.replace("*p", "").trim();
			// text = text.replace("ro*", "").trim();
			zero = false;
			if (!text.isEmpty()) {
				String tokens[] = text.split("\\s+");
				int tempPos = pos;
				for (String str : tokens) {
					if (str.equals("*pro*")) {
						continue;
					}
					// System.out.println(pos);
					String word = this.document.getParts().get(this.partID).getWord(tempPos++).word;
					// System.out.println(word + "#" + str);
					if (!str.equalsIgnoreCase(word)) {
						// rollback
//						System.out.println("rollback");
						overFlowText = text.trim();

						// BufferedReader br = new BufferedReader(new
						// InputStreamReader(System.in));
						// try {
						// String s = br.readLine();
						// } catch (IOException e) {
						// // TODO Auto-generated catch block
						// e.printStackTrace();
						// }
//						System.out.println("@" + str + "#" + word + "@");
//						System.out.println("@" + text.trim() + "@");
						// System.exit(1);
						return;
					}
				}
				// add *pro*
				int notZero = 0;
				for (int i = 0; i < tokens.length; i++) {
					String str = tokens[i];
					if (str.equals("*pro*")) {
						if (includeNotAnaphor) {
							Entity entity = new Entity();
							// singleton entity
							entity.singleton = true;
							entity.entityIdx = -20;
							Mention mention = new Mention();
							mention.notInChainZero = true;
							mention.start = pos + notZero;
							mention.end = -1;
							entity.addMention(mention);

							String corefID = mention.start + "_" + this.partID + "#";
							int entityIdx = this.entityMap.size();

							while (this.entityMap.containsKey(corefID)) {
								corefID += corefID + "_";
							}
							this.entityMap.put(corefID, entityIdx);
							this.entities.add(entity);
						}
					} else {
						String word = this.document.getParts().get(this.partID).getWord(pos + notZero).word;
						if (word.equalsIgnoreCase(str)) {
							// System.out.println(word + "#" + str + "#" +
							// (pos+notZero));
						} else {
							// System.out.println(word + "#" + str + "#" +
							// (pos+notZero));
							System.out.println("!!!!");
							System.exit(1);
						}
						notZero++;
					}
				}
				pos += notZero;
				overFlowText = "";
			}
		}
	}

	String overFlowText = "";

	String text;

	boolean zero = false;

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("COREF")) {
			boolean ident = identCoref.pop();
			if (ident) {
				if (zero) {
					currentMentions.get(currentMentions.size() - 1).end = -1;
				} else {
					currentMentions.get(currentMentions.size() - 1).end = pos - 1;
				}
				currentMentions.remove(currentMentions.size() - 1);
			}
		} else if (qName.equalsIgnoreCase("TEXT")) {
			partID++;
		}
		tags.remove(tags.size() - 1);

		// end document, check and add zero pronoun
		if (qName.equalsIgnoreCase("DOC")) {

			for (int i = 0; i < chainses.size(); i++) {
				ArrayList<Entity> chains = chainses.get(i);
				CoNLLPart part = document.getParts().get(i);
				ArrayList<Entity> conllChains = part.getChains();

				// sort conllChains
				for (Entity e : conllChains) {
					Collections.sort(e.mentions);
				}

				for (int e = 0; e < chains.size(); e++) {
					Entity entity = chains.get(e);
					if (entity.mentions.size() == 1 && entity.mentions.get(0).end != -1) {
						// skip it
						// continue;
					}
					Collections.sort(entity.mentions);

					Mention m = this.getFirstNonZeroMention(entity);

					// add a pure zero pronoun chain
					if (m == null) {
						// System.out.println("HAPPENS!!!!!");
						// this.printEntity(entity);
						conllChains.add(entity);
						// System.out.println(entity.mentions.size());
						// System.out.println(part.getWord(234).word);
						// System.out.println(part.getWord(235).word);
						// System.exit(1);
					} else {
						// check and add zero mention
						boolean find = false;
						for (Entity conllChain : conllChains) {
							if (m.equals(conllChain.mentions.get(0))) {
								this.combineEntity(conllChain, entity, i);
								// System.out.println("Combine!!!");
								find = true;
								break;
							}
						}
						if (!find) {
							// if not find, then may only coreference with zero
							// pronouns
							// check other mentions should be all zero pronouns
							int nonZero = 0;
							for (Mention temp : entity.mentions) {
								if (temp.end != -1) {
									nonZero++;
									if (nonZero == 2) {
										// System.out.println("NOT FOUND!!!!");
										// this.printEntity(entity);
										// System.out.println(this.document.getParts().get(i).getWord(79).word);
										// System.out.println(this.document.getParts().get(i).getWord(80).word);
										// System.out.println(this.document.getParts().get(i).getWord(81).word);
										// System.out.println(this.document.getParts().get(i).getWord(82).word);
										// System.out.println(this.document.getParts().get(i).getWord(83).word);
										// System.out.println(this.document.getParts().get(i).getWord(98).word);
										// System.out.println(this.document.getParts().get(i).getWord(99).word);
										// System.out.println(this.document.getParts().get(i).getWord(157).word);
										// System.exit(1);
									}
								}
							}
							boolean zero = false;
							for(Mention m2 : entity.mentions) {
								if(m2.end!=-1) {
									newNPMentions++;
								} else {
									zeroCorefNewNP++;
									zero = true;
								}
							}
//							if(!zero) {
//								printEntity(entity);
//								Common.bangErrorPOS("EE");
//							}
							if(zero) {
								conllChains.add(entity);
							}
						}
					}
					ArrayList<Mention> ems = entity.mentions;
					// emses.addAll(ems);
					// System.out.println("=====================");
					// for (Mention em : ems) {
					// System.out.println(em.start + "," + em.end + " ");
					// }
				}

				// check again
				Collections.sort(conllChains);
				Collections.sort(chains);

				// for (int m = 0; m < conllChains.size(); m++) {
				// Entity conllChain = conllChains.get(m);
				// Entity chain = chains.get(m);
				// Collections.sort(conllChain.mentions);
				// Collections.sort(chain.mentions);
				// for (int n = 0; n < conllChain.mentions.size(); n++) {
				// if
				// (!conllChain.mentions.get(n).equals(chain.mentions.get(n))) {
				// this.printEntity(conllChain);
				// this.printEntity(chain);
				// System.out.println("Finally check wrong!!!");

				// System.out.println(this.document.getParts().get(i).getWord(104).word);
				// System.out.println(this.document.getParts().get(i).getWord(105).word);
				// System.out.println(this.document.getParts().get(i).getWord(106).word);
				// System.out.println(this.document.getParts().get(i).getWord(107).word);
				// System.out.println(this.document.getParts().get(i).getWord(108).word);
				// System.out.println(this.document.getParts().get(i).getWord(314).word);
				// break;
				// System.out.println("PROBLEM4!!!!!: " +
				// conllChains.size() + "#" + chains.size());
				// System.exit(1);
				// }
				// }
				// // System.out.println("GOODDD");
				// }
				// if (conllChains.size() != chains.size()) {
				// System.out.println("PROBLEM3!!!!!: " + conllChains.size() +
				// "#" + chains.size());
				// System.exit(1);
				// }

				// System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
			}
		}
	}

	public static int newNPMentions = 0;
	
	public static int zeroCorefNewNP = 0;

	private void combineEntity(Entity conllE, Entity zeroE, int part) {
		int k = 0;
		for (Mention m : zeroE.mentions) {
			if (m.end == -1) {
				conllE.addMention(m);
			} else {
				// check
				// if (!m.equals(conllE.mentions.get(k))) {
				// System.out.println("PROBLEM1!!!!!: ");
				// this.printEntity(conllE);
				// this.printEntity(zeroE);
				// System.out.println(this.document.getParts().get(part).getWord(579).word);
				// System.out.println(this.document.getParts().get(part).getWord(580).word);
				// System.out.println(this.document.getParts().get(part).getWord(581).word);
				// System.out.println(part);
				// System.exit(1);
				// }else {
				// k++;
				// }
			}
		}

		// System.out.println("GOODD!!");

		Collections.sort(conllE.mentions);
		if (zeroE.mentions.size() != conllE.mentions.size()) {
			// System.out.println("PROBLEM2!!!!!");
			// System.exit(1);
		}
	}

	public static void printEntity(Entity e) {
		StringBuilder sb = new StringBuilder();
		for (Mention m : e.mentions) {
			sb.append(m.start).append(",").append(m.end).append(" ");
		}
		System.out.println(sb.toString().trim());
	}

	private Mention getFirstNonZeroMention(Entity e) {
		Mention ret = null;
		for (Mention m : e.mentions) {
			if (m.end != -1) {
				ret = m;
				break;
			}
		}
		return ret;
	}

	private static String prefix = "/shared/mlrdir1/disk1/mlr/corpora/CoNLL-2012/conll-2012-train-v0/data/files/data/chinese/annotations/";
	private static String anno = "annotations/";
	private static String suffix = ".coref";

	public static void addGoldZeroPronouns(CoNLLDocument document, boolean includeNotAnaphor) {
		try {
			String conllPath = document.getFilePath();
			int a = conllPath.indexOf(anno);
			int b = conllPath.indexOf(".");
			String middle = conllPath.substring(a + anno.length(), b);
			String path = prefix + middle + suffix;
//			System.out.println(path);
			InputStream inputStream = new FileInputStream(new File(path));
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			ArrayList<ArrayList<Entity>> chainses = new ArrayList<ArrayList<Entity>>();
			OntoCorefXMLReader reader = new OntoCorefXMLReader(chainses, document, includeNotAnaphor);
			sp.parse(new InputSource(inputStream), reader);
			// System.out.println(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<ArrayList<Mention>> getGoldZeroPronouns(CoNLLDocument document, boolean includeNotAnaphor) {
		ArrayList<ArrayList<Mention>> goldZeroses = new ArrayList<ArrayList<Mention>>();
		try {
			String conllPath = document.getFilePath();
			int a = conllPath.indexOf(anno);
			int b = conllPath.indexOf(".");
			String middle = conllPath.substring(a + anno.length(), b);
			String path = prefix + middle + suffix;
			System.out.println(path);
			InputStream inputStream = new FileInputStream(new File(path));
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			ArrayList<ArrayList<Entity>> chainses = new ArrayList<ArrayList<Entity>>();
			OntoCorefXMLReader reader = new OntoCorefXMLReader(chainses, document, includeNotAnaphor);
			sp.parse(new InputSource(inputStream), reader);

			for (CoNLLPart part : document.getParts()) {
				ArrayList<Mention> goldZeros = new ArrayList<Mention>();
				ArrayList<Entity> entities = part.getChains();
				for (Entity entity : entities) {
					for (Mention mention : entity.mentions) {
						if (mention.end == -1) {
							goldZeros.add(mention);
						}
					}
				}
				goldZeroses.add(goldZeros);
			}
			// System.out.println(path);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return goldZeroses;
	}

	public static void main(String args[]) {

		String conllPath = "/users/yzcchen/chen3/CoNLL/conll-2012/v4/data/development/data/chinese/annotations/bc/phoenix/00/"
				+ "phoenix_0000.v4_auto_conll";
		CoNLLDocument document = new CoNLLDocument(conllPath);

		addGoldZeroPronouns(document, true);

	}
}

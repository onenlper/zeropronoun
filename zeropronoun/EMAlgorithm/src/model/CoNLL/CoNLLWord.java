package model.CoNLL;

import java.util.HashSet;

import model.Mention;


public class CoNLLWord {

	public Mention speakerM;
	
	public String sourceLine;
	
	public int utterOrder = -2;
	
	public String word;

	public String previousSpeaker;
	
	public String speaker;
	
	public String nextSpeaker;
	
	public Mention getSpeakerM() {
		return speakerM;
	}
	
	public String arLemma;
	
	public String arUnBuck;
	
	public String arBuck;

	public String getArLemma() {
		return arLemma;
	}

	public void setArLemma(String arLemma) {
		this.arLemma = arLemma;
	}

	public String getArUnBuck() {
		return arUnBuck;
	}

	public void setArUnBuck(String arUnBuck) {
		this.arUnBuck = arUnBuck;
	}

	public String getArBuck() {
		return arBuck;
	}

	public void setArBuck(String arBuck) {
		this.arBuck = arBuck;
	}

	public HashSet<String> getToSpeaker() {
		return toSpeaker;
	}

	public void setToSpeaker(HashSet<String> toSpeaker) {
		this.toSpeaker = toSpeaker;
	}

	public void setSpeakerM(Mention speakerM) {
		this.speakerM = speakerM;
	}

	public HashSet<String> toSpeaker = new HashSet<String>();
	
	public int getUtterOrder() {
		return utterOrder;
	}

	public void setUtterOrder(int utterOrder) {
		this.utterOrder = utterOrder;
	}

	public String getPreviousSpeaker() {
		return previousSpeaker;
	}

	public void setPreviousSpeaker(String previousSpeaker) {
		this.previousSpeaker = previousSpeaker;
	}

	public String getSpeaker() {
		return speaker;
	}

	public void setSpeaker(String speaker) {
		this.speaker = speaker;
	}

	public String getNextSpeaker() {
		return nextSpeaker;
	}

	public void setNextSpeaker(String nextSpeaker) {
		this.nextSpeaker = nextSpeaker;
	}

	public int getIndexInSentence() {
		return indexInSentence;
	}

	public void setIndexInSentence(int indexInSentence) {
		this.indexInSentence = indexInSentence;
	}

	public String orig;
	
	public String posTag;
	
	public String predicateLemma;
	
	public String predicateFramesetID;
	
	public String wordSense;

	public String getOrig() {
		return orig;
	}

	public int indexInSentence;
	
	public void setOrig(String orig) {
		this.orig = orig;
	}

	public String rawNamedEntity;
	
	public String predicateArgument;
	
	public String rawCoreference;

	public int index;
	
	public CoNLLSentence getSentence() {
		return sentence;
	}

	public void setSentence(CoNLLSentence sentence) {
		this.sentence = sentence;
	}

	public CoNLLSentence sentence;
	
	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getPosTag() {
		return posTag;
	}

	public void setPosTag(String posTag) {
		this.posTag = posTag;
	}

	public String getPredicateLemma() {
		return predicateLemma;
	}

	public void setPredicateLemma(String predicateLemma) {
		this.predicateLemma = predicateLemma;
	}

	public String getPredicateFramesetID() {
		return predicateFramesetID;
	}

	public void setPredicateFramesetID(String predicateFramesetID) {
		this.predicateFramesetID = predicateFramesetID;
	}

	public String getWordSense() {
		return wordSense;
	}

	public void setWordSense(String wordSense) {
		this.wordSense = wordSense;
	}

	public String getRawNamedEntity() {
		return rawNamedEntity;
	}

	public void setRawNamedEntity(String rawNamedEntity) {
		this.rawNamedEntity = rawNamedEntity;
	}

	public String getPredicateArgument() {
		return predicateArgument;
	}

	public void setPredicateArgument(String predicateArgument) {
		this.predicateArgument = predicateArgument;
	}

	public String getRawCoreference() {
		return rawCoreference;
	}

	public void setRawCoreference(String rawCoreference) {
		this.rawCoreference = rawCoreference;
	}

}

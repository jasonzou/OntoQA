/**
 * Copyright 2011 HIT-CIR
 * Research Center for Information Retrieval
 * Harbin Institute of Technology
 * http://ir.hit.edu.cn
 */

package cn.edu.hit.ir.nlp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import cn.edu.hit.ir.util.ConfigUtil;

import edu.sussex.nlp.jws.JWS;
import edu.sussex.nlp.jws.Lin;

import net.didion.jwnl.JWNL;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.IndexWord;
import net.didion.jwnl.data.IndexWordSet;
import net.didion.jwnl.data.POS;
import net.didion.jwnl.data.Pointer;
import net.didion.jwnl.data.PointerType;
import net.didion.jwnl.data.PointerUtils;
import net.didion.jwnl.data.Synset;
import net.didion.jwnl.data.Word;
import net.didion.jwnl.data.list.PointerTargetNode;
import net.didion.jwnl.data.list.PointerTargetNodeList;
import net.didion.jwnl.dictionary.Dictionary;

/**
 * A wrapper of wordnet using the JWNL and JWS library.
 * <p>
 * Refer to OpenEphra.
 * <p>It surpports the following functions:
 * <ul>
 * <li>Get synonyms</li>
 * <li>Compute words similarity</li>
 * <li>Compute sentences similarity</li>
 * <li></li>
 * </ul>
 * 
 *
 * @author   bin3 (bin183cs@gmail.com)
 * @version  0.1.0
 * @date	 2011-5-15
 */

public class WordNet {
	
	/**
	 * JWNL的配置文件
	 * 在jwnl_properties.xml中需要设置dictionary_path为安装的wordnet的词典路径
	 */
	static final String JWNL_PROPERTIES_PATH = "JWNL_PROPERTIES_PATH";
	
	/**
	 * JWS需要的配置
	 */
	static final String WORDNET_VERSION = "WORDNET_VERSION";
	static final String WORDNET_PATH = "WORDNET_PATH";
	
	
	
	/** Indicates that a word is an adjective. */
	public static final POS ADJECTIVE = POS.ADJECTIVE;
	/** Indicates that a word is an adverb. */
	public static final POS ADVERB = POS.ADVERB;
	/** Indicates that a word is a noun. */
	public static final POS NOUN = POS.NOUN;
	/** Indicates that a word is a verb. */
	public static final POS VERB = POS.VERB;
	
	public static final POS[] ENTITY_POS_ARRAY = {NOUN, VERB};
	
	/** Maximum length of a path to an expansion. */
	public static final int MAX_PATH_LENGTH = 1;
	
	// relations for multiple parts of speech
	/** Weight for the relation 'synonym'. */
	private static final double SYNONYM_WEIGHT = 0.9;
	/** Weight for the relation 'hypernym'. */
	private static final double HYPERNYM_WEIGHT = 0.8;
	/** Weight for the relation 'hyponym'. */
	private static final double HYPONYM_WEIGHT = 0.7;
//	/** Weight for the relation 'see-also'. */
//	private static final double SEE_ALSO_WEIGHT = 0.5;
//	/** Weight for the relation 'gloss'. */
//	private static final double GLOSS_WEIGHT = 0.6;
//	/** Weight for the relation 'rgloss'. */
//	private static final double RGLOSS_WEIGHT = 0.2;
	
	// relations for verbs
	/** Weight for the relation 'entailing'. */
	private static final double ENTAILING_WEIGHT = 0.7;
	/** Weight for the relation 'causing'. */
	private static final double CAUSING_WEIGHT = 0.5;
	
	// relations for nouns
	/** Weight for the relation 'member-of'. */
	private static final double MEMBER_OF_WEIGHT = 0.5;
	/** Weight for the relation 'substance-of'. */
	private static final double SUBSTANCE_OF_WEIGHT = 0.5;
	/** Weight for the relation 'part-of'. */
	private static final double PART_OF_WEIGHT = 0.5;
	/** Weight for the relation 'has-member'. */
	private static final double HAS_MEMBER_WEIGHT = 0.5;
	/** Weight for the relation 'has-substance'. */
	private static final double HAS_SUBSTANCE_WEIGHT = 0.5;
	/** Weight for the relation 'has-part'. */
	private static final double HAS_PART_WEIGHT = 0.5;
	
	// relations for adjectives and adverbs
//	/** Weight for the relation 'pertainym'. */
//	private static final double PERTAINYM_WEIGHT = 0.5;
	
	/** Minimum weight of an expansion. */
	public static final double MIN_EXPANSION_WEIGHT = 0;	
	
	private static WordNet instance = null;
	
	private Configuration config;
	
	/** WordNet dictionary. */
	private net.didion.jwnl.dictionary.Dictionary dict;

	private JWS	jws;
	private Lin lin;
	
	public static WordNet getInstance() {
		if (instance == null) {
			instance = new WordNet();
		}
		return instance;
	}

	/**
	 * Creates a new instance of WordNet.
	 * 
	 */
	private WordNet() {
		initConfig();
		initResources();	
	}
	
	private void initConfig() {
		try {
			config = new PropertiesConfiguration(ConfigUtil.getPath(getClass()));
		} catch (ConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	private void initResources() {
		initJWNL();
		//initJWS();	// for development
	}
	
	private void initJWNL() {
		try {
			JWNL.initialize(new FileInputStream(config.getString(JWNL_PROPERTIES_PATH)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (JWNLException e) {
			e.printStackTrace();
		}
		dict = Dictionary.getInstance();
	}
	
	private void initJWS() {
		jws = new JWS(config.getString(WORDNET_PATH), config.getString(WORDNET_VERSION));
		lin = jws.getLin();
	}
	
	/**
	 * Checks if the word exists in WordNet.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is in WordNet
	 */
	public boolean isWord(String word) {
		if (dict == null) return false;
		
		IndexWordSet indexWordSet = null;
		try {
			indexWordSet = dict.lookupAllIndexWords(word);
		} catch (JWNLException e) {}
		
		return indexWordSet.size() > 0;
	}
	
	/**
	 * Checks if the word exists in WordNet. Supports multi-token terms.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is in WordNet
	 */
	public boolean isCompoundWord(String word) {
		if (dict == null) return false;
		
		// do not look up words with special characters other than '.'
		if (word.matches(".*?[^\\w\\s\\.].*+")) return false;
		
		IndexWordSet indexWordSet = null;
		try {
			indexWordSet = dict.lookupAllIndexWords(word);
		} catch (JWNLException e) {}
		
		// ensure that the word, and not just a substring, was found in WordNet
		int wordTokens = word.split("\\s", -1).length;
		int wordDots = word.split("\\.", -1).length;
		for (IndexWord indexWord : indexWordSet.getIndexWordArray()) {
			String lemma = indexWord.getLemma();
			int lemmaTokens = lemma.split("\\s", -1).length;
			int lemmaDots = lemma.split("\\.", -1).length;
			if (wordTokens == lemmaTokens && wordDots == lemmaDots) return true;
		}
		return false;
	}
	
	/**
	 * Checks if the word exists as an adjective.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is an adjective
	 */
	public boolean isAdjective(String word) {
		if (dict == null) return false;
		
		IndexWord indexWord = null;
		try {
			indexWord = dict.lookupIndexWord(POS.ADJECTIVE, word);
		} catch (JWNLException e) {}
		
		return (indexWord != null) ? true : false;
	}
	
	/**
	 * Checks if the word exists as an adverb.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is an adverb
	 */
	public boolean isAdverb(String word) {
		if (dict == null) return false;
		
		IndexWord indexWord = null;
		try {
			indexWord = dict.lookupIndexWord(POS.ADVERB, word);
		} catch (JWNLException e) {}
		
		return (indexWord != null) ? true : false;
	}
	
	/**
	 * Checks if the word exists as a noun.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is a noun
	 */
	public boolean isNoun(String word) {
		if (dict == null) return false;
		
		IndexWord indexWord = null;
		try {
			indexWord = dict.lookupIndexWord(POS.NOUN, word);
		} catch (JWNLException e) {}
		
		return (indexWord != null) ? true : false;
	}
	
	/**
	 * Checks if the word exists as a noun. Supports multi-token terms.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is a noun
	 */
	public boolean isCompoundNoun(String word) {
		if (dict == null) return false;
		
		// do not look up words with special characters other than '.'
		if (word.matches(".*?[^\\w\\s\\.].*+")) return false;
		
		IndexWord indexWord = null;
		try {
			indexWord = dict.lookupIndexWord(POS.NOUN, word);
		} catch (JWNLException e) {}
		if (indexWord == null) return false;
		
		// ensure that the word, and not just a substring, was found in WordNet
		int wordTokens = word.split("\\s", -1).length;
		int wordDots = word.split("\\.", -1).length;
		String lemma = indexWord.getLemma();
		int lemmaTokens = lemma.split("\\s", -1).length;
		int lemmaDots = lemma.split("\\.", -1).length;
		return wordTokens == lemmaTokens && wordDots == lemmaDots;
	}
	
	/**
	 * Checks if the word exists as a verb.
	 * 
	 * @param word a word
	 * @return <code>true</code> iff the word is a verb
	 */
	public boolean isVerb(String word) {
		if (dict == null) return false;
		
		IndexWord indexWord = null;
		try {
			indexWord = dict.lookupIndexWord(POS.VERB, word);
		} catch (JWNLException e) {}
		
		return (indexWord != null) ? true : false;
	}
	
	/**
	 * Looks up the lemma of a word.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return lemma or <code>null</code> if lookup failed
	 */
	public String getLemma(String word, POS pos) {
		if (dict == null) return null;
		
		IndexWord indexWord = null;
		try {
			indexWord = dict.lookupIndexWord(pos, word);
		} catch (JWNLException e) {}
		if (indexWord == null) return null;
		
		String lemma = indexWord.getLemma();
		lemma = lemma.replace("_", " ");
		
		return lemma;
	}
	
	/**
	 * Looks up the lemma of a compound word.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return lemma or <code>null</code> if lookup failed
	 */
	public String getCompoundLemma(String word, POS pos) {
		// do not look up words with special characters other than '.'
		if (word.matches(".*?[^\\w\\s\\.].*+")) return null;
		
		String lemma = getLemma(word, pos);
		if (lemma == null) return null;
		
		// ensure that the word, and not just a substring, was found in WordNet
		int wordTokens = word.split("\\s", -1).length;
		int wordDots = word.split("\\.", -1).length;
		int lemmaTokens = lemma.split("\\s", -1).length;
		int lemmaDots = lemma.split("\\.", -1).length;
		if (wordTokens != lemmaTokens || wordDots != lemmaDots) return null;
		
		return lemma;
	}
	
	/**
	 * Looks up the most common synset of a word.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return synset or <code>null</code> if lookup failed
	 */
	private Synset getCommonSynset(String word, POS pos) {
		if (dict == null) return null;
		
		Synset synset = null;
		try {
			IndexWord indexWord = dict.lookupIndexWord(pos, word);
			if (indexWord == null) return null;
			synset = indexWord.getSense(1);
		} catch (JWNLException e) {}
		
		return synset;
	}
	
	/**
	 * Looks up the synsets that correspond to the nodes in a node list.
	 * 
	 * @param nodes node list
	 * @return synsets
	 */
	private Synset[] getSynsets(PointerTargetNodeList nodes) {
		Synset[] synsets = new Synset[nodes.size()];
		
		for (int i = 0; i < nodes.size(); i++) {
			PointerTargetNode node  = (PointerTargetNode) nodes.get(i);
			synsets[i] = node.getSynset();
		}
		
		return synsets;
	}
	
	/**
	 * Looks up the lemmas of the words in a synset.
	 * 
	 * @param synset a synset
	 * @return lemmas
	 */
	private String[] getLemmas(Synset synset) {
		Word[] words = synset.getWords();
		String[] lemmas = new String[words.length];
		
		for (int i = 0; i < words.length; i++) {
			lemmas[i] = words[i].getLemma();
			lemmas[i] = lemmas[i].replace("_", " ");
		}
		
		return lemmas;
	}
	
	/**
	 * Looks up the lemmas of the words in all synsets.
	 * 
	 * @param synsets the synsets
	 * @return lemmas
	 */
	private String[] getLemmas(Synset[] synsets) {
		HashSet<String> lemmaSet = new HashSet<String>();
		
		for (Synset synset : synsets) {
			String[] lemmas = getLemmas(synset);
			for (String lemma : lemmas) lemmaSet.add(lemma);
		}
		
		return lemmaSet.toArray(new String[lemmaSet.size()]);
	}
	
	// relations for multiple parts of speech
	
	/**
	 * Looks up synonyms of the given word, assuming that it is used in its most
	 * common sense.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return synonyms or <code>null</code> if lookup failed
	 */
	public String[] getSynonyms(String word, POS pos) {
		Synset synset = getCommonSynset(word, pos);
		if (synset == null) return null;
		
		return getLemmas(synset);
	}
	
	/**
	 * Returns the set of synonyms of the given word with specific POS.
	 *
	 * @param word the word
	 * @param pos the POS of the word
	 * @return the set of synonyms or <code>null</code> if no synonym found
	 */
	public Set<String> getSynonymSet(String word, POS pos) {
		// If word is not compound word, such as "surrounding mississippi"
		// return null.
		if (!isCompoundWord(word)) {
			return null;
		}
		
		String[] synonyms = getSynonyms(word, pos);
		if (synonyms == null) {
			return null;
		} else {
			Set<String> synSet = new HashSet<String>();
			synSet.addAll(Arrays.asList(synonyms));
			
			// Remove the word itself
			synSet.remove(word);
			
			if (!synSet.isEmpty()) {
				return synSet;
			} else {
				return null;
			}
		}
	}
	
	/**
	 * Returns the set of synonyms of the given word, assuming that it is used 
	 * in its most common sense.
	 * 
	 * @param word a word
	 * @return the set of synonyms or <code>null</code> if no synonym found
	 */
	public Set<String> getSynonymSet(String word) {
		// If word is not compound word, such as "surrounding mississippi"
		// return null.
		if (!isCompoundWord(word)) {
			return null;
		}
		
		Set<String> synSet = new HashSet<String>();
		for (int i = 0; i < ENTITY_POS_ARRAY.length; i++) {
			Set<String> oneSet = getSynonymSet(word, ENTITY_POS_ARRAY[i]);
			if (oneSet != null) {
				synSet.addAll(oneSet);
			}		
		}
		
		// Remove the word itself
		synSet.remove(word);
		
		if (!synSet.isEmpty()) {
			return synSet;
		} else {
			return null;
		}
	}
	
	/**
	 * Looks up hypernyms of the given word, assuming that it is used in its
	 * most common sense.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return hypernyms or <code>null</code> if lookup failed
	 */
	public String[] getHypernyms(String word, POS pos) {
		Synset synset = getCommonSynset(word, pos);
		if (synset == null) return null;
		
		Synset[] hypernyms = getHypernymSynsets(synset);
		if (hypernyms == null) return null;
		
		return getLemmas(hypernyms);
	}
	
	// get 'hypernym' synsets
	private Synset[] getHypernymSynsets(Synset synset) {
		PointerTargetNodeList hypernyms = null;
		try {
			hypernyms = PointerUtils.getInstance().getDirectHypernyms(synset);
		} catch (JWNLException e) {}
		if (hypernyms == null) return null;
		
		return getSynsets(hypernyms);
	}
	
	/**
	 * Looks up hyponyms of the given word, assuming that it is used in its most
	 * common sense.
	 * 
	 * @param word a word
	 * @param pos its part of speech
	 * @return hyponyms or <code>null</code> if lookup failed
	 */
	public String[] getHyponyms(String word, POS pos) {
		Synset synset = getCommonSynset(word, pos);
		if (synset == null) return null;
		
		Synset[] hyponyms = getHyponymSynsets(synset);
		if (hyponyms == null) return null;
		
		return getLemmas(hyponyms);
	}
	
	/**
	 * Looks up hyponyms of the synset with the given POS and offset.
	 * 
	 * @param pos POS of the synset
	 * @param offset offset of the synset
	 * @return hyponyms or <code>null</code> if lookup failed
	 */
	public String[] getHyponyms(POS pos, long offset) {
		Synset synset = null;
		try {
			synset = dict.getSynsetAt(pos, offset);
		} catch (JWNLException e) {}
		if (synset == null) return null;
		
		Synset[] hyponyms = getHyponymSynsets(synset);
		if (hyponyms == null) return null;
		
		return getLemmas(hyponyms);
	}
	
	/**
	 * Looks up hyponyms of the synset with POS "noun" and the given offset.
	 * 
	 * @param offset offset of the synset
	 * @return hyponyms or <code>null</code> if lookup failed
	 */
	public String[] getNounHyponyms(long offset) {
		return getHyponyms(POS.NOUN, offset);
	}
	
	// get 'hyponym' synsets
	private Synset[] getHyponymSynsets(Synset synset) {
		PointerTargetNodeList hyponyms = null;
		try {
			hyponyms = PointerUtils.getInstance().getDirectHyponyms(synset);
		} catch (JWNLException e) {}
		if (hyponyms == null) return null;
		
		return getSynsets(hyponyms);
	}
	
	// relations for verbs
	
	/**
	 * Looks up verbs that entail the given verb, assuming that it is used in
	 * its most common sense.
	 * 
	 * @param verb a verb
	 * @return entailing verbs or <code>null</code> if lookup failed
	 */
	public String[] getEntailing(String verb) {
		Synset synset = getCommonSynset(verb, VERB);
		if (synset == null) return null;
		
		Synset[] entailing = getEntailingSynsets(synset);
		if (entailing == null) return null;
		
		return getLemmas(entailing);
	}
	
	// get 'entailing' synsets
	private Synset[] getEntailingSynsets(Synset synset) {
		PointerTargetNodeList entailing = null;
		try {
			entailing = PointerUtils.getInstance().getEntailments(synset);
		} catch (JWNLException e) {}
		if (entailing == null) return null;
		
		return getSynsets(entailing);
	}
	
	/**
	 * Looks up verbs that cause the given verb, assuming that it is used in its
	 * most common sense.
	 * 
	 * @param verb a verb
	 * @return causing verbs or <code>null</code> if lookup failed
	 */
	public String[] getCausing(String verb) {
		Synset synset = getCommonSynset(verb, VERB);
		if (synset == null) return null;
		
		Synset[] causing = getCausingSynsets(synset);
		if (causing == null) return null;
		
		return getLemmas(causing);
	}
	
	// get 'causing' synsets
	private Synset[] getCausingSynsets(Synset synset) {
		PointerTargetNodeList causing = null;
		try {
			causing = PointerUtils.getInstance().getCauses(synset);
		} catch (JWNLException e) {}
		if (causing == null) return null;
		
		return getSynsets(causing);
	}
	
	// relations for nouns
	
	/**
	 * Looks up member holonyms of the given noun, assuming that it is used in
	 * its most common sense.
	 * 
	 * @param noun a noun
	 * @return member holonyms or <code>null</code> if lookup failed
	 */
	public String[] getMembersOf(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] membersOf = getMemberOfSynsets(synset);
		if (membersOf == null) return null;
		
		return getLemmas(membersOf);
	}
	
	// get 'member-of' synsets
	private Synset[] getMemberOfSynsets(Synset synset) {
		PointerTargetNodeList membersOf = null;
		try {
			membersOf = PointerUtils.getInstance().getMemberHolonyms(synset);
		} catch (JWNLException e) {}
		if (membersOf == null) return null;
		
		return getSynsets(membersOf);
	}
	
	/**
	 * Looks up substance holonyms of the given noun, assuming that it is used in
	 * its most common sense.
	 * 
	 * @param noun a noun
	 * @return substance holonyms or <code>null</code> if lookup failed
	 */
	public String[] getSubstancesOf(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] substancesOf = getSubstanceOfSynsets(synset);
		if (substancesOf == null) return null;
		
		return getLemmas(substancesOf);
	}
	
	// get 'substance-of' synsets
	private Synset[] getSubstanceOfSynsets(Synset synset) {
		PointerTargetNodeList substancesOf = null;
		try {
			substancesOf = PointerUtils.getInstance().getSubstanceHolonyms(synset);
		} catch (JWNLException e) {}
		if (substancesOf == null) return null;
		
		return getSynsets(substancesOf);
	}
	
	/**
	 * Looks up part holonyms of the given noun, assuming that it is used in its
	 * most common sense.
	 * 
	 * @param noun a noun
	 * @return part holonyms or <code>null</code> if lookup failed
	 */
	public String[] getPartsOf(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] partsOf = getPartOfSynsets(synset);
		if (partsOf == null) return null;
		
		return getLemmas(partsOf);
	}
	
	// get 'part-of' synsets
	private Synset[] getPartOfSynsets(Synset synset) {
		PointerTargetNodeList partsOf = null;
		try {
			partsOf = PointerUtils.getInstance().getPartHolonyms(synset);
		} catch (JWNLException e) {}
		if (partsOf == null) return null;
		
		return getSynsets(partsOf);
	}
	
	/**
	 * Looks up member meronyms of the given noun, assuming that it is used in
	 * its most common sense.
	 * 
	 * @param noun a noun
	 * @return member meronyms or <code>null</code> if lookup failed
	 */
	public String[] getHaveMember(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] haveMember = getHasMemberSynsets(synset);
		if (haveMember == null) return null;
		
		return getLemmas(haveMember);
	}
	
	// get 'has-member' synsets
	private Synset[] getHasMemberSynsets(Synset synset) {
		PointerTargetNodeList haveMember = null;
		try {
			haveMember = PointerUtils.getInstance().getMemberMeronyms(synset);
		} catch (JWNLException e) {}
		if (haveMember == null) return null;
		
		return getSynsets(haveMember);
	}
	
	/**
	 * Looks up substance meronyms of the given noun, assuming that it is used in
	 * its most common sense.
	 * 
	 * @param noun a noun
	 * @return substance meronyms or <code>null</code> if lookup failed
	 */
	public String[] getHaveSubstance(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] haveSubstance = getHasSubstanceSynsets(synset);
		if (haveSubstance == null) return null;
		
		return getLemmas(haveSubstance);
	}
	
	// get 'has-substance' synsets
	private Synset[] getHasSubstanceSynsets(Synset synset) {
		PointerTargetNodeList haveSubstance = null;
		try {
			haveSubstance = PointerUtils.getInstance().getSubstanceMeronyms(synset);
		} catch (JWNLException e) {}
		if (haveSubstance == null) return null;
		
		return getSynsets(haveSubstance);
	}
	
	/**
	 * Looks up part meronyms of the given noun, assuming that it is used in its
	 * most common sense.
	 * 
	 * @param noun a noun
	 * @return part meronyms or <code>null</code> if lookup failed
	 */
	public String[] getHavePart(String noun) {
		Synset synset = getCommonSynset(noun, NOUN);
		if (synset == null) return null;
		
		Synset[] havePart = getHasPartSynsets(synset);
		if (havePart == null) return null;
		
		return getLemmas(havePart);
	}
	
	// get 'has-part' synsets
	private Synset[] getHasPartSynsets(Synset synset) {
		PointerTargetNodeList havePart = null;
		try {
			havePart = PointerUtils.getInstance().getPartMeronyms(synset);
		} catch (JWNLException e) {}
		if (havePart == null) return null;
		
		return getSynsets(havePart);
	}
	
	
	public Set<Synset> getSynsets(String word) throws JWNLException {
		Set<Synset> synsets = new HashSet<Synset>();
		IndexWordSet indexWordSet = dict.lookupAllIndexWords(word);
		IndexWord[] indexWords = indexWordSet.getIndexWordArray();
		for (IndexWord indexWord : indexWords) {
			Synset[] synsetArray = indexWord.getSenses();
			synsets.addAll(Arrays.asList(synsetArray));
		}
		return synsets;
	}
	
	private List<String> getSynonyms(Synset synset) {
		Word[] words = synset.getWords();
		List<String> synonyms = new ArrayList<String>(words.length);
		for (Word word : words) {
			synonyms.add(word.getLemma());
		}
		return synonyms;
	}
	
	private Set<String> getSynonyms(Collection<Synset> synsets) {
		Set<String> synonyms = new HashSet<String>();
		for (Synset synset : synsets) {
			synonyms.addAll(getSynonyms(synset));
		}
		return synonyms;
	}

	/**
	 * 获取word的所有同义词
	 *
	 * @param word
	 * @return
	 * @throws JWNLException
	 */
	public Set<String> getSynonyms(String word) throws JWNLException {
		Set<Synset> synsets = getSynsets(word);
		Set<String> synonyms = getSynonyms(synsets);
		return synonyms;
	}
	
	/**
	 * 获取word根据WordNet的type定义对应的词
	 *
	 * @param type
	 * @param word
	 * @return
	 */
	public Set<Synset> getPointers(PointerType type, String word) {
		Set<Synset> targets = new HashSet<Synset>();
		try {
			Set<Synset> synsets = getSynsets(word);
			for (Synset synset: synsets) {
				Pointer[] pointers = synset.getPointers(type);
				for (Pointer pointer: pointers) {
					Synset target = pointer.getTargetSynset();
					targets.add(target);
				}
			}
		} catch (JWNLException e) {
			e.printStackTrace();
		}	
		return targets;
	}
	
	/**
	 * 获取word对应的属性词，只作用于noun和adjective词性的词
	 * e.g. 输入length返回long, short
	 * e.g. 输入long返回length, duration
	 *
	 * @param word
	 * @return
	 */
	public Set<String> getAttributes(String word) {
		Set<Synset> targets = getPointers(PointerType.ATTRIBUTE, word);
		Set<String> atrributes = getSynonyms(targets);
		return atrributes;
	}
	
	// implement the interface 'Ontology'
	
	/**
	 * Looks up a word.
	 * 
	 * @param word the word to look up
	 * @return <code>true</code> iff the word was found
	 */
	public boolean contains(String word) {
//		// look for compound nouns and verbs
//		return isCompoundWord(word);
		// only look for compound nouns
		return isCompoundNoun(word);
	}
	
	/**
	 * Expands an event by looking up related events.
	 * 
	 * @param event an event
	 * @return related events and their weights
	 */
	public Map<String, Double> expandEvent(String event) {
		if (!isVerb(event)) return new Hashtable<String, Double>();
		
		// synsets of related concepts
		Map<Synset, Double> synsets = new Hashtable<Synset, Double>();
		// synsets that have already been expanded
		Map<Synset, Double> expanded = new Hashtable<Synset, Double>();
		
		// get most common synset
		double hurdle = MIN_EXPANSION_WEIGHT;
		if (SYNONYM_WEIGHT >= hurdle) {
			Synset synset = getCommonSynset(event, VERB);
			if (synset != null) synsets.put(synset, 1d);
		}
		
		// expand synsets
		int pathLength = 0;
		while (pathLength++ < MAX_PATH_LENGTH && synsets.size() > 0) {
			// get synsets and their weights
			Synset[] currSynsets =
				synsets.keySet().toArray(new Synset[synsets.size()]);
			double[] currWeights = new double[synsets.size()];
			for (int i = 0; i < synsets.size(); i++)
				currWeights[i] = synsets.get(currSynsets[i]);
			
			for (int i = 0; i < currSynsets.length; i++) {
				Synset synset = currSynsets[i];
				double weight = currWeights[i];
				
				// move to expanded synsets
				if (synsets.get(synset) == weight)
					synsets.remove(synset);
				if (!expanded.containsKey(synset) ||
						expanded.get(synset) < weight) {
					expanded.put(synset, weight);
				} else continue;
				
				// 'hypernym' relation
				double hypernymWeight = weight * HYPERNYM_WEIGHT;
				if (hypernymWeight >= hurdle) {
					Synset[] hypernyms = getHypernymSynsets(synset);
					for (Synset hypernym : hypernyms)
						if (!synsets.containsKey(hypernym) ||
								synsets.get(hypernym) < hypernymWeight)
						synsets.put(hypernym, hypernymWeight);
				}
				// 'hyponym' relation
				double hyponymWeight = weight * HYPONYM_WEIGHT;
				if (hyponymWeight >= hurdle) {
					Synset[] hyponyms = getHyponymSynsets(synset);
					for (Synset hyponym : hyponyms)
						if (!synsets.containsKey(hyponym) ||
								synsets.get(hyponym) < hyponymWeight)
						synsets.put(hyponym, hyponymWeight);
				}
				// 'entailing' relation
				double entailingWeight = weight * ENTAILING_WEIGHT;
				if (entailingWeight >= hurdle) {
					Synset[] entailing = getEntailingSynsets(synset);
					for (Synset entails : entailing)
						if (!synsets.containsKey(entails) ||
								synsets.get(entails) < entailingWeight)
						synsets.put(entails, entailingWeight);
				}
				// 'causing' relation
				double causingWeight = weight * CAUSING_WEIGHT;
				if (causingWeight >= hurdle) {
					Synset[] causing = getCausingSynsets(synset);
					for (Synset causes : causing)
						if (!synsets.containsKey(causes) ||
								synsets.get(causes) < causingWeight)
						synsets.put(causes, causingWeight);
				}
			}
		}
		
		for (Synset synset : synsets.keySet()) {
			double weight = synsets.get(synset);
			if (!expanded.containsKey(synset) ||
					expanded.get(synset) < weight)
				expanded.put(synset, weight);
		}
		
		// get concepts in synsets
		Map<String, Double> expansions = new Hashtable<String, Double>();
		for (Synset synset : expanded.keySet()) {
			double weight = expanded.get(synset);
			if (weight == 1) weight = SYNONYM_WEIGHT;  // direct synonyms
			for (String expansion : getLemmas(synset))
				if (!expansions.containsKey(expansion) ||
						expansions.get(expansion) < weight)
					expansions.put(expansion, weight);
		}
		List<String> dropped = new ArrayList<String>();
		for (String expansion : expansions.keySet())
			if (expansion.equalsIgnoreCase(event)) dropped.add(expansion);
		for (String expansion : dropped) expansions.remove(expansion);
		
		return expansions;
	}
	
	/**
	 * Expands an entity by looking up related entities.
	 * 
	 * @param entity an entity
	 * @return related entities and their weights
	 */
	public Map<String, Double> expandEntity(String entity) {
		if (!isCompoundNoun(entity)) return new Hashtable<String, Double>();
		
		// synsets of related concepts
		Map<Synset, Double> synsets = new Hashtable<Synset, Double>();
		// synsets that have already been expanded
		Map<Synset, Double> expanded = new Hashtable<Synset, Double>();
		
		// get most common synset
		double hurdle = MIN_EXPANSION_WEIGHT;
		if (SYNONYM_WEIGHT >= hurdle) {
			Synset synset = getCommonSynset(entity, NOUN);
			if (synset != null) synsets.put(synset, 1d);
		}
		
		// expand synsets
		int pathLength = 0;
		while (pathLength++ < MAX_PATH_LENGTH && synsets.size() > 0) {
			// get synsets and their weights
			Synset[] currSynsets =
				synsets.keySet().toArray(new Synset[synsets.size()]);
			double[] currWeights = new double[synsets.size()];
			for (int i = 0; i < synsets.size(); i++)
				currWeights[i] = synsets.get(currSynsets[i]);
			
			for (int i = 0; i < currSynsets.length; i++) {
				Synset synset = currSynsets[i];
				double weight = currWeights[i];
				
				// move to expanded synsets
				if (synsets.get(synset) == weight)
					synsets.remove(synset);
				if (!expanded.containsKey(synset) ||
						expanded.get(synset) < weight) {
					expanded.put(synset, weight);
				} else continue;
				
				// 'hypernym' relation
				double hypernymWeight = weight * HYPERNYM_WEIGHT;
				if (hypernymWeight >= hurdle) {
					Synset[] hypernyms = getHypernymSynsets(synset);
					for (Synset hypernym : hypernyms)
						if (!synsets.containsKey(hypernym) ||
								synsets.get(hypernym) < hypernymWeight)
						synsets.put(hypernym, hypernymWeight);
				}
				// 'hyponym' relation
				double hyponymWeight = weight * HYPONYM_WEIGHT;
				if (hyponymWeight >= hurdle) {
					Synset[] hyponyms = getHyponymSynsets(synset);
					for (Synset hyponym : hyponyms)
						if (!synsets.containsKey(hyponym) ||
								synsets.get(hyponym) < hyponymWeight)
						synsets.put(hyponym, hyponymWeight);
				}
				// 'member-of' relation
				double memberOfWeight = weight * MEMBER_OF_WEIGHT;
				if (memberOfWeight >= hurdle) {
					Synset[] membersOf = getMemberOfSynsets(synset);
					for (Synset memberOf : membersOf)
						if (!synsets.containsKey(memberOf) ||
								synsets.get(memberOf) < memberOfWeight)
						synsets.put(memberOf, memberOfWeight);
				}
				// 'substance-of' relation
				double substanceOfWeight = weight * SUBSTANCE_OF_WEIGHT;
				if (substanceOfWeight >= hurdle) {
					Synset[] substancesOf = getSubstanceOfSynsets(synset);
					for (Synset substanceOf : substancesOf)
						if (!synsets.containsKey(substanceOf) ||
								synsets.get(substanceOf) < substanceOfWeight)
						synsets.put(substanceOf, substanceOfWeight);
				}
				// 'part-of' relation
				double partOfWeight = weight * PART_OF_WEIGHT;
				if (partOfWeight >= hurdle) {
					Synset[] partsOf = getPartOfSynsets(synset);
					for (Synset partOf : partsOf)
						if (!synsets.containsKey(partOf) ||
								synsets.get(partOf) < partOfWeight)
						synsets.put(partOf, partOfWeight);
				}
				// 'has-member' relation
				double hasMemberWeight = weight * HAS_MEMBER_WEIGHT;
				if (hasMemberWeight >= hurdle) {
					Synset[] haveMember = getHasMemberSynsets(synset);
					for (Synset hasMember : haveMember)
						if (!synsets.containsKey(hasMember) ||
								synsets.get(hasMember) < hasMemberWeight)
						synsets.put(hasMember, hasMemberWeight);
				}
				// 'has-substance' relation
				double hasSubstanceWeight = weight * HAS_SUBSTANCE_WEIGHT;
				if (hasSubstanceWeight >= hurdle) {
					Synset[] haveSubstance = getHasSubstanceSynsets(synset);
					for (Synset hasSubstance : haveSubstance)
						if (!synsets.containsKey(hasSubstance) ||
								synsets.get(hasSubstance) < hasSubstanceWeight)
						synsets.put(hasSubstance, hasSubstanceWeight);
				}
				// 'has-part' relation
				double hasPartWeight = weight * HAS_PART_WEIGHT;
				if (hasPartWeight >= hurdle) {
					Synset[] havePart = getHasPartSynsets(synset);
					for (Synset hasPart : havePart)
						if (!synsets.containsKey(hasPart) ||
								synsets.get(hasPart) < hasPartWeight)
						synsets.put(hasPart, hasPartWeight);
				}
			}
		}
		
		for (Synset synset : synsets.keySet()) {
			double weight = synsets.get(synset);
			if (!expanded.containsKey(synset) ||
					expanded.get(synset) < weight)
				expanded.put(synset, weight);
		}
		
		// get concepts in synsets
		Map<String, Double> expansions = new Hashtable<String, Double>();
		for (Synset synset : expanded.keySet()) {
			double weight = expanded.get(synset);
			if (weight == 1) weight = SYNONYM_WEIGHT;  // direct synonyms
			for (String expansion : getLemmas(synset))
				if (!expansions.containsKey(expansion) ||
						expansions.get(expansion) < weight)
					expansions.put(expansion, weight);
		}
		List<String> dropped = new ArrayList<String>();
		for (String expansion : expansions.keySet())
			if (expansion.equalsIgnoreCase(entity)) dropped.add(expansion);
		for (String expansion : dropped) expansions.remove(expansion);
		
		return expansions;
	}
	
	/**
	 * Expands a modifier by looking up related modifiers.
	 * 
	 * @param modifier a modifier
	 * @param pos its part of speech: <code>POS.ADJECTIVE</code> or
	 *            <code>POS.ADVERB</code>
	 * @return related modifiers and their weights
	 */
	public Map<String, Double> expandModifier(String modifier, POS pos) {
		if ((pos.equals(ADJECTIVE) && !isAdjective(modifier)) ||
				(pos.equals(ADVERB) && !isAdverb(modifier)))
			return new Hashtable<String, Double>();
		
		// synsets of related concepts
		Map<Synset, Double> synsets = new Hashtable<Synset, Double>();
		// synsets that have already been expanded
		Map<Synset, Double> expanded = new Hashtable<Synset, Double>();
		
		// get most common synset
		double hurdle = MIN_EXPANSION_WEIGHT;
		if (SYNONYM_WEIGHT >= hurdle) {
			Synset synset = getCommonSynset(modifier, pos);
			if (synset != null) synsets.put(synset, 1d);
		}
		
		// expand synsets
		int pathLength = 0;
		while (pathLength++ < MAX_PATH_LENGTH && synsets.size() > 0) {
			// get synsets and their weights
			Synset[] currSynsets =
				synsets.keySet().toArray(new Synset[synsets.size()]);
			double[] currWeights = new double[synsets.size()];
			for (int i = 0; i < synsets.size(); i++)
				currWeights[i] = synsets.get(currSynsets[i]);
			
			for (int i = 0; i < currSynsets.length; i++) {
				Synset synset = currSynsets[i];
				double weight = currWeights[i];
				
				// move to expanded synsets
				if (synsets.get(synset) == weight)
					synsets.remove(synset);
				if (!expanded.containsKey(synset) ||
						expanded.get(synset) < weight) {
					expanded.put(synset, weight);
				} else continue;
				
				// currently no relations other than synonyms
			}
		}
		
		for (Synset synset : synsets.keySet()) {
			double weight = synsets.get(synset);
			if (!expanded.containsKey(synset) ||
					expanded.get(synset) < weight)
				expanded.put(synset, weight);
		}
		
		// get concepts in synsets
		Map<String, Double> expansions = new Hashtable<String, Double>();
		for (Synset synset : expanded.keySet()) {
			double weight = expanded.get(synset);
			if (weight == 1) weight = SYNONYM_WEIGHT;  // direct synonyms
			for (String expansion : getLemmas(synset))
				if (!expansions.containsKey(expansion) ||
						expansions.get(expansion) < weight)
					expansions.put(expansion, weight);
		}
		List<String> dropped = new ArrayList<String>();
		for (String expansion : expansions.keySet())
			if (expansion.equalsIgnoreCase(modifier)) dropped.add(expansion);
		for (String expansion : dropped) expansions.remove(expansion);
		
		return expansions;
	}
	
	/**
	 * Returns the similarity of two words.
	 *
	 * @param word1 The first word
	 * @param word2 The second word
	 * @return The similarity of the two words
	 */
	public double getSimilarity(String word1, String word2) {
		double ws = lin.max(word1, word2, "n");
		return ws;
	}
	
	public double getSimilarity(String word1, String word2, String pos) {
		double ws = lin.max(word1, word2, pos);
		return ws;
	}
	
	private double getSimilarityHalf(String[] sentence1, String[] sentence2) {
		if (sentence1 == null || sentence1.length == 0
				|| sentence2 == null || sentence2.length == 0) {
			return 0;
		}
		
		double ss = 0;
		for (int i = 0; i < sentence1.length; ++i) {
			double maxWs = 0;
			for (int j = 0; j < sentence2.length; ++j) {
				double ws = getSimilarity(sentence1[i], sentence2[j]);
				if (maxWs < ws) {
					maxWs = ws;
				}
			}
			ss += maxWs;
		}
		ss = ss / sentence1.length;
		return ss;
	}
	
	/**
	 * Returns the similarity of two sentences.
	 *
	 * @param sentence1 The first sentence
	 * @param sentence2 The second sentence
	 * @return The similarity of two sentences
	 */
	public double getSimilarity(String[] sentence1, String[] sentence2) {
		double ss = (getSimilarityHalf(sentence1, sentence2) 
				+ getSimilarityHalf(sentence2, sentence1)) / 2;
		return ss;
	}
	
	/**
	 * Returns the similarity of two sentences. Assumes the words in those
	 * sentences are separated by white spaces.
	 *
	 * @param sentence1 The first sentence
	 * @param sentence2 The second sentence
	 * @return The similarity of two sentences
	 */
	public double getSentencesSimilarity(String sentence1, String sentence2) {
		String[] words1 = sentence1.split("\\s+");
		String[] words2 = sentence2.split("\\s+");
		return getSimilarity(words1, words2);
	}
}

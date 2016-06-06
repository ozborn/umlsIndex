package edu.uab.ccts.nlp.umlsIndex;

public class Config {
	public static final String UMLS_CONCEPT_RETRIEVE_SQL_PATH = "sql/oracle/umlsConceptDefSelect.sql";
	public static final String UMLS_LUCENE_INDEX_DIR = "target/index.lucene";

	public static final String UMLS_WORD2TERM_INDEX_DIR = "target/word2term.lucene"; //Words in TermNames
	public static final String UMLS_TERM2CONCEPT_INDEX_DIR = "target/term2concept.lucene"; //Contains CUIs
}

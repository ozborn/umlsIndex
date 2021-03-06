package edu.uab.ccts.nlp.umlsIndex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class UmlsIndexWriter {
	private static final Logger LOG  = LoggerFactory.getLogger(UmlsIndexWriter.class);
	private String jdbcConnectString;
	
	Map<String,Set<String>> word2term = new HashMap<String,Set<String>>(500000);

	/**
	 * Write a simple UMLS Indexer
	 * @param args
	 */
	public static void main(String args[]){
		UmlsIndexWriter uiw = new UmlsIndexWriter(args[0]);
		uiw.buildIndex();
	}

	public UmlsIndexWriter(String umlsDbString) { this.jdbcConnectString = umlsDbString; }

	public void buildIndex(){
		LOG.info("jdbcString:\n"+jdbcConnectString);
		String fetchsql = getTextFromFile(Config.UMLS_CONCEPT_RETRIEVE_SQL_PATH);
		try(Connection con = DriverManager.getConnection(jdbcConnectString)){
			Statement st = con.createStatement();
			ResultSet rs = st.executeQuery(fetchsql);
			//StandardAnalyzer converts to lowercase and removes stop words
			StandardAnalyzer analyzer = new StandardAnalyzer();
			IndexWriterConfig tconfig = new IndexWriterConfig(analyzer);
			IndexWriterConfig cconfig = new IndexWriterConfig(analyzer);
			FSDirectory wordIndex = FSDirectory.open(Paths.get(Config.UMLS_WORD2TERM_INDEX_DIR));
			FSDirectory termIndex = FSDirectory.open(Paths.get(Config.UMLS_TERM2CONCEPT_INDEX_DIR));
			IndexWriter termW = new IndexWriter(wordIndex, tconfig);
			IndexWriter conW = new IndexWriter(termIndex, cconfig);

			//Foreach concept
			while(rs.next()){
				String cui = rs.getString(1);
				String[] termnames = rs.getString(2).split("__");
				String[] stypes = rs.getString(3).split("__");
				HashSet<String> stset = new HashSet<String>(Arrays.asList(stypes));
				StringBuilder commaSTs=new StringBuilder();
				for(String ststring : stset) { 
					commaSTs.append(ststring);  commaSTs.append(",");
				}
				commaSTs.deleteCharAt(commaSTs.length()-1);
				HashSet<String> termset = new HashSet<String>(Arrays.asList(termnames));
				//Foreach term/synonym
				for(String tnames : termset) {
					LOG.debug("Dealing with concept name:"+tnames);
					//Tokenize and underscore to reflect what Lucene does and 
					List<String> tokens = tokenizeString(analyzer, tnames);
					List<String> cleantokens = cleanText(tokens);
					List<String> nostops = dropStopWords(cleantokens);
					StringBuilder sb = new StringBuilder();
					for(int i=0;i<nostops.size();i++){
						String tok = nostops.get(i);
						LOG.debug("Dealing with non-stop token:"+tok);
						if(i<nostops.size()-1) { sb.append(tok); sb.append("_"); }
						else sb.append(tok);
					}
					String officialLuceneTerm = sb.toString();
					addConceptDoc(conW, cui,officialLuceneTerm,commaSTs.toString());
					//addTermDoc(termW, officialLuceneTerm,nostops);
					List<String> words = stemWords(nostops);
					addWord2Term(officialLuceneTerm,words);

				}
				rs.next();
			}
			conW.close();
			termIndex.close();
			rs.close();

			for(Iterator<String> it = word2term.keySet().iterator();it.hasNext();){
				String word = it.next();
				Document doc = new Document();
				doc.add(new TextField("word", word, Field.Store.YES));
				Set<String> conceptTexts = word2term.get(word);
				StringBuilder sb = new StringBuilder();
				for(String ctext : conceptTexts) {
					sb.append(ctext); sb.append(" ");
				}
				StoredField strField = new StoredField("conceptText", sb.toString());
				doc.add(strField);
				LOG.debug("Concept text after adding all that was:"+doc.get("conceptText"));
				LOG.info("Adding document for "+word+" with "+conceptTexts.size()+" entries");
				termW.addDocument(doc);
				it.remove();
				
			}
			termW.close();
			wordIndex.close();
		} catch (Exception e) { e.printStackTrace(); }
	}



	private String getTextFromFile(String path) {
		StringBuilder sb = new StringBuilder();
		File test = new File(path);
		InputStream in = null;
		try {
			if (test.exists() && test.isFile() && test.canRead()) {
				in = new FileInputStream(path);
			} else {
				in = this.getClass().getClassLoader().getResourceAsStream(path);
			}
			try(BufferedReader br = new BufferedReader(new InputStreamReader(in))){
				while(br.ready()) {
					sb.append(br.readLine()+"\n");
				}
			} catch (Exception e) { e.printStackTrace(); }
		} catch (Exception e) { e.printStackTrace();}
		return sb.toString();
	}


	/**
	 * A text field is a sequence of terms that has been tokenized while a string 
	field is a single term (although it can also be multivalued.)

Punctuation and spacing is ignored for text fields. Text tends to be 
lowercased, stemmed, and even stop words removed. You tend to search text 
using a handful of keywords whose exact order is not required, although 
quoted phrases can be used as well. Fuzzy queries can be done on individual 
terms (words). Wildcards as well.

String fields are literal character strings with all punctuation, spacing, 
and case preserved. Anything other than exact match is done using wildcards, 
although I suppose fuzzy query should work as well.

String fields are useful for facets and filter queries or display.

Text fields are useful for keyword search.
	 * 
	 * @param w
	 * @param conceptUnderscoredText
	 * @param nostops
	 * @param tokenized
	 * @throws IOException
	 */
	private void addTermDoc(IndexWriter w, String conceptUnderscoredText, List<String> nostops) throws IOException {
		Document doc = new Document();
		for(String tword : nostops) {
			doc.add(new TextField("word", tword, Field.Store.YES));
			StoredField strField = new StoredField("conceptText", conceptUnderscoredText);
			doc.add(strField);
			LOG.info("Adding to words:"+tword+" with concept name:"+conceptUnderscoredText);
			w.addDocument(doc);
		}
	}


	private void addConceptDoc(IndexWriter w, String cui, String conceptUnderscoredText, String semantic_type) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("concept", conceptUnderscoredText, Field.Store.YES));
		StoredField strField = new StoredField("cui", cui);
		doc.add(strField);
		//doc.add(new StringField("sty", semantic_type, Field.Store.YES));
		StoredField styField = new StoredField("sty", semantic_type);
		doc.add(styField);
		LOG.debug("Adding to concepts:"+conceptUnderscoredText+" with cui:"+cui+" with types:"+semantic_type);
		w.addDocument(doc);
	}


	public static List<String> tokenizeString(Analyzer analyzer, String string) {
		List<String> result = new ArrayList<String>();
		try (TokenStream stream  = analyzer.tokenStream(null, new StringReader(string))){
			stream.reset();
			while (stream.incrementToken()) {
				result.add(stream.getAttribute(CharTermAttribute.class).toString());
			}
			stream.end();
		} catch (IOException e) {
			// not thrown b/c we're using a string reader...
			throw new RuntimeException(e);
		}
		return result;
	}


	/**
	 * May not be needed depending on which stopwords Lucenes uses (FIXME)
	 * @param allWords
	 * @return
	 * @throws URISyntaxException 
	 */
	public List<String> dropStopWords(List<String> allWords) throws URISyntaxException{
		List<String> stops = new ArrayList<String>();
		try (Stream<String> stream = Files.lines(Paths.get(getClass().getResource("/StopWords.txt").toURI()))) {
		//try (Stream<String> stream = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream("StopWords.txt"))).lines()){
			stops = stream
			.filter(line -> !line.startsWith("#"))
			.collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		allWords.removeAll(stops);
		return allWords;
	}


	/**
	 * FIXME - Stem words, use either CTAKES or Noble Porter Stemmer?
	 * @param allWords
	 * @return
	 */
	private List<String> stemWords(List<String> allWords){
		return new ArrayList<String>(allWords);

	}
	
	
	private void addWord2Term(String concept, List<String> tokens) {
		for(String tok : tokens) {
			Set<String> s = word2term.get(tok);
			if(s==null) s = new HashSet<String>();
			s.add(concept);
			word2term.put(tok, s);
			LOG.debug("Added/Updated "+tok+" with "+s.size()+" concepts");
		}
	}
	
	
	private List<String> cleanText(List<String> input) {
		for(String dirty : input) {
			dirty.replaceAll(":", "_");
		}
		return input;
	}


}

package edu.uab.ccts.nlp.umlsIndex;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultiset;

public class SearchClient {
	private IndexSearcher word2termSearcher = null;
	private IndexSearcher term2conceptSearcher = null;
	private QueryParser wordParser = null;
	private QueryParser termParser = null;
	private static final Logger LOG  = LoggerFactory.getLogger(SearchClient.class);

	public static void main(String[] args) throws Exception {
		File f = new File(Config.UMLS_WORD2TERM_INDEX_DIR);
		if(!(f.isDirectory() && f.list().length>0)) { System.err.println("Missing "+f); }
		
		SearchClient sc = new SearchClient();

		TopDocs td = sc.performSearch(sc.getWordParser(),sc.getWord2termSearcher(),args[0], 100);
		ScoreDoc[] hits = td.scoreDocs;
		LOG.info(args[0]+ "has " + hits.length+" word hits");
		HashMultiset<String> searchsummary = sc.doIndexSearch(hits);
		System.out.println("Got back "+searchsummary.elementSet().size()+" results from "+args[0]);
		for(String s : searchsummary.elementSet()) {
			System.out.println(s);
		}
	}
	
	
	public SearchClient() throws IOException
	{
		word2termSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(Config.UMLS_WORD2TERM_INDEX_DIR))));
		term2conceptSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(Config.UMLS_TERM2CONCEPT_INDEX_DIR))));
		wordParser = new QueryParser("word", new StandardAnalyzer());
		termParser = new QueryParser("concept", new StandardAnalyzer());
	}

	
	
	public HashMultiset<String> doIndexSearch(ScoreDoc[] hits) throws IOException, ParseException {
		HashMultiset<String> searchsummary = HashMultiset.create();
		for (int i = 0; i < hits.length; i++) {
	        Document hitDoc = word2termSearcher.doc(hits[i].doc);
	        String theword = hitDoc.get("word");
	        String allconcept_text = hitDoc.get("conceptText");
	        String[] allcons = allconcept_text.split(" ");
	        LOG.debug(theword+" id:"+hits[i].doc+" with score:"+hits[i].score
	        +" is associated with "+allcons.length+" concepts, see::"+allconcept_text);

	        HashMultiset<String> hitsummary = HashMultiset.create();
	        for(int j=0;j<allcons.length;j++) {
	        	TopDocs topcons = performSearch(termParser,term2conceptSearcher,allcons[j], 1);
	        	ScoreDoc[] conhits = topcons.scoreDocs;
	        	Document conDoc = term2conceptSearcher.doc(conhits[0].doc);
	        	String cui = conDoc.get("cui");
	        	LOG.debug(conDoc.get("concept")+" with concept score:"+
	        	conhits[0].score+" and cui: "+cui);
	        	hitsummary.add(cui);
	        }
	        if(searchsummary.size()==0) searchsummary.addAll(hitsummary);
	        else {
	        	searchsummary.retainAll(hitsummary);
	        }
	      }
		return searchsummary;
	}
	
	
	public TopDocs performSearch(QueryParser qp, IndexSearcher is, String queryString, int n)
			throws IOException, ParseException {
		Query query = qp.parse(queryString);
		return is.search(query, n);
	}
	
	
	public Document getDocument(int docId)
			throws IOException {
		return word2termSearcher.doc(docId);
	}


	public IndexSearcher getWord2termSearcher() {
		return word2termSearcher;
	}


	public void setWord2termSearcher(IndexSearcher word2termSearcher) {
		this.word2termSearcher = word2termSearcher;
	}


	public QueryParser getWordParser() {
		return wordParser;
	}


	public void setWordParser(QueryParser wordParser) {
		this.wordParser = wordParser;
	}


	

}

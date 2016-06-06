package edu.uab.ccts.nlp.umlsIndex.test.integration;


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

import com.google.common.collect.HashMultiset;

import edu.uab.ccts.nlp.umlsIndex.Config;


/**
 * Unit test for simple App.
 */
public class LuceneIndexIT 
{
	private IndexSearcher word2termSearcher = null;
	private IndexSearcher term2conceptSearcher = null;
	private QueryParser wordParser = null;
	private QueryParser termParser = null;
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public LuceneIndexIT() throws IOException
	{
		word2termSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(Config.UMLS_WORD2TERM_INDEX_DIR))));
		term2conceptSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(Config.UMLS_TERM2CONCEPT_INDEX_DIR))));
		wordParser = new QueryParser("word", new StandardAnalyzer());
		termParser = new QueryParser("concept", new StandardAnalyzer());
	}

	
	/**
	 */
	@org.junit.Test
	public void testIndex() throws Exception
	{
		TopDocs td = performSearch(wordParser,word2termSearcher,"multiple ulcers", 100);
		ScoreDoc[] hits = td.scoreDocs;
		System.out.println("Number of hits: " + hits.length);
		HashMultiset<String> searchsummary = HashMultiset.create();
	    for (int i = 0; i < hits.length; i++) {
	        Document hitDoc = word2termSearcher.doc(hits[i].doc);
	        String theword = hitDoc.get("word");
	        String allconcept_text = hitDoc.get("conceptText");
	        String[] allcons = allconcept_text.split(" ");
	        System.out.println(theword+" id:"+hits[i].doc+" with score:"+hits[i].score
	        +" is associated with "+allcons.length+" concepts, see::"+allconcept_text);

	        HashMultiset<String> hitsummary = HashMultiset.create();
	        for(int j=0;j<allcons.length;j++) {
	        	TopDocs topcons = performSearch(termParser,term2conceptSearcher,allcons[j], 1);
	        	ScoreDoc[] conhits = topcons.scoreDocs;
	        	Document conDoc = term2conceptSearcher.doc(conhits[0].doc);
	        	String cui = conDoc.get("cui");
	        	System.out.println(conDoc.get("concept")+" with concept score:"+
	        	conhits[0].score+" and cui: "+cui);
	        	hitsummary.add(cui);
	        }
	        searchsummary.addAll(hitsummary);
	      }
          assert(searchsummary.contains("C1265815"));
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
}

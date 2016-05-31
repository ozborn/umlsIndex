package edu.uab.ccts.nlp.umlsIndex.test.integration;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;
import org.junit.Test;

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


/**
 * Unit test for simple App.
 */
public class LuceneIndexIT 
{
	private IndexSearcher searcher = null;
	private QueryParser parser = null;
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public LuceneIndexIT() throws IOException
	{
		searcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get("target/index.lucene"))));
		parser = new QueryParser("stemmedTerms", new StandardAnalyzer());
	}

	
	/**
	 */
	@org.junit.Test
	public void testIndex() throws Exception
	{
		TopDocs td = performSearch("cancer", 2);
		ScoreDoc[] hits = td.scoreDocs;
		System.out.println("Number of hits: " + hits.length);
	    for (int i = 0; i < hits.length; i++) {
	        Document hitDoc = searcher.doc(hits[i].doc);
	        System.out.println(hitDoc.get("stemmedTerms"));
	        System.out.println(hitDoc.get("cui"));
	        System.out.println(hitDoc.get("sty"));
	        //assertEquals("C0814136", hitDoc.get("cui"));
	      }
	}

	public TopDocs performSearch(String queryString, int n)
			throws IOException, ParseException {
		Query query = parser.parse(queryString);
		return searcher.search(query, n);
	}

	public Document getDocument(int docId)
			throws IOException {
		return searcher.doc(docId);
	}
}

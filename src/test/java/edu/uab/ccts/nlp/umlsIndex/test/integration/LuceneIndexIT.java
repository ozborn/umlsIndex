package edu.uab.ccts.nlp.umlsIndex.test.integration;

import static org.junit.Assert.*;

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
		word2termSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get("target/word2concept.lucene"))));
		term2conceptSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get("target/term2concept.lucene"))));
		wordParser = new QueryParser("word", new StandardAnalyzer());
		termParser = new QueryParser("concept", new StandardAnalyzer());
	}

	
	/**
	 */
	@org.junit.Test
	public void testIndex() throws Exception
	{
		TopDocs td = performSearch(wordParser,"mucocutaneous ulcers", 20);
		ScoreDoc[] hits = td.scoreDocs;
		System.out.println("Number of hits: " + hits.length);
	    for (int i = 0; i < hits.length; i++) {
	        Document hitDoc = word2termSearcher.doc(hits[i].doc);
	        System.out.println(hitDoc.get("stemmedTerms"));
	        System.out.println(hitDoc.get("cui")+" with score:"+hits[i].score);
	        System.out.println(hitDoc.get("sty"));
	        //assertEquals("C0814136", hitDoc.get("cui"));
	      }
	}

	public TopDocs performSearch(QueryParser qp,String queryString, int n)
			throws IOException, ParseException {
		Query query = qp.parse(queryString);
		return word2termSearcher.search(query, n);
	}

	public Document getDocument(int docId)
			throws IOException {
		return word2termSearcher.doc(docId);
	}
}

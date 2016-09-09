package edu.uab.ccts.nlp.umlsIndex.test.integration;


import java.io.IOException;
import java.io.File;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import com.google.common.collect.HashMultiset;

import edu.uab.ccts.nlp.umlsIndex.Config;
import edu.uab.ccts.nlp.umlsIndex.SearchClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit test for simple App.
 */
public class LuceneIndexIT 
{
	
	SearchClient searchClient = null;

	private static final Logger LOG  = LoggerFactory.getLogger(LuceneIndexIT.class);
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 */
	public LuceneIndexIT() throws IOException
	{
		searchClient = new SearchClient();
	}

	
	/**
	 */
	@org.junit.Test
	public void testIndex() throws Exception
	{
		File f = new File(Config.UMLS_WORD2TERM_INDEX_DIR);
		org.junit.Assume.assumeTrue(!(f.isDirectory() && f.list().length>0));

		TopDocs td = searchClient.performSearch(searchClient.getWordParser(),
		searchClient.getWord2termSearcher(),"multiple ulcers", 100);

		ScoreDoc[] hits = td.scoreDocs;
		LOG.info("Number of hits: " + hits.length);
		HashMultiset<String> searchsummary = searchClient.doIndexSearch(hits);
        assert(searchsummary.contains("C1265815"));
	}



	
}

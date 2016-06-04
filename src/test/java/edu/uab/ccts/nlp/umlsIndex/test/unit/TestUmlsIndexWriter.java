package edu.uab.ccts.nlp.umlsIndex.test.unit;

import static org.junit.Assert.*;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.uab.ccts.nlp.umlsIndex.UmlsIndexWriter;

public class TestUmlsIndexWriter {
	
	@Test
	public void testDropStopWords() throws URISyntaxException{
		List<String> testlist = new ArrayList<String>();
		testlist.add("and");
		testlist.add("cancer");
		assertEquals(2,testlist.size());
		UmlsIndexWriter uiw = new UmlsIndexWriter("fake");
		List<String> fixed = uiw.dropStopWords(testlist);
		assertEquals(1,fixed.size());
		
	}

}

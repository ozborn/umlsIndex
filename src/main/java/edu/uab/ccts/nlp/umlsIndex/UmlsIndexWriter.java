package edu.uab.ccts.nlp.umlsIndex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UmlsIndexWriter {
	private static final Logger LOG  = LoggerFactory.getLogger(UmlsIndexWriter.class);
	private String jdbcConnectString;
	
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
			StandardAnalyzer analyzer = new StandardAnalyzer();
			FSDirectory index = FSDirectory.open(Paths.get("target/index.lucene"));
			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			IndexWriter w = new IndexWriter(index, config);
			while(rs.next()){
				String cui = rs.getString(1);
				String termText = rs.getString(2);
				String semanticType = rs.getString(3);
				addDoc(w, cui,termText,semanticType);
				rs.next();
			}
			w.close();
			rs.close();
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
	 * @param w
	 * @param title
	 * @param isbn
	 * @throws IOException
	 */
	private void addDoc(IndexWriter w, String cui, String raw_text, String semantic_type) throws IOException {
		Document doc = new Document();
		String stemmedTerms = stemText(raw_text);
		doc.add(new TextField("stemmedTerms", stemmedTerms, Field.Store.YES));
		doc.add(new StringField("cui", cui, Field.Store.YES));
		doc.add(new StringField("sty", semantic_type, Field.Store.YES));
		w.addDocument(doc);
	}

	private String stemText(String raw_text) { return raw_text; }

}

package com.tenode.baleen.extra.annotators.relationships;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.junit.Test;

import com.tenode.baleen.extra.annotators.relationships.PatternExtractor;

import uk.gov.dstl.baleen.annotators.testing.AnnotatorTestBase;
import uk.gov.dstl.baleen.types.language.Pattern;
import uk.gov.dstl.baleen.types.language.Sentence;
import uk.gov.dstl.baleen.types.language.WordToken;
import uk.gov.dstl.baleen.types.semantic.Entity;

public class PatternExtractorTest extends AnnotatorTestBase {

	private AnalysisEngine ae;

	@Override
	public void beforeTest() throws UIMAException {
		super.beforeTest();

		AnalysisEngineDescription desc = AnalysisEngineFactory.createEngineDescription(PatternExtractor.class);

		ae = AnalysisEngineFactory.createEngine(desc);
	}

	@Test
	public void testProcess() throws AnalysisEngineProcessException {
		String text = "The fox jumps over the dog.";
		jCas.setDocumentText(text);

		Sentence sentence = new Sentence(jCas);
		sentence.setBegin(0);
		sentence.setEnd(text.length());
		sentence.addToIndexes(jCas);

		int offset = 0;
		while (offset < text.length()) {
			int end = text.indexOf(" ", offset);
			if (end == -1) {
				end = text.indexOf(".", offset);
			}

			if (end > 0) {
				WordToken wordToken = new WordToken(jCas);
				wordToken.setBegin(offset);
				wordToken.setEnd(end);
				wordToken.addToIndexes(jCas);
				offset = end + 1;
			} else {
				offset = text.length();
			}
		}

		Entity fox = new Entity(jCas);
		fox.setBegin(4);
		fox.setEnd(7);
		fox.addToIndexes(jCas);

		Entity dog = new Entity(jCas);
		dog.setBegin(23);
		dog.setEnd(26);
		dog.addToIndexes(jCas);

		SimplePipeline.runPipeline(jCas, ae);

		Collection<Pattern> patterns = JCasUtil.select(jCas, Pattern.class);
		assertEquals(1, patterns.size());

		Pattern p = patterns.iterator().next();
		assertEquals(1, p.getWords().size());
		assertEquals("jumps", p.getWords(0).getCoveredText());

	}

}
package de.julielab.semedico.ae;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.julielab.jcore.types.Header;
import de.julielab.jcore.types.Token;

public class TokenSplitter extends JCasAnnotator_ImplBase {
	private static final Logger log = LoggerFactory.getLogger(TokenSplitter.class);
	private List<Token> toRemove = new ArrayList<>(200);
	private List<Token> newTokens = new ArrayList<>(100);
	private long tokenCreationTime = 0;
	private long tokenRemovalTime = 0;
	private long tokenAddingTime = 0;
	private long listClearingTime = 0;

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		FSIterator<Annotation> it = aJCas.getAnnotationIndex(Token.type).iterator();
		if (!it.hasNext())
			return;
		long time = System.currentTimeMillis();
		while (it.hasNext()) {
			Token t = (Token) it.next();
			if (t.getBegin() < 0 || t.getEnd() < 0 || t.getEnd() > aJCas.getDocumentText().length() - 1) {
				FSIterator<Annotation> iterator = aJCas.getAnnotationIndex(Header.type).iterator();
				String docId = "<unknown>";
				if (iterator.hasNext()) {
					Header h = (Header) iterator.next();
					docId = h.getDocId();
				}
				log.debug("A token of document {} has begin={} and end={} (document length {}); skipping this token",
						new Object[] { docId, t.getBegin(), t.getEnd(), aJCas.getDocumentText().length() });

				continue;
			}
			String ttext = t.getCoveredText();
			String splitString = "-";
			if (!ttext.equals("-") && ttext.contains(splitString)) {
				toRemove.add(t);
				int pos = splitString.length() * -1;
				int start = t.getBegin();
				while ((pos = ttext.indexOf(splitString, pos + splitString.length())) != -1) {
					if (pos > 0)
						newTokens.add(new Token(aJCas, start, start + pos));
					newTokens.add(new Token(aJCas, start + pos, start + pos + splitString.length()));
					start = start + pos + splitString.length();
				}
				if (start < t.getEnd()) {
					newTokens.add(new Token(aJCas, start, t.getEnd()));
				}
				// if (log.isDebugEnabled()) {
				// StringBuilder sb = new StringBuilder();
				// for (Token nt : newTokens)
				// sb.append(nt.getCoveredText()).append(", ");
				// sb.delete(sb.length() - 2, sb.length() - 1);
				// log.debug("Old token: {}, new Tokens: {}",
				// t.getCoveredText(), sb.toString());
				// }
			}
		}
		time = System.currentTimeMillis() - time;
		tokenCreationTime += time;

		log.trace("Removing {} tokens, adding {} split tokens.", toRemove.size(), newTokens.size());
		time = System.currentTimeMillis();
		for (Token t : toRemove)
			t.removeFromIndexes();
		time = System.currentTimeMillis() - time;
		tokenRemovalTime += time;

		time = System.currentTimeMillis();
		for (Token t : newTokens)
			t.addToIndexes();
		time = System.currentTimeMillis() - time;
		tokenAddingTime += time;

		time = System.currentTimeMillis();
		toRemove.clear();
		newTokens.clear();
		time = System.currentTimeMillis() - time;
		listClearingTime += time;
	}

	@Override
	public void collectionProcessComplete() throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		log.debug("{}: {}", "token creation time", tokenCreationTime);
		log.debug("{}: {}", "token removal time", tokenRemovalTime);
		log.debug("{}: {}", "token adding time", tokenAddingTime);
		log.debug("{}: {}", "list clearing time", listClearingTime);
	}

}

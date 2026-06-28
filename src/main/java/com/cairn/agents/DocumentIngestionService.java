package com.cairn.agents;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * WHY: This service acts as the ETL (Extract, Transform, Load) pipeline for our RAG setup. It takes
 * raw files (PDFs, TXT, etc.), extracts text using Tika, splits them into semantic chunks so we
 * don't blow past the LLM's context window, and stores those chunks in PgVector.
 */
@Service
public class DocumentIngestionService {

  private final VectorStore vectorStore;

  // WHY: We use a sensible default token splitter configuration.
  // This breaks large documents into smaller semantic chunks that fit inside
  // an LLM's context window.
  private final TokenTextSplitter splitter = new TokenTextSplitter();

  public DocumentIngestionService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  public int ingest(Resource resource) {
    TikaDocumentReader reader = new TikaDocumentReader(resource);
    List<Document> rawDocuments = reader.get();
    List<Document> chunkedDocuments = splitter.apply(rawDocuments);
    vectorStore.add(chunkedDocuments);
    return chunkedDocuments.size();
  }
}

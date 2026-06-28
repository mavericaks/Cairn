# Epic 4: The RAG Pipeline — Walkthrough

## What We Built

We successfully implemented **Retrieval-Augmented Generation (RAG)** into the Cairn agent swarm. This capability allows our agents to ground their LLM responses in factual data extracted from uploaded documents, solving the hallucination problem inherent to LLMs.

To adhere to the user's requirement to accelerate development and complete the Epic in one shot, all tasks (E4-T1 through E4-T5) were executed sequentially on the `feat/epic4-rag-pipeline` branch.

### 1. The VectorStore Adapter (Bridging the gap)
In Epic 2, we built `LocalEmbeddingService` to run standard CPU embeddings (MiniLM) using DJL at zero cost. To plug this into Spring AI's powerful RAG abstractions without refactoring our core, we implemented the **Adapter Pattern**.
- `LocalEmbeddingModelAdapter` implements Spring AI's `EmbeddingModel` interface but delegates the actual math to our DJL engine.
- `VectorStoreConfig` initializes Spring AI's native `PgVectorStore`, pointing it at our existing PostgreSQL database and injecting our custom adapter.

### 2. Document Ingestion Pipeline (ETL)
We exposed a new REST endpoint, `POST /api/v1/documents/upload`, which accepts `MultipartFile` uploads (PDF, TXT, DOCX).
- **Extraction:** We integrated `spring-ai-tika-document-reader` to crack open PDFs and extract raw text.
- **Chunking:** We use `TokenTextSplitter` to break massive documents into smaller semantic chunks (default ~800 tokens) with overlap, preventing context loss at boundaries.
- **Storage:** The chunks are passed to the `VectorStore`, which automatically calls our DJL adapter to vectorize them, and then stores them in the `vector_store` table in pgvector.

### 3. Context Injection (The RAG Core)
We updated `AbstractDomainAgent` to automatically retrieve context before building the LLM prompt.
- `retrieveContext(String query)`: Performs a cosine similarity search against the VectorStore to find the top 3 most relevant document chunks based on the user's message.
- These chunks are injected directly into the prompt template: `System: ... \n\n Context: ... \n\n User: ...`.

### 4. HyDE (Hypothetical Document Embeddings)
To optimize search accuracy for the `DiscoveryAgent`, we implemented **HyDE**. 
- Instead of embedding the user's short query (e.g., "how do I reset password?"), the `DiscoveryAgent` intercepts the query and asks the `ChatClient` to generate a hypothetical, hallucinated answer.
- We then embed that *hallucinated answer* and search the vector store with it. Because the hallucination is semantically shaped like a real document, it matches the actual documentation chunks in pgvector far more accurately than the short query would have.

## How to Test

1. **Start the Infrastructure**
   ```bash
   docker-compose up -d
   ```

2. **Upload a Document**
   Send a POST request with a file containing a specific fact:
   ```bash
   curl -F "file=@secret.txt" http://localhost:8080/api/v1/documents/upload
   ```
   *Expected:* Returns "Successfully ingested document into N chunks."

3. **Query the Agent**
   Send a chat request asking about the secret fact. The agent will retrieve the injected context and provide a fact-based answer, proving the RAG pipeline is actively grounding the model.

## Git Hygiene

This Epic was executed on `feat/epic4-rag-pipeline` per **Rule 17**. A Pull Request should be created and merged to `main` via the GitHub UI to ensure CI gates the code properly.

# Domain Data Strategy & Mechanics

This document outlines exactly how each of the 6 domains operates, what data is required to train their specific LoRA models (Behavior), what data is stored in `pgvector` (Facts), and the examples used to route requests to them.

---

## 1. Discovery Domain
* **Purpose:** Act as the internal search engine for the user's uploaded documents.
* **Semantic Routing Examples (stored in pgvector):** 
  - *"What is the company policy on remote work?"*
  - *"Find the documentation for the API."*
  - *"Search my files for the Q3 financial report."*
* **RAG Data (Facts in pgvector):** PDF documents, Markdown wiki pages, Word documents uploaded by the user.
* **LoRA Fine-Tuning Data (Behavior):** Trained on 500 examples of reading raw text chunks and outputting concise, bulleted summaries with citations. 
  - *Training Goal:* Forcing the LLM to never hallucinate answers outside of the provided context.

## 2. Analytical Domain
* **Purpose:** Convert natural language into complex data queries and analyze tabular data.
* **Semantic Routing Examples:**
  - *"How many users signed up last month?"*
  - *"Give me a breakdown of revenue by region."*
  - *"What is the average latency of our API?"*
* **RAG Data (Facts):** Database schemas (e.g., table names, column names, foreign keys) and CSV file headers.
* **LoRA Fine-Tuning Data (Behavior):** Trained on 1,000 examples of Natural Language to SQL/JSON. 
  - *Training Goal:* The LLM must output strictly valid SQL or JSON visualization configs without any conversational filler like *"Sure, here is your SQL query:"*.

## 3. Execution Domain
* **Purpose:** Take autonomous action on the user's behalf via tools.
* **Semantic Routing Examples:**
  - *"Restart the production server."*
  - *"Send an email to the marketing team."*
  - *"Deploy the latest code to staging."*
* **RAG Data (Facts):** A library of allowed Bash scripts, API documentation, and tool descriptions.
* **LoRA Fine-Tuning Data (Behavior):** Trained heavily on Function Calling (Tool Use).
  - *Training Goal:* The model must correctly map user intent to JSON tool schemas and know when to ask for Human-In-The-Loop approval before doing something dangerous.

## 4. Generative Domain
* **Purpose:** Create net-new content like code scaffolds, images, or template documents.
* **Semantic Routing Examples:**
  - *"Create a Spring Boot controller for user management."*
  - *"Generate an image of a futuristic city."*
  - *"Write a boilerplate React component."*
* **RAG Data (Facts):** Corporate design guidelines, coding standards, and project templates.
* **LoRA Fine-Tuning Data (Behavior):** Trained on producing high-quality, formatted markdown output, code blocks, and adhering strictly to specific structural templates.

## 5. Conversational Domain
* **Purpose:** Empathy, small talk, and maintaining session continuity.
* **Semantic Routing Examples:**
  - *"Hello, how are you today?"*
  - *"That's a very helpful answer, thank you."*
  - *"I'm feeling frustrated with my code."*
* **RAG Data (Facts):** None! This domain relies entirely on the Redis `DomainContextCacheService` to remember the last 10 things the user said.
* **LoRA Fine-Tuning Data (Behavior):** Trained on empathetic, human-like dialogue. 
  - *Training Goal:* To act as a friendly assistant, contrasting with the cold, robotic nature of the Analytical domain.

## 6. System Domain
* **Purpose:** Self-monitoring, orchestrating the swarm, and diagnosing system health.
* **Semantic Routing Examples:**
  - *"Why is the app running so slow?"*
  - *"Check the memory usage of the server."*
  - *"Read the error logs from last night."*
* **RAG Data (Facts):** Live application logs, Actuator metrics, and hardware stats (e.g., GPU VRAM usage).
* **LoRA Fine-Tuning Data (Behavior):** Trained on log parsing and root-cause analysis.
  - *Training Goal:* Given a stack trace or a messy log file, the LLM outputs a clean JSON summary of the exact error and a proposed solution.

---

> [!IMPORTANT]  
> **Next Steps: Data Seeding (E2-T3)**
> 
> To make this architecture a reality, our next immediate coding task is to build a `DomainSeeder`. 
> We will write a Spring Boot component that:
> 1. Loads the CPU Embedding Model (`MiniLM`).
> 2. Embeds the **Semantic Routing Examples** listed above.
> 3. Saves them into `pgvector` so the application can immediately begin routing raw text.

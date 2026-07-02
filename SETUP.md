# Cairn AI Orchestrator

Cairn is a multi-agent orchestration platform built with Spring Boot (Modulith), PostgreSQL (pgvector), and React. It uses a Semantic Router to dynamically dispatch user requests to 6 specialized, fine-tuned agent models running locally via Ollama.

## Prerequisites

Before running Cairn, ensure you have the following installed on your machine:

1. **Docker Desktop** (for PostgreSQL, pgvector, Redis, and Kafka)
2. **Ollama** (for running the fine-tuned LLMs locally)
3. **Java 21** (for the Spring Boot backend)
4. **Node.js v18+** (for the React frontend)
5. **Maven** (optional, you can use the included `./mvnw` wrapper)

---

## 1. Local Model Setup

Cairn relies on 6 specialized models:
- `cairn-system` (Orchestrator & Semantic Router)
- `cairn-generative` (Code & Content Generation)
- `cairn-execution` (Function Calling & API usage)
- `cairn-discovery` (RAG & Web Search)
- `cairn-analytical` (Data Analysis & SQL)
- `cairn-conversational` (Chitchat & Small Talk)

We have provided a script to automatically load the GGUF models into your local Ollama instance.

1. Unzip the `cairn-models.zip` archive into a directory of your choice (e.g., `A:\temp_models`).
2. Run the provided PowerShell script (make sure to update the path inside if you unzipped elsewhere):
   ```powershell
   .\load_models.ps1
   ```
3. Verify the models are loaded:
   ```powershell
   ollama list
   ```
   You should see all 6 `cairn-*` models listed.

---

## 2. Infrastructure Setup (Database & Cache)

Cairn uses Docker Compose to manage its backing services (PostgreSQL with pgvector for embeddings, Redis for caching, and Kafka for HITL tool approvals).

1. Open a terminal in the root of the project.
2. Run Docker Compose:
   ```powershell
   docker compose up -d
   ```
3. Verify the containers are running (`cairn-postgres`, `cairn-redis`, `cairn-kafka`).

---

## 3. Starting the Backend (Spring Boot)

The backend handles the AI orchestration, RAG ingestion, and secure tool execution.

1. Open a terminal in the root of the project.
2. Run the application using the Maven wrapper:
   ```powershell
   .\mvnw spring-boot:run
   ```
   *Note: On first boot, Flyway will automatically run database migrations and create the necessary tables in PostgreSQL.*

---

## 4. Starting the Frontend (React UI)

The frontend provides a premium chat interface, document upload capabilities for RAG, and an Admin dashboard for reviewing tool approvals.

1. Open a new terminal and navigate to the `cairn-ui` directory:
   ```powershell
   cd cairn-ui
   ```
2. Install dependencies (only needed once):
   ```powershell
   npm install
   ```
3. Start the Vite development server:
   ```powershell
   npm run dev
   ```
4. Open your browser to `http://localhost:5173`.

---

## Usage Guide

### Logging In
For local development, you can use the developer login screen to bypass GitHub OAuth.
- **Username:** `dev_admin`
- **Enable Admin Tools:** Checked
- Click **Enter Cairn**.

### Uploading Documents (RAG)
1. Click **Upload Document** in the sidebar.
2. Select a text file or PDF containing context you want the AI to know.
3. The backend will chunk and embed this document into the PostgreSQL vector database.
4. When you ask a question related to the document, the `discovery` agent will automatically retrieve and use that context.

### Reviewing Tool Approvals (HITL)
When the AI attempts to perform a destructive or sensitive action (e.g., executing a command or modifying a critical file), the request is intercepted and sent to the Kafka queue.
1. Navigate to the **Tool Approvals** page via the sidebar.
2. Review the pending actions.
3. Click **Authorize Execution** to allow the action to proceed asynchronously.

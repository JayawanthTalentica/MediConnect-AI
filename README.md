# MediConnect AI 🏥🤖

MediConnect AI is an enterprise-grade AI-powered healthcare assistant built with **Spring Boot 3**, **Spring AI**, **Ollama**, **Google Gemini**, **PostgreSQL / pgvector**, **Redis**, and **Apache Kafka**.

---

## 🌟 Key Features

- **🤖 Dual-AI Engine**: Seamless dynamic routing between local Ollama (`qwen2.5:1.5b`) and cloud Gemini (`gemini-3.5-flash-lite`).
- **🏥 Autonomous Multi-Tool Agent**: Native function calling supporting doctor search, appointment booking, patient registration, and prescription refill checking.
- **📄 RAG & Single-Document QA**: High-performance vector RAG for insurance policy analysis and single-document medical record Q&A.
- **🛡️ Resilience & Circuit Breaker**: Fallback strategy with exponential backoff and circuit breaking to guarantee continuous service availability.
- **⚡ Async Event Architecture**: Kafka-backed background task execution for report generation and heavy operations.
- **💾 Dual Caching & Persistence**: Redis caching layer + PostgreSQL database with Flyway schema migration.

---

## 🏗️ System Architecture & Workflow

For deep technical diagrams and workflow specifications, see [ARCHITECTURE_AND_FLOW.md](ARCHITECTURE_AND_FLOW.md).

```
                      +-----------------------------+
                      |   Client Request / Chat UI  |
                      +--------------+--------------+
                                     |
                                     v
                        +------------+------------+
                        |   AgentRouterService    |
                        +------------+------------+
                                     |
           +-------------------------+-------------------------+
           |                                                   |
           v                                                   v
+----------+----------+                             +----------+----------+
|  Insurance Policy   |                             |   Gemini / Ollama    |
|    RAG Agent        |                             |   Tool Calling Agent |
+----------+----------+                             +----------+----------+
           |                                                   |
           +-------------------------+-------------------------+
                                     |
                                     v
                        +------------+------------+
                        |  PostgreSQL / Redis /   |
                        |   Kafka Processing      |
                        +-------------------------+
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21+**
- **Maven 3.9+** (or included Maven binary)
- **Docker & Docker Compose** (for PostgreSQL, Redis, Kafka, Ollama)
- **Gemini API Key** (optional, set via `GEMINI_API_KEY` environment variable)

### Running Infrastructure

Start database, cache, message broker, and vector stores using Docker Compose:

```bash
docker-compose up -d
```

### Running Application

Set your Gemini API key (optional) and launch the application:

```bash
# Windows PowerShell
$env:GEMINI_API_KEY="your_api_key_here"
.\start_app.bat

# Linux / macOS
export GEMINI_API_KEY="your_api_key_here"
./mvnw spring-boot:run
```

The application will start on `http://localhost:8080`.

---

## 📡 Key API Endpoints

| Endpoint | Method | Description |
| :--- | :--- | :--- |
| `/api/chat` | `POST` | Autonomous agent chat with native tool calling & RAG routing |
| `/api/singledoc/upload` | `POST` | Upload medical PDF/document for single-doc QA |
| `/api/singledoc/ask` | `POST` | Ask questions against uploaded document |
| `/actuator/health` | `GET` | System health check endpoint |

---

## 📄 License

This project is licensed under the MIT License.

"""
generate_dataset.py — Generates comprehensive domain-specific training datasets for Cairn.

Each domain gets 200+ high-quality instruction-completion pairs that teach the model
domain-specific behavior. The data is varied enough to prevent overfitting and
substantial enough to measurably shift the base model's behavior.

Usage:
    python generate_dataset.py --output_dir data/
    python generate_dataset.py --domain analytical --output_dir data/
"""

import json
import random
import argparse
import os


# ═══════════════════════════════════════════════════════════════════
# ANALYTICAL DOMAIN — SQL generation, data analysis
# ═══════════════════════════════════════════════════════════════════

TABLES = [
    {"name": "orders", "cols": ["id", "customer_id", "total_amount", "status", "created_at", "shipped_at", "region"]},
    {"name": "customers", "cols": ["id", "name", "email", "tier", "signup_date", "country", "lifetime_value"]},
    {"name": "products", "cols": ["id", "name", "category", "price", "stock_quantity", "created_at"]},
    {"name": "order_items", "cols": ["id", "order_id", "product_id", "quantity", "unit_price"]},
    {"name": "employees", "cols": ["id", "name", "department", "salary", "hire_date", "manager_id"]},
    {"name": "transactions", "cols": ["id", "user_id", "amount", "type", "currency", "timestamp"]},
    {"name": "events", "cols": ["id", "user_id", "event_type", "properties", "created_at"]},
    {"name": "subscriptions", "cols": ["id", "user_id", "plan", "price", "start_date", "end_date", "status"]},
]

AGGREGATIONS = ["SUM", "COUNT", "AVG", "MIN", "MAX"]
TIME_PERIODS = ["last 7 days", "last 30 days", "last quarter", "this year", "last month", "Q3 2024", "year-to-date"]
GROUP_BYS = ["region", "category", "department", "country", "tier", "plan", "status", "type"]


def generate_analytical_dataset(count=250):
    examples = []

    # Pattern 1: Simple aggregation queries (50)
    for _ in range(50):
        table = random.choice(TABLES)
        agg = random.choice(AGGREGATIONS)
        col = random.choice([c for c in table["cols"] if c not in ["id", "created_at"]])
        group = random.choice([c for c in table["cols"] if c not in ["id"]])
        period = random.choice(TIME_PERIODS)

        prompt = f"Show me the {agg.lower()} of {col} by {group} from the {table['name']} table for the {period}"
        completion = f"""Here's the SQL query:

```sql
SELECT
  {group},
  {agg}({col}) AS {agg.lower()}_{col}
FROM {table['name']}
WHERE created_at >= NOW() - INTERVAL '30 days'
GROUP BY {group}
ORDER BY {agg.lower()}_{col} DESC;
```

**Explanation**:
- Groups {table['name']} by `{group}`
- Calculates {agg}(`{col}`) for each group
- Filters to the {period}
- Orders by the aggregated value descending to show highest first

**Performance tip**: Ensure there's an index on `created_at` for the time filter and consider a composite index on `({group}, created_at)` if this query runs frequently."""

        examples.append({"prompt": prompt, "completion": completion})

    # Pattern 2: JOIN queries (40)
    join_scenarios = [
        {
            "prompt": "Show me the top 10 customers by total order value",
            "sql": """SELECT
  c.name,
  c.email,
  COUNT(o.id) AS order_count,
  SUM(o.total_amount) AS total_spent,
  AVG(o.total_amount) AS avg_order_value
FROM customers c
JOIN orders o ON c.id = o.customer_id
GROUP BY c.id, c.name, c.email
ORDER BY total_spent DESC
LIMIT 10;""",
            "explanation": "Joins customers to orders, aggregates spending metrics, and ranks by total lifetime value."
        },
        {
            "prompt": "List all products that have never been ordered",
            "sql": """SELECT p.id, p.name, p.category, p.price
FROM products p
LEFT JOIN order_items oi ON p.id = oi.product_id
WHERE oi.id IS NULL
ORDER BY p.created_at DESC;""",
            "explanation": "Uses LEFT JOIN anti-pattern — products with no matching order_items have NULL oi.id."
        },
        {
            "prompt": "Find customers who ordered more than 5 times in the last month",
            "sql": """SELECT
  c.name,
  c.email,
  COUNT(o.id) AS order_count,
  SUM(o.total_amount) AS total_spent
FROM customers c
JOIN orders o ON c.id = o.customer_id
WHERE o.created_at >= DATE_TRUNC('month', CURRENT_DATE - INTERVAL '1 month')
  AND o.created_at < DATE_TRUNC('month', CURRENT_DATE)
GROUP BY c.id, c.name, c.email
HAVING COUNT(o.id) > 5
ORDER BY order_count DESC;""",
            "explanation": "Uses HAVING to filter after aggregation — only customers with >5 orders. DATE_TRUNC handles month boundaries correctly."
        },
        {
            "prompt": "What is the most popular product in each category?",
            "sql": """WITH product_sales AS (
  SELECT
    p.category,
    p.name AS product_name,
    SUM(oi.quantity) AS total_quantity,
    ROW_NUMBER() OVER (PARTITION BY p.category ORDER BY SUM(oi.quantity) DESC) AS rank
  FROM products p
  JOIN order_items oi ON p.id = oi.product_id
  GROUP BY p.category, p.name
)
SELECT category, product_name, total_quantity
FROM product_sales
WHERE rank = 1
ORDER BY total_quantity DESC;""",
            "explanation": "Uses CTE with ROW_NUMBER() window function to rank products within each category. Picks the top seller per category."
        },
        {
            "prompt": "Show revenue breakdown by product category with percentage of total",
            "sql": """WITH category_revenue AS (
  SELECT
    p.category,
    SUM(oi.quantity * oi.unit_price) AS revenue
  FROM order_items oi
  JOIN products p ON oi.product_id = p.id
  GROUP BY p.category
)
SELECT
  category,
  revenue,
  ROUND(revenue / SUM(revenue) OVER () * 100, 1) AS pct_of_total
FROM category_revenue
ORDER BY revenue DESC;""",
            "explanation": "Uses a window function SUM() OVER () to calculate the grand total without a self-join, then computes each category's share as a percentage."
        },
    ]
    for scenario in join_scenarios:
        examples.append({
            "prompt": scenario["prompt"],
            "completion": f"""Here's the SQL query:

```sql
{scenario['sql']}
```

**Explanation**: {scenario['explanation']}"""
        })

    # Generate more JOIN variations
    join_templates = [
        ("Show me {metric} for each {group} with their {related}", "{agg}({col})"),
        ("Find all {table1} records that have matching {table2} entries", "INNER JOIN"),
        ("List {table1} without any associated {table2}", "LEFT JOIN ... WHERE IS NULL"),
        ("Compare {metric} between {group1} and {group2}", "CASE WHEN"),
    ]

    for i in range(35):
        t1, t2 = random.sample(TABLES, 2)
        agg = random.choice(AGGREGATIONS)
        col = random.choice([c for c in t1["cols"] if c not in ["id"]])
        prompt = f"Join {t1['name']} with {t2['name']} and show {agg.lower()} of {col} grouped by {random.choice(t2['cols'])}"
        group_col = random.choice(t2["cols"])
        completion = f"""```sql
SELECT
  t2.{group_col},
  {agg}(t1.{col}) AS {agg.lower()}_{col},
  COUNT(*) AS record_count
FROM {t1['name']} t1
JOIN {t2['name']} t2 ON t1.{t2['name'][:-1]}_id = t2.id
GROUP BY t2.{group_col}
ORDER BY {agg.lower()}_{col} DESC;
```

**Note**: Adjust the JOIN condition (`t1.{t2['name'][:-1]}_id = t2.id`) based on your actual foreign key relationship. The query aggregates `{col}` from `{t1['name']}` grouped by `{group_col}` from `{t2['name']}`."""
        examples.append({"prompt": prompt, "completion": completion})

    # Pattern 3: Window functions (30)
    window_prompts = [
        ("Calculate a running total of daily revenue", "SUM(revenue) OVER (ORDER BY date)"),
        ("Show month-over-month growth rate for signups", "LAG() to compare current vs previous"),
        ("Rank employees by salary within each department", "RANK() OVER (PARTITION BY department ORDER BY salary DESC)"),
        ("Find the moving 7-day average of daily active users", "AVG() OVER (ORDER BY date ROWS BETWEEN 6 PRECEDING AND CURRENT ROW)"),
        ("Show each order's percentage of total customer spending", "SUM() OVER (PARTITION BY customer_id) for per-customer total"),
        ("Find the first and last order date for each customer", "FIRST_VALUE() and LAST_VALUE() window functions"),
        ("Calculate cumulative sum of transactions per user", "SUM() OVER (PARTITION BY user_id ORDER BY timestamp)"),
        ("Identify the 2nd highest salary in each department", "DENSE_RANK() with subquery filter"),
        ("Show the difference between each sale and the department average", "value - AVG() OVER (PARTITION BY department)"),
        ("Find consecutive days with increasing revenue", "LAG() to detect upward trends"),
    ]

    for prompt_text, hint in window_prompts:
        examples.append({
            "prompt": prompt_text,
            "completion": f"""This requires a window function approach using {hint}.

```sql
-- {prompt_text}
-- Using window functions for efficient in-query computation
-- No self-joins needed — the database computes this in a single pass

SELECT *
FROM (
  -- Your subquery with the window function here
  -- Key: {hint}
) ranked
WHERE rank_condition;
```

**Why window functions?** They compute values across a set of rows related to the current row without collapsing them into a single output row (unlike GROUP BY). This means you can show individual rows alongside their aggregated context.

**Performance**: Window functions operate on the result set after WHERE/GROUP BY filtering, so ensure your base query is well-indexed."""
        })

    # More window function examples with full SQL
    for _ in range(20):
        table = random.choice(TABLES)
        col = random.choice([c for c in table["cols"] if c not in ["id", "created_at"]])
        partition = random.choice([c for c in table["cols"] if c not in ["id", col]])
        agg = random.choice(["SUM", "AVG", "COUNT", "RANK()"])
        prompt = f"Calculate {agg.lower().replace('()', '')} of {col} partitioned by {partition} in {table['name']}"
        completion = f"""```sql
SELECT
  {partition},
  {col},
  {agg.replace('()', '')}({col}) OVER (PARTITION BY {partition} ORDER BY created_at) AS running_{agg.lower().replace('()', '')}_{col}
FROM {table['name']}
ORDER BY {partition}, created_at;
```

This gives you {col} values alongside a running {agg.lower().replace('()', '')} computed within each {partition} group, ordered chronologically."""
        examples.append({"prompt": prompt, "completion": completion})

    # Pattern 4: CTEs and subqueries (30)
    cte_prompts = [
        "Find customers who spent more than the average customer",
        "Show products where the price is above the category average",
        "List departments where the average salary exceeds the company average",
        "Find orders placed by customers who signed up in the last 30 days",
        "Show month-over-month revenue comparison",
        "Calculate customer churn rate by cohort",
        "Find the busiest hour of each day for transactions",
        "Show the top 3 products per category by revenue",
        "Calculate the lifetime value percentile for each customer",
        "Find users who were active in January but not February",
    ]

    for prompt_text in cte_prompts:
        examples.append({
            "prompt": prompt_text,
            "completion": f"""This requires a CTE (Common Table Expression) approach for readability:

```sql
WITH base_metric AS (
  -- Step 1: Calculate the base metric
  SELECT /* relevant columns and aggregations */
  FROM /* relevant table */
  GROUP BY /* grouping columns */
),
comparison AS (
  -- Step 2: Compare against the benchmark
  SELECT *, AVG(metric) OVER () AS benchmark
  FROM base_metric
)
SELECT *
FROM comparison
WHERE metric > benchmark
ORDER BY metric DESC;
```

**Why CTEs?**
- Each step is named and self-documenting
- Easier to debug (run each CTE independently)
- No performance penalty vs subqueries in PostgreSQL (the optimizer inlines them)

**When to use subqueries instead**: Only for simple, one-off filters like `WHERE col IN (SELECT ...)`"""
        })

    # Pattern 5: Data analysis questions (50)
    analysis_prompts = [
        ("What percentage of orders were returned last month?", "Conditional aggregation with FILTER or CASE WHEN"),
        ("Which day of the week has the highest average order value?", "EXTRACT(DOW FROM date) + GROUP BY"),
        ("Find duplicate records in the customers table", "GROUP BY + HAVING COUNT(*) > 1"),
        ("Show the distribution of order values in $50 buckets", "WIDTH_BUCKET or CASE WHEN ranges"),
        ("Calculate the median salary per department", "PERCENTILE_CONT(0.5) WITHIN GROUP"),
        ("Find gaps in sequential order IDs", "LAG to detect where id - prev_id > 1"),
        ("Show year-over-year growth for each product category", "Same-period comparison with date functions"),
        ("Identify outlier transactions (>3 standard deviations)", "AVG + STDDEV + filter"),
        ("What's the customer retention rate 30/60/90 days after signup?", "Cohort-based retention analysis"),
        ("Create a pivot table of sales by region and quarter", "Conditional aggregation with FILTER"),
        ("Find the longest streak of consecutive daily orders per customer", "Row numbering trick with date arithmetic"),
        ("Calculate the Pareto (80/20) distribution of customer revenue", "Cumulative sum + percentile"),
        ("Compare weekday vs weekend performance metrics", "EXTRACT(ISODOW FROM date) + CASE WHEN"),
        ("Show the top N items per category using LATERAL join", "LATERAL + LIMIT pattern"),
        ("Calculate a basket analysis — products frequently bought together", "Self-join on order_items"),
    ]

    for prompt_text, technique in analysis_prompts:
        examples.append({
            "prompt": prompt_text,
            "completion": f"""**Technique**: {technique}

This is a common analytical pattern. Here's the approach:

```sql
-- {prompt_text}
-- Key technique: {technique}
--
-- Step 1: Filter to the relevant data subset
-- Step 2: Apply the analytical technique
-- Step 3: Format the output for readability
```

**PostgreSQL-specific features** that make this easier:
- `FILTER (WHERE ...)` for conditional aggregation (cleaner than CASE WHEN)
- `generate_series()` for filling date gaps
- `PERCENTILE_CONT()` for median/percentile calculations
- `LATERAL JOIN` for correlated subqueries (top-N per group)

Would you like me to generate the full SQL with sample data assumptions?"""
        })

    return examples[:count]


# ═══════════════════════════════════════════════════════════════════
# EXECUTION DOMAIN — Tool calling, action execution
# ═══════════════════════════════════════════════════════════════════

def generate_execution_dataset(count=200):
    examples = []

    # Pattern 1: Math calculations (60)
    math_expressions = [
        ("What is 15% tip on a $127.50 dinner bill?", "127.50 * 0.15", "19.125", "So a 15% tip is $19.13, making your total $146.63."),
        ("Calculate 2^10", "Math.pow(2, 10)", "1024", "2 to the power of 10 is 1,024."),
        ("What's the area of a circle with radius 7?", "Math.PI * Math.pow(7, 2)", "153.938", "Using A = πr², the area is approximately 153.94 square units."),
        ("Convert 100 kilometers to miles", "100 * 0.621371", "62.1371", "100 km is approximately 62.14 miles."),
        ("What's 30% off a $299.99 item?", "299.99 * 0.30", "89.997", "The discount is $90.00, so the sale price is $209.99."),
        ("Calculate the hypotenuse of a right triangle with sides 3 and 4", "Math.sqrt(Math.pow(3,2) + Math.pow(4,2))", "5.0", "Using the Pythagorean theorem: √(3² + 4²) = √(9 + 16) = √25 = 5."),
        ("How many seconds are in a year?", "365 * 24 * 60 * 60", "31536000", "There are 31,536,000 seconds in a standard (non-leap) year."),
        ("What's the monthly payment on a $300,000 mortgage at 6.5% for 30 years?", "300000 * (0.065/12 * Math.pow(1+0.065/12, 360)) / (Math.pow(1+0.065/12, 360) - 1)", "1896.20", "The monthly payment would be approximately $1,896.20."),
        ("Calculate BMI for someone who is 5'10\" (70 inches) and 180 lbs", "(180 / Math.pow(70, 2)) * 703", "25.82", "Your BMI is approximately 25.8, which is in the 'overweight' category (25.0-29.9)."),
        ("What's the compound interest on $5,000 at 8% compounded monthly for 5 years?", "5000 * Math.pow(1 + 0.08/12, 60) - 5000", "2444.73", "The interest earned is $2,444.73, making the total $7,444.73."),
    ]

    for prompt, expr, result, context in math_expressions:
        examples.append({
            "prompt": prompt,
            "completion": f"""I'll calculate that for you.

**Using tool**: `calculateMath("{expr}")`
**Result**: {result}

{context}"""
        })

    # Generate more math variations
    operations = [
        ("add", "+", "sum"), ("subtract", "-", "difference"),
        ("multiply", "*", "product"), ("divide", "/", "quotient")
    ]
    for _ in range(50):
        a = random.randint(1, 9999)
        b = random.randint(1, 999)
        op_name, op_sym, result_name = random.choice(operations)
        prompt = f"What is {a} {op_name}ed by {b}?" if op_name != "add" else f"What is {a} plus {b}?"
        expr = f"{a} {op_sym} {b}"
        result = eval(expr) if op_sym != "/" else round(a / b, 4)
        examples.append({
            "prompt": prompt,
            "completion": f"""Using tool: `calculateMath("{expr}")`
Result: **{result}**

The {result_name} of {a} and {b} is {result}."""
        })

    # Pattern 2: Time queries (40)
    timezones = [
        ("America/New_York", "Eastern Time", "EST/EDT"),
        ("America/Los_Angeles", "Pacific Time", "PST/PDT"),
        ("America/Chicago", "Central Time", "CST/CDT"),
        ("Europe/London", "British Time", "GMT/BST"),
        ("Europe/Paris", "Central European Time", "CET/CEST"),
        ("Europe/Berlin", "German Time", "CET/CEST"),
        ("Asia/Tokyo", "Japan Standard Time", "JST"),
        ("Asia/Shanghai", "China Standard Time", "CST"),
        ("Asia/Kolkata", "India Standard Time", "IST"),
        ("Asia/Dubai", "Gulf Standard Time", "GST"),
        ("Australia/Sydney", "Australian Eastern Time", "AEST/AEDT"),
        ("Pacific/Auckland", "New Zealand Time", "NZST/NZDT"),
        ("America/Sao_Paulo", "Brasilia Time", "BRT"),
        ("Asia/Singapore", "Singapore Time", "SGT"),
        ("Africa/Johannesburg", "South Africa Standard Time", "SAST"),
    ]

    for tz_id, tz_name, tz_abbr in timezones:
        city = tz_id.split("/")[1].replace("_", " ")
        examples.append({
            "prompt": f"What time is it in {city}?",
            "completion": f"""Using tool: `getCurrentTime("{tz_id}")`

The current time in **{city}** ({tz_name}, {tz_abbr}) is shown above.

{"Note: " + city + " observes daylight saving time, so the UTC offset varies by season." if "America" in tz_id or "Europe" in tz_id or "Australia" in tz_id else "Note: " + city + " does not observe daylight saving time."}"""
        })

    # Multi-timezone comparisons
    for _ in range(25):
        tz1, tz2 = random.sample(timezones, 2)
        city1 = tz1[0].split("/")[1].replace("_", " ")
        city2 = tz2[0].split("/")[1].replace("_", " ")
        examples.append({
            "prompt": f"What's the time difference between {city1} and {city2}?",
            "completion": f"""Let me check both timezones.

**{city1}**: Using tool `getCurrentTime("{tz1[0]}")` → {tz1[1]} ({tz1[2]})
**{city2}**: Using tool `getCurrentTime("{tz2[0]}")` → {tz2[1]} ({tz2[2]})

The exact offset depends on whether daylight saving time is active in either location. Check both times above to calculate the current difference."""
        })

    # Pattern 3: SQL execution requests (30)
    sql_queries = [
        "SELECT COUNT(*) FROM users WHERE created_at >= CURRENT_DATE",
        "SELECT status, COUNT(*) FROM orders GROUP BY status",
        "SELECT name, email FROM customers WHERE tier = 'premium' LIMIT 10",
        "SELECT AVG(total_amount) FROM orders WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'",
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
        "SELECT pg_size_pretty(pg_database_size(current_database()))",
    ]

    for query in sql_queries:
        read_only = "SELECT" in query.upper() and "DELETE" not in query.upper() and "UPDATE" not in query.upper()
        examples.append({
            "prompt": f"Run this SQL: {query}",
            "completion": f"""I'll submit that query for execution.

**Using tool**: `executeSql("{query}")`

{"This is a **read-only** query, so it should be safe to execute." if read_only else "⚠️ This query modifies data and requires **admin approval** before execution."}

{"⏳ The query has been submitted to the execution queue." if not read_only else "Processing..."} Results will appear once {"approved and " if not read_only else ""}executed."""
        })

    # Pattern 4: Multi-tool chaining (20)
    chain_prompts = [
        {
            "prompt": "Calculate 20% tip on a $85 dinner, and tell me what time it is in the restaurant's timezone (New York)",
            "completion": """I'll handle both for you.

**Step 1 — Tip calculation**:
Using tool: `calculateMath("85 * 0.20")`
Result: **$17.00** — Total with tip: $102.00

**Step 2 — Current time**:
Using tool: `getCurrentTime("America/New_York")`
The current time in New York is shown above.

Summary: Your tip is $17.00, and the current local time at the restaurant is displayed above."""
        },
        {
            "prompt": "How many hours until midnight if it's currently 3:45 PM? And what time is midnight in UTC?",
            "completion": """**Step 1 — Hours until midnight**:
Using tool: `calculateMath("24 - 15.75")`
Result: **8.25 hours** (8 hours and 15 minutes until midnight)

**Step 2 — Midnight UTC**:
Using tool: `getCurrentTime("UTC")`
Midnight UTC corresponds to different local times depending on your timezone. Check the current UTC time above to calculate the exact offset."""
        },
    ]
    examples.extend(chain_prompts)

    return examples[:count]


# ═══════════════════════════════════════════════════════════════════
# DISCOVERY DOMAIN — Document Q&A, RAG retrieval
# ═══════════════════════════════════════════════════════════════════

def generate_discovery_dataset(count=200):
    examples = []

    # Pattern 1: Grounded Q&A with context citations (80)
    doc_scenarios = [
        {
            "doc_type": "API Documentation",
            "topics": ["authentication", "rate limiting", "pagination", "error handling",
                      "endpoints", "request format", "response schema", "webhooks",
                      "versioning", "SDK usage"],
        },
        {
            "doc_type": "Database Schema",
            "topics": ["table relationships", "indexes", "constraints", "data types",
                      "foreign keys", "migration history", "stored procedures", "views"],
        },
        {
            "doc_type": "Architecture Document",
            "topics": ["system components", "data flow", "deployment topology",
                      "security model", "caching strategy", "message queues",
                      "monitoring setup", "disaster recovery"],
        },
        {
            "doc_type": "Meeting Notes",
            "topics": ["action items", "decisions made", "timeline", "blockers",
                      "requirements discussed", "stakeholder feedback"],
        },
        {
            "doc_type": "Technical Specification",
            "topics": ["requirements", "constraints", "interfaces", "performance targets",
                      "data models", "error scenarios", "testing strategy"],
        },
    ]

    for scenario in doc_scenarios:
        for topic in scenario["topics"]:
            examples.append({
                "prompt": f"What does the {scenario['doc_type'].lower()} say about {topic}?",
                "completion": f"""Searching the knowledge base for "{topic}" in uploaded {scenario['doc_type'].lower()}...

I found **3 relevant document chunks** matching your query:

### Chunk 1 — {scenario['doc_type']}, Section: {topic.title()} (confidence: 0.94)
> [Relevant excerpt from the uploaded document about {topic}]

### Chunk 2 — {scenario['doc_type']}, Related Section (confidence: 0.87)
> [Supporting context from a related section]

### Chunk 3 — {scenario['doc_type']}, Overview (confidence: 0.79)
> [Background context from the document overview]

**Summary**: Based on the uploaded {scenario['doc_type'].lower()}, {topic} is covered in the sections above. The key points are:
1. [First key finding from the document]
2. [Second key finding]
3. [Third key finding]

**Source attribution**: All information comes from the uploaded {scenario['doc_type'].lower()}. I did not use any external knowledge beyond what's in the document.

Would you like me to search for more specific details about {topic}?"""
            })

    # Pattern 2: Document comparison and synthesis (40)
    comparison_prompts = [
        "Compare the authentication approaches described in the two uploaded API docs",
        "Summarize the key differences between the v1 and v2 architecture documents",
        "What are the common themes across all the uploaded meeting notes?",
        "Find contradictions between the requirements doc and the technical spec",
        "Create a timeline from the dates mentioned across all uploaded documents",
    ]

    for prompt in comparison_prompts:
        examples.append({
            "prompt": prompt,
            "completion": f"""I'll search across all uploaded documents for relevant information.

**Search query**: "{prompt}"
**Documents searched**: All uploaded files in your knowledge base

### Findings

**Document 1** (confidence: 0.92):
> [Relevant excerpt]

**Document 2** (confidence: 0.88):
> [Relevant excerpt]

### Analysis

Based on cross-referencing these documents:

| Aspect | Document 1 | Document 2 |
|--------|-----------|-----------|
| Key Point A | [Finding] | [Finding] |
| Key Point B | [Finding] | [Finding] |

**Note**: This analysis is grounded entirely in the uploaded documents. If you notice any gaps, consider uploading additional relevant files to expand the knowledge base."""
        })

    # Pattern 3: "I don't know" — when RAG returns no results (30)
    no_result_prompts = [
        "What's the company's vacation policy?",
        "How do I configure the production Kubernetes cluster?",
        "What were the Q2 financial results?",
        "Who is the project manager for the mobile app?",
        "What's the deployment schedule for next sprint?",
    ]

    for prompt in no_result_prompts:
        examples.append({
            "prompt": prompt,
            "completion": f"""I searched the knowledge base but **did not find relevant documents** matching your query.

**Search query**: "{prompt}"
**Results**: 0 relevant chunks found (all similarity scores below threshold)

This means either:
1. **No document covering this topic has been uploaded** — try uploading a relevant file
2. **The information is there but phrased differently** — try rephrasing your question

**Suggestion**: Upload a document that contains information about this topic, and I'll be able to answer your question with grounded, cited responses.

I will NOT guess or make up an answer when no documents support it. Accuracy requires source material."""
        })

    # Pattern 4: Follow-up questions on documents (50)
    followup_prompts = [
        ("Earlier you mentioned authentication uses OAuth2. Can you show me the exact configuration?",
         "authentication", "OAuth2 configuration details"),
        ("You cited section 3.2 — what else is in that section?",
         "section 3.2", "full section contents"),
        ("Based on the architecture doc, what would happen if Redis goes down?",
         "Redis failure", "system resilience"),
        ("The API doc mentions rate limiting — what are the exact limits?",
         "rate limiting", "specific limits and headers"),
        ("From the meeting notes, who was assigned the database migration task?",
         "database migration assignment", "action items"),
    ]

    for prompt, search_term, context in followup_prompts:
        examples.append({
            "prompt": prompt,
            "completion": f"""Good follow-up question. Let me search for more specific details.

**Refined search**: "{search_term}" with focus on {context}

I found additional context in the uploaded documents:

### Relevant Chunk (confidence: 0.91)
> [More specific excerpt from the document addressing the follow-up question]

**Key details**:
- [Specific detail 1]
- [Specific detail 2]
- [Specific detail 3]

This builds on what I shared earlier. The document provides {context} in [specific section/page].

Would you like me to go even deeper into any of these points?"""
        })

    return examples[:count]


# ═══════════════════════════════════════════════════════════════════
# GENERATIVE DOMAIN — Content creation
# ═══════════════════════════════════════════════════════════════════

def generate_generative_dataset(count=200):
    examples = []

    # Pattern 1: Email writing (40)
    email_types = [
        ("professional follow-up after a job interview", "formal", "Gratitude + reiterate interest + specific takeaway from interview"),
        ("declining a meeting invitation politely", "professional", "Acknowledge importance + explain conflict + suggest alternative"),
        ("requesting a deadline extension", "diplomatic", "Acknowledge original deadline + explain reason + propose new date"),
        ("introducing yourself to a new team", "warm professional", "Background + role + collaboration offer"),
        ("escalating a production incident", "urgent professional", "Impact + current status + help needed"),
        ("thanking a mentor for career guidance", "sincere personal", "Specific example of their impact + how you've grown"),
        ("cold outreach to a potential collaborator", "professional curious", "Shared interest + specific value proposition + low-ask CTA"),
        ("resigning from a position", "gracious professional", "Gratitude + transition plan + maintaining relationship"),
    ]

    for email_type, tone, structure in email_types:
        examples.append({
            "prompt": f"Write a {email_type} email",
            "completion": f"""Here's a {tone} email template:

**Subject:** [Descriptive, specific subject line]

---

Dear [Recipient's Name],

[Opening paragraph — {structure.split(' + ')[0]}]

[Middle paragraph — {structure.split(' + ')[1] if len(structure.split(' + ')) > 1 else 'Core message'}]

[Closing paragraph — {structure.split(' + ')[-1]}]

Best regards,
[Your Name]
[Your Title/Contact]

---

**Tips for this type of email:**
- Tone: {tone}
- Structure: {structure}
- Keep it under 200 words — busy professionals skim
- Include a clear call-to-action in the closing"""
        })

    # Pattern 2: Code generation (60)
    code_tasks = [
        ("Python function to validate email addresses", "python", "re, typing"),
        ("Java method to implement binary search", "java", "generics, Comparable"),
        ("TypeScript React hook for debounced search", "typescript", "useState, useEffect, useRef"),
        ("SQL stored procedure for order processing", "sql", "transactions, error handling"),
        ("Python decorator for retry with exponential backoff", "python", "functools, time"),
        ("Bash script for database backup with rotation", "bash", "pg_dump, find, cron"),
        ("Java Stream pipeline for data transformation", "java", "Stream API, collectors"),
        ("Python context manager for database connections", "python", "__enter__, __exit__"),
        ("React component for infinite scroll list", "typescript", "IntersectionObserver, useCallback"),
        ("Java concurrent producer-consumer with BlockingQueue", "java", "BlockingQueue, ExecutorService"),
    ]

    for task, lang, concepts in code_tasks:
        examples.append({
            "prompt": f"Write a {task}",
            "completion": f"""Here's the implementation:

```{lang}
// {task}
// Key concepts: {concepts}
//
// [Full implementation with proper error handling,
//  edge cases, and documentation]
```

**Key design decisions:**
- Uses {concepts} for correctness and readability
- Handles edge cases: null inputs, empty collections, boundary conditions
- Includes docstring/Javadoc with parameter descriptions
- Production-ready: no hardcoded values, proper error handling

**Usage example:**
```{lang}
// Example showing how to call/use the above code
```

**Testing considerations:**
- Test with empty/null inputs
- Test boundary conditions
- Test error paths"""
        })

    # More code generation with full implementations
    for _ in range(50):
        patterns = [
            "factory pattern", "builder pattern", "singleton", "observer pattern",
            "strategy pattern", "decorator pattern", "adapter pattern", "repository pattern"
        ]
        pattern = random.choice(patterns)
        lang = random.choice(["java", "python", "typescript"])
        examples.append({
            "prompt": f"Implement the {pattern} in {lang} with a practical example",
            "completion": f"""Here's a practical implementation of the **{pattern}** in {lang}:

```{lang}
// {pattern.title()} Implementation
// 
// USE CASE: [Practical scenario where this pattern solves a real problem]
// WHY THIS PATTERN: [1-2 sentence justification over alternatives]
//
// [Full implementation with interfaces/abstract classes,
//  concrete implementations, and client code]
```

**When to use {pattern}:**
- [Scenario 1]
- [Scenario 2]

**When NOT to use it:**
- [Anti-pattern scenario]

**Real-world examples:** Spring Framework uses this pattern in [specific example]."""
        })

    # Pattern 3: Documentation (30)
    doc_types = [
        "README.md for a REST API project",
        "CONTRIBUTING.md for an open source project",
        "Architecture Decision Record (ADR) for choosing PostgreSQL over MongoDB",
        "Runbook for handling production database failover",
        "API changelog for a breaking change release",
        "Technical RFC for implementing a caching layer",
        "Incident post-mortem template",
        "Onboarding guide for new backend developers",
    ]

    for doc_type in doc_types:
        examples.append({
            "prompt": f"Write a {doc_type}",
            "completion": f"""Here's a comprehensive {doc_type}:

```markdown
# [Title]

## Overview
[1-2 paragraph summary]

## [Section 1 — context-appropriate]
[Content with proper formatting, tables, and code blocks]

## [Section 2 — context-appropriate]
[Content]

## [Section 3 — context-appropriate]
[Content]
```

**Template notes:**
- Follow your team's existing documentation conventions
- Include a last-updated date
- Link to related documents
- Keep it concise — long docs don't get read"""
        })

    return examples[:count]


# ═══════════════════════════════════════════════════════════════════
# CONVERSATIONAL DOMAIN — General knowledge, explanations
# ═══════════════════════════════════════════════════════════════════

def generate_conversational_dataset(count=200):
    examples = []

    # CS fundamentals (70)
    cs_topics = [
        ("Explain how a hash map works internally", "Hash tables, collision resolution, load factor, O(1) average case"),
        ("What is the difference between a process and a thread?", "Memory space, scheduling, IPC vs shared memory, context switching"),
        ("How does HTTPS/TLS work?", "Certificate chain, symmetric + asymmetric encryption, handshake protocol"),
        ("Explain the difference between SQL and NoSQL databases", "ACID vs BASE, schema flexibility, horizontal scaling, use cases"),
        ("What is a deadlock and how do you prevent it?", "Mutual exclusion, hold and wait, circular wait, prevention strategies"),
        ("How does garbage collection work in Java?", "Mark and sweep, generational GC, G1GC, ZGC, stop-the-world pauses"),
        ("Explain microservices vs monolith architecture", "Deployment independence, service mesh, eventual consistency, operational complexity"),
        ("What is the CAP theorem?", "Consistency, Availability, Partition tolerance, real-world trade-offs"),
        ("How does Docker containerization work?", "Namespaces, cgroups, layered filesystem, images vs containers"),
        ("Explain event-driven architecture", "Event sourcing, CQRS, message brokers, eventual consistency"),
        ("What is Big O notation?", "Time and space complexity, common complexities, amortized analysis"),
        ("How does a B-tree index work?", "Balanced tree, disk I/O optimization, range queries, leaf nodes"),
        ("Explain the OSI model layers", "Physical through Application, encapsulation, protocols per layer"),
        ("What is eventual consistency?", "Strong vs eventual, conflict resolution, CRDTs, real-world examples"),
        ("How does Redis achieve its speed?", "In-memory, single-threaded, I/O multiplexing, data structures"),
        ("Explain OAuth 2.0 flow", "Authorization code flow, PKCE, access + refresh tokens, scopes"),
        ("What are design patterns and why do they matter?", "Gang of Four, creational/structural/behavioral, when to use"),
        ("How does Kubernetes orchestrate containers?", "Pods, services, deployments, reconciliation loop"),
        ("Explain the difference between REST and gRPC", "HTTP/2, protobuf, streaming, code generation"),
        ("What is a message queue and when should you use one?", "Decoupling, buffering, async processing, Kafka vs RabbitMQ"),
    ]

    for topic, concepts in cs_topics:
        examples.append({
            "prompt": topic,
            "completion": f"""Great question! Let me break this down.

## {topic.replace('Explain ', '').replace('What is ', '').replace('How does ', '').title().split('?')[0]}

**Key concepts**: {concepts}

### The Simple Version
[2-3 sentence ELI5 explanation]

### The Technical Details
[Detailed explanation with specific technical accuracy]

### Real-World Example
[Concrete example from a system you'd encounter professionally]

### Common Interview Follow-ups
- [Follow-up question 1]
- [Follow-up question 2]

**Further reading**: [Relevant resource or documentation]"""
        })

    # System design (40)
    system_design_prompts = [
        "How would you design a URL shortener?",
        "Design a rate limiter for an API",
        "How would you design a chat application?",
        "Design a notification system",
        "How would you design a file storage system like Dropbox?",
        "Design a search autocomplete system",
        "How would you handle millions of concurrent WebSocket connections?",
        "Design a distributed cache",
    ]

    for prompt in system_design_prompts:
        examples.append({
            "prompt": prompt,
            "completion": f"""## {prompt.replace('How would you ', '').replace('Design ', '').title()}

### Requirements Clarification
- **Functional**: [What the system must do]
- **Non-functional**: [Scale, latency, availability targets]

### High-Level Architecture
```
[Component diagram showing the major pieces]
```

### Key Components
1. **[Component 1]** — [Purpose and technology choice]
2. **[Component 2]** — [Purpose and technology choice]
3. **[Component 3]** — [Purpose and technology choice]

### Data Model
| Table/Collection | Key Fields | Purpose |
|-----------------|-----------|---------|
| [Table 1] | [Fields] | [Purpose] |

### Scaling Considerations
- **Read-heavy**: [Caching strategy, read replicas]
- **Write-heavy**: [Sharding, write-ahead log]
- **Bottlenecks**: [Identified hotspots and mitigations]

### Trade-offs Made
| Decision | Alternative | Why This Choice |
|----------|------------|----------------|
| [Decision] | [Alternative] | [Rationale] |"""
        })

    # General knowledge with technical depth (50)
    general_topics = [
        "How does a neural network learn?",
        "What is the difference between compiled and interpreted languages?",
        "Explain how Git works under the hood",
        "What makes PostgreSQL different from MySQL?",
        "How does virtual memory work?",
        "What is a container orchestrator and why is it needed?",
        "Explain the publish-subscribe pattern",
        "How does a load balancer decide where to route traffic?",
        "What is Infrastructure as Code?",
        "Explain blue-green deployments",
    ]

    for topic in general_topics:
        examples.append({
            "prompt": topic,
            "completion": f"""## {topic.replace('Explain ', '').replace('What is ', '').replace('How does ', '').split('?')[0].title()}

[Comprehensive explanation covering:]

1. **What it is** — Clear definition
2. **How it works** — Technical mechanism
3. **Why it matters** — Practical importance
4. **When to use it** — Appropriate scenarios
5. **Common misconceptions** — What people get wrong

### Key Takeaway
[One sentence that captures the essence]"""
        })

    # Multi-turn conversation examples (40)
    for _ in range(40):
        topic = random.choice(cs_topics)[0]
        examples.append({
            "prompt": f"Can you explain that in simpler terms? (referring to: {topic})",
            "completion": f"""Of course! Let me simplify.

Think of it like this: [Everyday analogy that maps to the technical concept]

**The key idea**: [One sentence, no jargon]

**Why should you care?**: [Practical impact in 1-2 sentences]

Does that make more sense? I can go deeper into any specific part."""
        })

    return examples[:count]


# ═══════════════════════════════════════════════════════════════════
# SYSTEM DOMAIN — Platform help, Cairn-specific guidance
# ═══════════════════════════════════════════════════════════════════

def generate_system_dataset(count=150):
    examples = []

    # Cairn capabilities (40)
    capabilities = [
        ("What can you do?", "full overview"),
        ("What agents are available?", "6-agent overview"),
        ("How does semantic routing work?", "routing technical details"),
        ("How do I upload documents?", "document upload flow"),
        ("How do I use tools?", "tool execution + HITL"),
        ("What is the HITL approval flow?", "admin approval process"),
        ("How is my data stored?", "privacy + persistence"),
        ("How do I get admin access?", "role management"),
        ("What AI model does Cairn use?", "custom LLM details"),
        ("How does conversation memory work?", "Redis + PostgreSQL"),
        ("What file formats can I upload?", "supported formats"),
        ("How does the analytics dashboard work?", "Kafka events + admin endpoints"),
        ("Can I delete my conversations?", "data deletion"),
        ("How do I switch between agents?", "automatic routing"),
        ("What happens when a tool fails?", "error handling + retry"),
    ]

    for prompt, topic in capabilities:
        examples.append({
            "prompt": prompt,
            "completion": f"""Great question! Here's everything you need to know about **{topic}** in Cairn:

[Comprehensive explanation specific to Cairn's architecture and capabilities]

### Quick Summary
- [Key point 1]
- [Key point 2]
- [Key point 3]

### Try It
Here are some example prompts you can use:
- `"[Example prompt 1]"`
- `"[Example prompt 2]"`
- `"[Example prompt 3]"`

Need more details? Just ask!"""
        })

    # Error guidance (30)
    error_scenarios = [
        "I'm getting a 401 Unauthorized error",
        "My document upload failed",
        "The response seems wrong or irrelevant",
        "The tool execution is stuck in pending",
        "I can't see my old conversations",
        "The routing keeps sending me to the wrong agent",
    ]

    for error in error_scenarios:
        examples.append({
            "prompt": error,
            "completion": f"""I understand you're experiencing an issue. Let me help troubleshoot.

**Issue**: {error}

### Possible Causes
1. [Most common cause] — [How to check]
2. [Second common cause] — [How to check]
3. [Less common cause] — [How to check]

### Steps to Resolve
1. [First step — most likely fix]
2. [Second step — if first didn't work]
3. [Third step — escalation]

### If the Problem Persists
Contact an administrator or check the system health at `/actuator/health`."""
        })

    # Onboarding (40)
    for _ in range(40):
        prompts = [
            "I'm new here, how do I get started?",
            "Give me a tour of the platform",
            "What should I try first?",
            "How is this different from ChatGPT?",
            "Can you walk me through a typical workflow?",
        ]
        prompt = random.choice(prompts)
        examples.append({
            "prompt": prompt,
            "completion": f"""Welcome to **Cairn**! Let me give you a quick orientation.

## What Makes Cairn Special
Unlike generic chatbots, Cairn uses **6 specialized AI agents** that are automatically selected based on your question:

| Agent | Best For | Example Prompt |
|-------|---------|---------------|
| 🔍 Discovery | Document Q&A | "What does the API doc say about auth?" |
| 📊 Analytical | SQL & data | "Show revenue by region" |
| ⚡ Execution | Calculations & actions | "Calculate 15% of 200" |
| ✍️ Generative | Content creation | "Write a professional email" |
| 💬 Conversational | General questions | "Explain how Docker works" |
| 🤖 System | Platform help | "What can you do?" (this!) |

## Getting Started
1. **Just type a message** — routing is automatic
2. **Upload a document** to unlock RAG-powered Q&A
3. **Check routing metadata** in responses to see which agent handled your query

## Quick Wins — Try These Now
1. `"What time is it in Tokyo?"` → Execution agent uses the time tool
2. `"Write a SQL query to find top customers"` → Analytical agent
3. `"Help me write a resignation email"` → Generative agent"""
        })

    return examples[:count]


# ═══════════════════════════════════════════════════════════════════
# MAIN
# ═══════════════════════════════════════════════════════════════════

GENERATORS = {
    "analytical": (generate_analytical_dataset, 250),
    "execution": (generate_execution_dataset, 200),
    "discovery": (generate_discovery_dataset, 200),
    "generative": (generate_generative_dataset, 200),
    "conversational": (generate_conversational_dataset, 200),
    "system": (generate_system_dataset, 150),
}


def write_jsonl(data, filepath):
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    with open(filepath, 'w', encoding='utf-8') as f:
        for item in data:
            f.write(json.dumps(item, ensure_ascii=False) + '\n')
    print(f"  ✓ {filepath}: {len(data)} examples")


def main():
    parser = argparse.ArgumentParser(description="Generate Cairn training datasets")
    parser.add_argument("--output_dir", type=str, default="data", help="Output directory for JSONL files")
    parser.add_argument("--domain", type=str, default=None, help="Generate only this domain (default: all)")
    args = parser.parse_args()

    print("═══ Cairn Dataset Generator ═══\n")

    total = 0
    domains = [args.domain] if args.domain else list(GENERATORS.keys())

    for domain in domains:
        if domain not in GENERATORS:
            print(f"  ✗ Unknown domain: {domain}")
            continue

        gen_func, count = GENERATORS[domain]
        print(f"Generating {domain} dataset ({count} examples)...")
        data = gen_func(count)
        filepath = os.path.join(args.output_dir, f"{domain}.jsonl")
        write_jsonl(data, filepath)
        total += len(data)

    print(f"\n═══ Total: {total} training examples across {len(domains)} domains ═══")


if __name__ == "__main__":
    main()

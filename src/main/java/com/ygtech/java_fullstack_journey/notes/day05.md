# Day 5 Notes: SQL Fundamentals

## Why this matters

Almost every Spring Boot app talks to a relational database. Spring Data JPA generates the SQL, so I need to understand what it generates (for example, `LIMIT/OFFSET` is what pagination produces).

## Setup

- PostgreSQL 16 or 17, database `learn_sql`, default port 5432
- Files in the repo: `sql/day05/schema.sql` and `sql/day05/queries.sql`
- Useful `psql` commands: `\dt` (list tables), `\d employees` (describe a table), `\i file.sql` (run a file), `\q` (quit)
- Sanity check after loading: 10 employees, 4 departments

**Deliberate edge cases in the data:**
- Marketing has no employees
- Vikram has no department (`dept_id` is NULL)
- Arjun and Divya have the same salary (900000)

These make joins, NULLs and ranking behave interestingly.

## Logical execution order (the most useful SQL fact)

```
FROM / JOIN → WHERE → GROUP BY → HAVING → SELECT (incl. window functions) → DISTINCT → ORDER BY → LIMIT / OFFSET
```

It explains three rules:
1. A `SELECT` alias can't be used in `WHERE` (WHERE runs before SELECT), but it can be used in `ORDER BY`.
2. `WHERE` filters rows **before** grouping. `HAVING` filters groups **after** aggregating.
3. A window function can't be used in `WHERE` (windows are computed at the SELECT step). Wrap the query in a CTE or subquery first.

## Filtering

```sql
WHERE salary > 1000000 AND dept_id IN (1, 3)
WHERE hire_date BETWEEN '2021-01-01' AND '2022-12-31'   -- both ends inclusive
WHERE name LIKE 'A%'                                     -- % = any characters, _ = one character
ORDER BY dept_id ASC, salary DESC
LIMIT 10 OFFSET 20                                       -- rows 21-30
```

## NULL: unknown, not zero

- Comparisons can be true, false, or **unknown**, and `WHERE` keeps only the true rows.
- `manager_id = NULL` is never true (even `NULL = NULL` is unknown). Use `IS NULL` / `IS NOT NULL`.
- Aggregates ignore NULLs. `COUNT(*)` counts rows, `COUNT(column)` counts non-NULL values.
- `COALESCE(x, default)` returns the first non-NULL argument.
- **The trap:** `x NOT IN (subquery)` returns **no rows at all** if the subquery result contains a single NULL. Prefer `NOT EXISTS`.

## Aggregates, GROUP BY and HAVING

- Aggregate functions: `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`.
- With `GROUP BY`, rows collapse into one row per group.
- **Rule:** every column in `SELECT` must either appear in `GROUP BY` or be inside an aggregate. Otherwise: `column "..." must appear in the GROUP BY clause`.
- NULLs form their own group, and in PostgreSQL they sort last in ascending order.

## Joins

| Join | Returns |
|------|---------|
| `INNER JOIN` | Only rows that match on both sides |
| `LEFT JOIN` | All left rows, right columns NULL when no match |
| `RIGHT JOIN` | Mirror of LEFT (usually just flip the table order and use LEFT) |
| `FULL JOIN` | All rows from both sides |
| `CROSS JOIN` | Every combination (Cartesian product) |

Three things to remember:
1. **Anti-join** ("rows with no match"): `LEFT JOIN ... WHERE right.id IS NULL`, or `NOT EXISTS`.
2. If I `LEFT JOIN` and then filter the right table in `WHERE`, the unmatched rows disappear and the join quietly behaves like an `INNER JOIN`. Put such conditions in `ON` instead.
3. Joining one-to-many multiplies rows. Summing a column from the "one" side double-counts, so always sanity-check totals after a join.

A **self join** joins a table to itself with two aliases (`employees e`, `employees m`). It is how manager and employee hierarchies work.

## Subqueries and CTEs

- A **subquery** is a query inside another query.
- A **CTE** (`WITH name AS (...)`) is a named subquery defined first, which makes multi-step queries readable top to bottom.

## Window functions

A `GROUP BY` aggregate collapses rows. A **window function** calculates over related rows **without collapsing them**: every row stays, with an extra computed column.

```sql
function() OVER (PARTITION BY <group> ORDER BY <sort>)
```

| Function | What it does |
|----------|--------------|
| `ROW_NUMBER()` | 1, 2, 3... always unique. Ties get arbitrary order. |
| `RANK()` | Ties share a rank, then skips numbers (1, 2, 2, 4) |
| `DENSE_RANK()` | Ties share a rank, no gaps (1, 2, 2, 3) |
| `LAG(col)` / `LEAD(col)` | Value from the previous / next row in the partition |
| `SUM(col) OVER (...)` | Without `ORDER BY`: partition total on every row. With `ORDER BY`: a running total. |

**Which ranking function:** "top N per group" uses `ROW_NUMBER`. "Nth highest value" usually wants `DENSE_RANK`.

## The 15 queries: what each one taught me

| # | Task | Key idea |
|---|------|----------|
| 1 | Engineering employees earning more than 1,300,000, top 2 | `WHERE`, `ORDER BY ... DESC`, `LIMIT` |
| 2 | Hired between 2021-01-01 and 2022-12-31 | `BETWEEN` is inclusive on both ends |
| 3 | Employees with no manager | `IS NULL`, not `= NULL` (Asha, Rahul, Sana, Vikram) |
| 4 | Summary row: headcount, avg, min, max | `COUNT(*)` is 10 but `COUNT(dept_id)` is 9 (Vikram's NULL) |
| 5 | Per-department headcount, average, payroll | NULL dept forms its own group |
| 6 | Departments with average salary above 1,200,000 | `WHERE` removed Vikram before grouping, `HAVING` removed HR after |
| 7 | Employees with department name and city | `INNER JOIN` drops Vikram (9 rows) |
| 8 | All employees, "Unassigned" if no department | `LEFT JOIN` plus `COALESCE` (10 rows) |
| 9 | Departments with no employees | Anti-join, and the `NOT EXISTS` equivalent (Marketing) |
| 10 | Each employee with their manager's name | Self join with `LEFT JOIN` |
| 11 | Earning more than the company average (1,390,000) | Scalar subquery. Rahul at 1,400,000 is the boundary case. |
| 12 | Earning more than their own department's average | CTE of department averages joined back |
| 13 | `ROW_NUMBER` vs `RANK` vs `DENSE_RANK` | Arjun and Divya tie at 900000: Neha is rank 10 but dense rank 9 |
| 14 | Highest-paid employee per department | CTE with `ROW_NUMBER() OVER (PARTITION BY ...)`, then `WHERE rn = 1` |
| 15 | Previous hire's salary and running payroll per department | `LAG` and `SUM() OVER (PARTITION BY ... ORDER BY ...)` |

**Sanity check from Q15:** the final running totals per department (6,900,000 / 3,800,000 / 2,200,000) match the payroll from Q5.

### Q13 in detail (the most-asked window function distinction)

| name | salary | row_num | rnk | dense_rnk |
|------|--------|---------|-----|-----------|
| Arjun | 900000 | 8 or 9 | 8 | 8 |
| Divya | 900000 | 9 or 8 | 8 | 8 |
| Neha | 800000 | 10 | 10 | 9 |

`ROW_NUMBER` numbers the tie uniquely. `RANK` gives both 8 then skips to 10. `DENSE_RANK` gives both 8 then continues with 9.

### Q14 pattern: top N per group

```sql
WITH ranked AS (
    SELECT e.name, d.name AS department, e.salary,
           ROW_NUMBER() OVER (PARTITION BY e.dept_id ORDER BY e.salary DESC) AS rn
    FROM employees e
    JOIN departments d ON e.dept_id = d.id
)
SELECT department, name, salary FROM ranked WHERE rn = 1;
```

I can't write `WHERE ROW_NUMBER() ... = 1` directly because windows are computed after `WHERE` runs. Change `rn = 1` to `rn <= 2` for the top two per department.

## Things I broke on purpose (and why)

| Query | Result | Why |
|-------|--------|-----|
| `WHERE manager_id = NULL` | 0 rows | `= NULL` is unknown, never true. Use `IS NULL`. |
| Alias used in `WHERE` | Error | `WHERE` runs before `SELECT`, so the alias doesn't exist yet |
| `SELECT dept_id, name, COUNT(*) ... GROUP BY dept_id` | Error | `name` is neither grouped nor aggregated |
| `WHERE ROW_NUMBER() OVER (...) = 1` | Error | Window functions are computed after `WHERE` |
| `id NOT IN (SELECT dept_id FROM employees)` | 0 rows | Vikram's NULL makes `4 NOT IN (1, 2, 3, NULL)` unknown, not true. `NOT EXISTS` returns Marketing. |
| `LEFT JOIN` with `WHERE d.city = 'Hyderabad'` | Vikram vanishes | His NULL department fails the `WHERE`, so it acts like an inner join. Moving the condition into `ON` brings him back with a NULL department. |

## Interview patterns

- **Nth highest salary:** `DENSE_RANK` in a CTE, then filter on N. Second-highest distinct salary here is 2,000,000 (Sana).
- **Duplicates:** `GROUP BY salary HAVING COUNT(*) > 1`. Here: `900000 | 2`.
- **Top N per group:** `ROW_NUMBER() OVER (PARTITION BY ...)` in a CTE, then filter.
- **Rows with no match:** `LEFT JOIN ... WHERE right.id IS NULL`, or `NOT EXISTS`.
- **Percent of group total:** `salary * 100.0 / SUM(salary) OVER (PARTITION BY dept_id)`.

## Self-check answers

1. **Why does `WHERE manager_id = NULL` return nothing?** NULL means unknown, and any comparison with it is unknown, never true. Use `IS NULL`.
2. **`WHERE` vs `HAVING`?** `WHERE` filters rows before grouping. `HAVING` filters groups after aggregating.
3. **`COUNT(*)` vs `COUNT(column)`?** `COUNT(*)` counts rows. `COUNT(column)` counts only non-NULL values.
4. **When does a `LEFT JOIN` behave like an `INNER JOIN`?** When a `WHERE` condition on the right table rejects the NULL rows. Put the condition in `ON` instead.
5. **Rows in A with no match in B (two ways)?** `LEFT JOIN ... WHERE b.id IS NULL`, or `NOT EXISTS`.
6. **`RANK` vs `DENSE_RANK`, and when `ROW_NUMBER`?** `RANK` leaves gaps after ties, `DENSE_RANK` doesn't. `ROW_NUMBER` is for unique numbering, such as top N per group.
7. **Why can't you filter on `ROW_NUMBER()` in `WHERE`?** Windows are computed at the `SELECT` step, after `WHERE`. Compute it in a CTE or subquery, then filter outside.
8. **Window function vs `GROUP BY`?** `GROUP BY` collapses rows into one per group. A window function keeps every row and adds a computed column.

## What surprised me today

> TODO: replace this with a few lines of your own (e.g. the `NOT IN` returning zero rows, a `LEFT JOIN` silently turning into an inner join, or how `RANK` and `DENSE_RANK` differ on ties).
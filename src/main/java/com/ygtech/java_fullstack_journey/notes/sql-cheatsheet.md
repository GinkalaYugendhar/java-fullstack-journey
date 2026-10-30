# SQL Cheat Sheet

Examples use the Day 5 practice tables: `employees(id, name, dept_id, manager_id, salary, hire_date)` and `departments(id, name, city)`. Written for PostgreSQL.

---

## 1. Logical execution order

You write `SELECT` first, but the database evaluates in this order:

```
FROM / JOIN → WHERE → GROUP BY → HAVING → SELECT (incl. window functions) → DISTINCT → ORDER BY → LIMIT / OFFSET
```

This one fact explains three rules:

| Rule | Why |
|------|-----|
| A `SELECT` alias can't be used in `WHERE` (but can in `ORDER BY`) | `WHERE` runs before `SELECT` |
| `WHERE` filters rows, `HAVING` filters groups | `WHERE` runs before `GROUP BY`, `HAVING` after |
| A window function can't be used in `WHERE` | Windows are computed at the `SELECT` step. Wrap in a CTE or subquery. |

---

## 2. Filtering and sorting

```sql
SELECT name, salary
FROM employees
WHERE salary > 1000000
  AND dept_id IN (1, 3)
  AND hire_date BETWEEN '2021-01-01' AND '2022-12-31'   -- inclusive on both ends
  AND name LIKE 'A%'                                     -- % = any characters, _ = one character
ORDER BY dept_id ASC, salary DESC
LIMIT 10 OFFSET 20;                                      -- rows 21-30 (this is what pagination generates)
```

| Operator | Meaning |
|----------|---------|
| `=`, `<>`, `<`, `>`, `<=`, `>=` | Comparison (`<>` or `!=` for not equal) |
| `AND`, `OR`, `NOT` | Logic (use parentheses when mixing `AND` and `OR`) |
| `IN (a, b, c)` | Matches any in the list |
| `BETWEEN a AND b` | Range, inclusive |
| `LIKE` / `ILIKE` | Pattern match / case-insensitive (PostgreSQL) |
| `IS NULL`, `IS NOT NULL` | NULL checks |
| `DISTINCT` | Remove duplicate rows |

---

## 3. NULL rules

NULL means **unknown**, not zero or empty. Comparisons can be true, false, or unknown, and `WHERE` keeps only the true rows.

```sql
WHERE manager_id = NULL      -- WRONG: never true, returns 0 rows
WHERE manager_id IS NULL     -- right
```

| Rule | Example |
|------|---------|
| `= NULL` is never true, even `NULL = NULL` | Use `IS NULL` / `IS NOT NULL` |
| Aggregates ignore NULLs | `AVG`, `SUM`, `MIN`, `MAX` skip NULL values |
| `COUNT(*)` counts rows, `COUNT(col)` counts non-NULL | `COUNT(*)` = 10 but `COUNT(dept_id)` = 9 |
| `COALESCE(x, default)` returns the first non-NULL | `COALESCE(d.name, 'Unassigned')` |
| NULL in arithmetic gives NULL | `salary + NULL` is NULL |
| NULL sorts last in ascending order (PostgreSQL) | `NULLS FIRST` / `NULLS LAST` override it |
| **`NOT IN` trap** | One NULL in the subquery makes `NOT IN` return **no rows** |

```sql
-- Returns 0 rows if any employees.dept_id is NULL:
SELECT name FROM departments WHERE id NOT IN (SELECT dept_id FROM employees);

-- Safe version:
SELECT name FROM departments d
WHERE NOT EXISTS (SELECT 1 FROM employees e WHERE e.dept_id = d.id);
```

---

## 4. GROUP BY, aggregates and HAVING

```sql
SELECT dept_id,
       COUNT(*)              AS headcount,
       ROUND(AVG(salary), 2) AS avg_salary,
       SUM(salary)           AS payroll
FROM employees
WHERE dept_id IS NOT NULL         -- filters ROWS before grouping
GROUP BY dept_id
HAVING AVG(salary) > 1200000      -- filters GROUPS after aggregating
ORDER BY avg_salary DESC;
```

- Aggregates: `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`.
- **Rule:** every column in `SELECT` must be in `GROUP BY` or inside an aggregate. Otherwise: `column "..." must appear in the GROUP BY clause`.
- NULLs form their own group.
- Use `WHERE` when the condition is on a raw column. Use `HAVING` only when it involves an aggregate.

---

## 5. Joins

```sql
SELECT e.name, d.name AS department
FROM employees e
JOIN departments d ON e.dept_id = d.id;
```

| Join | Returns |
|------|---------|
| `INNER JOIN` (or `JOIN`) | Only rows that match on both sides |
| `LEFT JOIN` | All left rows. Right columns are NULL when there is no match |
| `RIGHT JOIN` | Mirror of LEFT (usually flip the table order and use LEFT) |
| `FULL JOIN` | All rows from both sides |
| `CROSS JOIN` | Every combination (Cartesian product) |

### Self join (hierarchies)

```sql
SELECT e.name AS employee, m.name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id;
```

### Anti-join: rows with no match (two ways)

```sql
-- 1. LEFT JOIN ... IS NULL
SELECT d.name FROM departments d
LEFT JOIN employees e ON e.dept_id = d.id
WHERE e.id IS NULL;

-- 2. NOT EXISTS
SELECT d.name FROM departments d
WHERE NOT EXISTS (SELECT 1 FROM employees e WHERE e.dept_id = d.id);
```

### `ON` vs `WHERE` with a LEFT JOIN

```sql
-- Vikram (no department) VANISHES: the WHERE rejects his NULL city.
SELECT e.name, d.name FROM employees e
LEFT JOIN departments d ON e.dept_id = d.id
WHERE d.city = 'Hyderabad';

-- Vikram stays, with a NULL department: the condition is part of the join.
SELECT e.name, d.name FROM employees e
LEFT JOIN departments d ON e.dept_id = d.id AND d.city = 'Hyderabad';
```

Filtering the **right** table of a LEFT JOIN in `WHERE` quietly turns it into an INNER JOIN.

### Double-counting warning

Joining one-to-many multiplies rows. Summing a column from the "one" side after the join inflates the total. Always sanity-check totals, or aggregate **before** joining.

---

## 6. Subqueries and CTEs

```sql
-- Scalar subquery
SELECT name, salary FROM employees
WHERE salary > (SELECT AVG(salary) FROM employees);

-- CTE: a named subquery, readable top to bottom
WITH dept_avg AS (
    SELECT dept_id, AVG(salary) AS avg_sal
    FROM employees
    WHERE dept_id IS NOT NULL
    GROUP BY dept_id
)
SELECT e.name, e.salary
FROM employees e
JOIN dept_avg d ON e.dept_id = d.dept_id
WHERE e.salary > d.avg_sal;
```

| Use | When |
|-----|------|
| Subquery | One small, simple inner step |
| CTE | More than one logical step, or the same result is used twice |
| `EXISTS` / `NOT EXISTS` | "Does a matching row exist?" Safe with NULLs |
| `IN` | Short lists or non-NULL subqueries |

---

## 7. Window functions

A `GROUP BY` aggregate **collapses** rows. A window function calculates over related rows **without collapsing them**: every row stays, with an extra computed column.

```sql
function() OVER (PARTITION BY <group> ORDER BY <sort>)
```

- `PARTITION BY` splits rows into independent groups (like `GROUP BY`, but rows survive).
- `ORDER BY` inside `OVER` sets the order within each partition.

| Function | What it does |
|----------|--------------|
| `ROW_NUMBER()` | 1, 2, 3... always unique. Ties get arbitrary order. |
| `RANK()` | Ties share a rank, then skips numbers (1, 2, 2, 4) |
| `DENSE_RANK()` | Ties share a rank, no gaps (1, 2, 2, 3) |
| `LAG(col)` / `LEAD(col)` | Previous / next row's value in the partition |
| `SUM(col) OVER (...)` | No `ORDER BY`: partition total on every row. With `ORDER BY`: running total |
| `AVG`, `COUNT`, `MIN`, `MAX` `OVER (...)` | Same idea with other aggregates |

```sql
-- Compare the three ranking functions (name the window once)
SELECT name, salary,
       ROW_NUMBER() OVER w AS row_num,
       RANK()       OVER w AS rnk,
       DENSE_RANK() OVER w AS dense_rnk
FROM employees
WINDOW w AS (ORDER BY salary DESC);

-- Previous hire's salary and a running total per department
SELECT name, dept_id, hire_date, salary,
       LAG(salary) OVER (PARTITION BY dept_id ORDER BY hire_date) AS prev_salary,
       SUM(salary) OVER (PARTITION BY dept_id ORDER BY hire_date) AS running_total
FROM employees
WHERE dept_id IS NOT NULL;
```

**Which ranking function?**
- "Top N per group": `ROW_NUMBER`
- "Nth highest value": `DENSE_RANK`
- When ties should leave gaps (competition ranking): `RANK`

---

## 8. Interview patterns

### Nth highest salary (here N = 2)

```sql
WITH ranked AS (
    SELECT name, salary, DENSE_RANK() OVER (ORDER BY salary DESC) AS dr
    FROM employees
)
SELECT name, salary FROM ranked WHERE dr = 2;
```

### Duplicates

```sql
SELECT salary, COUNT(*) FROM employees
GROUP BY salary
HAVING COUNT(*) > 1;
```

### Top N per group

```sql
WITH ranked AS (
    SELECT name, dept_id, salary,
           ROW_NUMBER() OVER (PARTITION BY dept_id ORDER BY salary DESC) AS rn
    FROM employees
)
SELECT * FROM ranked WHERE rn <= 2;
```

### Rows with no match

Use the anti-join patterns in section 5.

### Percent of group total

```sql
SELECT name, ROUND(salary * 100.0 / SUM(salary) OVER (PARTITION BY dept_id), 1) AS pct_of_dept
FROM employees
WHERE dept_id IS NOT NULL;
```

### Bucketing with CASE

```sql
SELECT CASE WHEN salary >= 1500000 THEN 'High'
            WHEN salary >= 1000000 THEN 'Mid'
            ELSE 'Entry' END AS band,
       COUNT(*)
FROM employees
GROUP BY 1;
```

### Employees earning more than their manager

```sql
SELECT e.name FROM employees e
JOIN employees m ON e.manager_id = m.id
WHERE e.salary > m.salary;
```

---

## 9. Common errors and their causes

| Error or symptom | Cause |
|------------------|-------|
| `column "x" must appear in the GROUP BY clause` | A selected column is neither grouped nor aggregated |
| `column "alias" does not exist` in `WHERE` | `WHERE` runs before `SELECT`, so the alias isn't defined yet |
| `window functions are not allowed in WHERE` | Windows run after `WHERE`. Use a CTE, then filter outside |
| `= NULL` returns 0 rows | NULL comparisons are unknown. Use `IS NULL` |
| `NOT IN (subquery)` returns 0 rows | The subquery contains a NULL. Use `NOT EXISTS` |
| LEFT JOIN loses unmatched rows | A `WHERE` condition on the right table rejected the NULLs. Move it to `ON` |
| Totals too large after a join | One-to-many join duplicated rows |
| `more than one row returned by a subquery` | A scalar subquery returned several rows. Use `IN`, aggregate, or `LIMIT 1` |
| `division by zero` | Guard with `NULLIF(denominator, 0)` |
| Integer division gives 0 | `1/2` is `0` in integer math. Use `1.0/2` or a cast |

---

## 10. Handy functions (PostgreSQL)

| Task | Example |
|------|---------|
| Round | `ROUND(x, 2)` |
| First non-NULL | `COALESCE(a, b, 'default')` |
| NULL if equal | `NULLIF(a, 0)` |
| String | `UPPER()`, `LOWER()`, `LENGTH()`, `TRIM()`, `SUBSTRING()`, `a \|\| b` |
| Date now | `CURRENT_DATE`, `NOW()` |
| Date parts | `EXTRACT(YEAR FROM hire_date)` |
| Date arithmetic | `CURRENT_DATE - hire_date` (days), `hire_date + INTERVAL '1 month'` |
| Type cast | `salary::numeric` or `CAST(salary AS numeric)` |
| Conditional | `CASE WHEN ... THEN ... ELSE ... END` |

---

## 11. psql commands

| Command | Does |
|---------|------|
| `\l` | List databases |
| `\c dbname` | Connect to a database |
| `\dt` | List tables |
| `\d table` | Describe a table |
| `\i file.sql` | Run a SQL file |
| `\timing` | Show query time |
| `\q` | Quit |

---

## 12. One-page memory aid

```
Order:      FROM → WHERE → GROUP BY → HAVING → SELECT → ORDER BY → LIMIT
NULL:       IS NULL, COALESCE, avoid NOT IN with NULLs
Filter:     WHERE = rows, HAVING = groups
Joins:      INNER (matches) | LEFT (keep left) | anti-join (LEFT ... IS NULL / NOT EXISTS)
Windows:    OVER (PARTITION BY ... ORDER BY ...), rows are kept
Ranking:    ROW_NUMBER unique | RANK gaps | DENSE_RANK no gaps
Patterns:   Nth highest = DENSE_RANK | top N per group = ROW_NUMBER in a CTE
```
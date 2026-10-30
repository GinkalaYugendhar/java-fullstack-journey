-- ============================================================
-- Day 5: SQL practice queries (PostgreSQL)
-- Load first:  \i sql/day05/schema.sql
-- Tip: attempt each query yourself BEFORE reading the answer.
-- ============================================================


-- ############################################################
-- SECTION A: BASICS
-- ############################################################

-- Q1. Engineering employees earning more than 1,300,000,
--     highest salary first, top 2 only.
--     Expected: Asha 2400000, Ravi 1800000
SELECT name, salary
FROM employees
WHERE dept_id = 1
  AND salary > 1300000
ORDER BY salary DESC
LIMIT 2;


-- Q2. Employees hired between 2021-01-01 and 2022-12-31, oldest hire first.
--     BETWEEN is inclusive on both ends.
--     Expected: Meera, Arjun, Divya, Kiran
SELECT name, hire_date
FROM employees
WHERE hire_date BETWEEN '2021-01-01' AND '2022-12-31'
ORDER BY hire_date;


-- Q3. Employees who have no manager, alphabetically.
--     Use IS NULL, never "= NULL".
--     Expected: Asha, Rahul, Sana, Vikram
SELECT name
FROM employees
WHERE manager_id IS NULL
ORDER BY name;


-- ############################################################
-- SECTION B: AGGREGATES
-- ############################################################

-- Q4. One summary row: headcount, how many have a department,
--     average salary, lowest and highest salary.
--     Expected: 10 | 9 | 1390000.00 | 800000 | 2400000
--     COUNT(*) counts rows; COUNT(dept_id) skips NULLs.
SELECT COUNT(*)              AS headcount,
       COUNT(dept_id)        AS with_dept,
       ROUND(AVG(salary), 2) AS avg_salary,
       MIN(salary)           AS lowest,
       MAX(salary)           AS highest
FROM employees;


-- Q5. Per department: headcount, average salary, total payroll.
--     NULL dept_id forms its own group (sorted last in PostgreSQL).
--     Expected: 1 -> 4, 1725000.00, 6900000
--               2 -> 3, 1266666.67, 3800000
--               3 -> 2, 1100000.00, 2200000
--               NULL -> 1, 1000000.00, 1000000
SELECT dept_id,
       COUNT(*)              AS headcount,
       ROUND(AVG(salary), 2) AS avg_salary,
       SUM(salary)           AS payroll
FROM employees
GROUP BY dept_id
ORDER BY dept_id;


-- Q6. Only departments whose average salary is above 1,200,000,
--     highest average first (ignore employees with no department).
--     WHERE removes Vikram BEFORE grouping; HAVING removes HR AFTER grouping.
--     Expected: 1 -> 4, 1725000.00
--               2 -> 3, 1266666.67
SELECT dept_id,
       COUNT(*)              AS headcount,
       ROUND(AVG(salary), 2) AS avg_salary
FROM employees
WHERE dept_id IS NOT NULL
GROUP BY dept_id
HAVING AVG(salary) > 1200000
ORDER BY avg_salary DESC;


-- ############################################################
-- SECTION C: JOINS
-- ############################################################

-- Q7. Each employee with department name and city.
--     INNER JOIN drops Vikram (no department). Expected: 9 rows.
SELECT e.name,
       d.name AS department,
       d.city
FROM employees e
JOIN departments d ON e.dept_id = d.id
ORDER BY e.id;


-- Q8. All employees, "Unassigned" when there is no department.
--     LEFT JOIN keeps Vikram. Expected: 10 rows.
SELECT e.name,
       COALESCE(d.name, 'Unassigned') AS department
FROM employees e
LEFT JOIN departments d ON e.dept_id = d.id
ORDER BY e.id;


-- Q9. Departments that have no employees. Expected: Marketing
--     Version 1: anti-join with LEFT JOIN ... IS NULL
SELECT d.name
FROM departments d
LEFT JOIN employees e ON e.dept_id = d.id
WHERE e.id IS NULL;

--     Version 2: NOT EXISTS (same result)
SELECT d.name
FROM departments d
WHERE NOT EXISTS (SELECT 1 FROM employees e WHERE e.dept_id = d.id);


-- Q10. Each employee with their manager's name (NULL if none).
--      Self join: same table twice, under two aliases.
--      Expected: Asha NULL, Ravi Asha, Meera Asha, Kiran Ravi, Sana NULL,
--                Arjun Sana, Divya Sana, Rahul NULL, Neha Rahul, Vikram NULL
SELECT e.name AS employee,
       m.name AS manager
FROM employees e
LEFT JOIN employees m ON e.manager_id = m.id
ORDER BY e.id;


-- ############################################################
-- SECTION D: SUBQUERIES AND CTEs
-- ############################################################

-- Q11. Employees earning more than the company average (1,390,000).
--      Expected: Asha, Sana, Ravi, Meera, Rahul (Rahul is only 10,000 above).
SELECT name, salary
FROM employees
WHERE salary > (SELECT AVG(salary) FROM employees)
ORDER BY salary DESC;


-- Q12. Employees earning more than the average of their OWN department.
--      Expected: Asha 2400000 (1725000), Ravi 1800000 (1725000),
--                Sana 2000000 (1266667), Rahul 1400000 (1100000)
WITH dept_avg AS (
    SELECT dept_id, AVG(salary) AS avg_sal
    FROM employees
    WHERE dept_id IS NOT NULL
    GROUP BY dept_id
)
SELECT e.name,
       e.salary,
       ROUND(d.avg_sal, 0) AS dept_avg
FROM employees e
JOIN dept_avg d ON e.dept_id = d.dept_id
WHERE e.salary > d.avg_sal
ORDER BY e.id;


-- ############################################################
-- SECTION E: WINDOW FUNCTIONS
-- ############################################################

-- Q13. Rank everyone by salary: ROW_NUMBER vs RANK vs DENSE_RANK.
--      Watch Arjun and Divya (tie at 900000) and Neha after them:
--        ROW_NUMBER: 8, 9, 10 (unique; which tie gets 8 is not guaranteed)
--        RANK:       8, 8, 10 (gap after the tie)
--        DENSE_RANK: 8, 8, 9  (no gap)
SELECT name,
       salary,
       ROW_NUMBER() OVER w AS row_num,
       RANK()       OVER w AS rnk,
       DENSE_RANK() OVER w AS dense_rnk
FROM employees
WINDOW w AS (ORDER BY salary DESC)
ORDER BY salary DESC, name;


-- Q14. Highest-paid employee in each department (top N per group).
--      Windows run after WHERE, so compute the rank in a CTE, then filter.
--      Change rn = 1 to rn <= 2 for the top two per department.
--      Expected: Engineering Asha, HR Rahul, Sales Sana
WITH ranked AS (
    SELECT e.name,
           d.name AS department,
           e.salary,
           ROW_NUMBER() OVER (PARTITION BY e.dept_id ORDER BY e.salary DESC) AS rn
    FROM employees e
    JOIN departments d ON e.dept_id = d.id
)
SELECT department, name, salary
FROM ranked
WHERE rn = 1
ORDER BY department;


-- Q15. Per department, in hiring order: salary, previous hire's salary,
--      and running payroll total.
--      Sanity check: the last running total per department equals Q5's payroll
--      (6900000 / 3800000 / 2200000).
SELECT name,
       dept_id,
       hire_date,
       salary,
       LAG(salary) OVER (PARTITION BY dept_id ORDER BY hire_date) AS prev_hire_salary,
       SUM(salary) OVER (PARTITION BY dept_id ORDER BY hire_date) AS running_payroll
FROM employees
WHERE dept_id IS NOT NULL
ORDER BY dept_id, hire_date;


-- ############################################################
-- PART 4: BREAK THINGS ON PURPOSE
-- Queries that raise errors are commented out so the file runs cleanly.
-- Uncomment ONE at a time, run it, read the error, and write one
-- sentence in your notes about why.
-- ############################################################

-- B1. "= NULL" is never true -> 0 rows. Compare with IS NULL (Q3).
SELECT name FROM employees WHERE manager_id = NULL;

-- B2. Alias in WHERE -> ERROR (WHERE runs before SELECT).
-- SELECT salary * 12 AS monthly_times_12
-- FROM employees
-- WHERE monthly_times_12 > 0;

-- B3. Column missing from GROUP BY -> ERROR.
-- SELECT dept_id, name, COUNT(*)
-- FROM employees
-- GROUP BY dept_id;

-- B4. Window function in WHERE -> ERROR (windows run after WHERE).
-- SELECT name
-- FROM employees
-- WHERE ROW_NUMBER() OVER (ORDER BY salary) = 1;

-- B5. NOT IN + NULL trap -> 0 rows (you expected Marketing).
--     Vikram's NULL dept_id is in the subquery, so 4 NOT IN (1,2,3,NULL)
--     is unknown, not true. Compare with Q9's NOT EXISTS version.
SELECT name
FROM departments
WHERE id NOT IN (SELECT dept_id FROM employees);

-- B6a. LEFT JOIN turned into an INNER JOIN: Vikram vanishes.
SELECT e.name, d.name AS department
FROM employees e
LEFT JOIN departments d ON e.dept_id = d.id
WHERE d.city = 'Hyderabad';

-- B6b. Condition moved into ON: Vikram is back with a NULL department.
--      Different meaning: "join only Hyderabad departments, keep everyone".
SELECT e.name, d.name AS department
FROM employees e
LEFT JOIN departments d ON e.dept_id = d.id AND d.city = 'Hyderabad';


-- ############################################################
-- PART 5: EXTENSION CHALLENGES (suggested solutions)
-- Try them yourself first.
-- ############################################################

-- X1. Second-highest DISTINCT salary (famous interview question).
--     DENSE_RANK so ties don't create gaps. Expected: Sana 2000000
WITH ranked AS (
    SELECT name,
           salary,
           DENSE_RANK() OVER (ORDER BY salary DESC) AS dr
    FROM employees
)
SELECT name, salary
FROM ranked
WHERE dr = 2;

-- X1 alternative without window functions (returns the value only):
SELECT MAX(salary) AS second_highest
FROM employees
WHERE salary < (SELECT MAX(salary) FROM employees);


-- X2. Duplicate detection: salaries shared by more than one employee.
--     Expected: 900000 | 2
SELECT salary, COUNT(*) AS employee_count
FROM employees
GROUP BY salary
HAVING COUNT(*) > 1;


-- X3. Percent of department payroll. Each department should add up to ~100.
SELECT name,
       dept_id,
       salary,
       ROUND(salary * 100.0 / SUM(salary) OVER (PARTITION BY dept_id), 1) AS pct_of_dept
FROM employees
WHERE dept_id IS NOT NULL
ORDER BY dept_id, salary DESC;

-- X3 check: percentages per department sum to about 100.
WITH pct AS (
    SELECT dept_id,
           salary * 100.0 / SUM(salary) OVER (PARTITION BY dept_id) AS p
    FROM employees
    WHERE dept_id IS NOT NULL
)
SELECT dept_id, ROUND(SUM(p), 1) AS total_pct
FROM pct
GROUP BY dept_id
ORDER BY dept_id;


-- X4. Salary bands with CASE, then count per band.
--     High >= 1,500,000 | Mid 1,000,000 to 1,499,999 | Entry below that
WITH banded AS (
    SELECT name,
           salary,
           CASE
               WHEN salary >= 1500000 THEN 'High'
               WHEN salary >= 1000000 THEN 'Mid'
               ELSE 'Entry'
           END AS band
    FROM employees
)
SELECT band, COUNT(*) AS employee_count
FROM banded
GROUP BY band
ORDER BY MIN(salary) DESC;


-- X5. Three-table join: employee, department, project (tables from schema.sql).
SELECT e.name       AS employee,
       d.name       AS department,
       p.name       AS project,
       a.hours
FROM assignments a
JOIN employees   e ON a.employee_id = e.id
JOIN projects    p ON a.project_id  = p.id
LEFT JOIN departments d ON e.dept_id = d.id
ORDER BY e.id, p.id;

-- X5 double-counting check. Joining one-to-many multiplies rows.
-- Wrong: payroll inflated because Ravi appears in 2 assignment rows.
SELECT SUM(e.salary) AS inflated_payroll
FROM employees e
JOIN assignments a ON a.employee_id = e.id;

-- Right: sum salaries over distinct employees (or aggregate BEFORE joining).
SELECT SUM(e.salary) AS correct_payroll_of_assigned
FROM employees e
WHERE EXISTS (SELECT 1 FROM assignments a WHERE a.employee_id = e.id);

-- Total hours per project (safe: aggregating the many side).
SELECT p.name AS project,
       COUNT(*)     AS people,
       SUM(a.hours) AS total_hours
FROM projects p
JOIN assignments a ON a.project_id = p.id
GROUP BY p.name
ORDER BY total_hours DESC;
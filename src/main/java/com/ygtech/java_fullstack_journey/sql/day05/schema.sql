-- ============================================================
-- Day 5: practice schema and data (PostgreSQL)
-- Run:  psql -d learn_sql -f sql/day05/schema.sql
--   or: \i sql/day05/schema.sql   (inside psql)
--
-- Deliberate edge cases:
--   * Marketing has no employees
--   * Vikram has no department (dept_id is NULL)
--   * Arjun and Divya have the same salary (900000)
-- ============================================================

CREATE SCHEMA IF NOT EXISTS public;

SET search_path TO public;

DROP TABLE IF EXISTS assignments;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS employees;
DROP TABLE IF EXISTS departments;

CREATE TABLE departments (
    id   INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL
);

CREATE TABLE employees (
    id         INT PRIMARY KEY,
    name       VARCHAR(50) NOT NULL,
    dept_id    INT REFERENCES departments(id),   -- nullable on purpose
    manager_id INT REFERENCES employees(id),     -- self-reference
    salary     INT NOT NULL,                     -- yearly, in INR
    hire_date  DATE NOT NULL
);

INSERT INTO departments VALUES
(1, 'Engineering', 'Hyderabad'),
(2, 'Sales',       'Mumbai'),
(3, 'HR',          'Hyderabad'),
(4, 'Marketing',   'Bengaluru');   -- no employees on purpose

INSERT INTO employees VALUES
(1,  'Asha',   1,    NULL, 2400000, '2019-04-10'),
(2,  'Ravi',   1,    1,    1800000, '2020-07-15'),
(3,  'Meera',  1,    1,    1500000, '2021-01-20'),
(4,  'Kiran',  1,    2,    1200000, '2022-06-01'),
(5,  'Sana',   2,    NULL, 2000000, '2018-03-12'),
(6,  'Arjun',  2,    5,     900000, '2021-09-05'),
(7,  'Divya',  2,    5,     900000, '2022-02-14'),
(8,  'Rahul',  3,    NULL, 1400000, '2019-11-25'),
(9,  'Neha',   3,    8,     800000, '2023-05-08'),
(10, 'Vikram', NULL, NULL, 1000000, '2023-08-01');   -- no department on purpose

-- ------------------------------------------------------------
-- Extra tables for extension challenge 5 (three-table join)
-- ------------------------------------------------------------
CREATE TABLE projects (
    id   INT PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

CREATE TABLE assignments (
    employee_id INT NOT NULL REFERENCES employees(id),
    project_id  INT NOT NULL REFERENCES projects(id),
    hours       INT NOT NULL,
    PRIMARY KEY (employee_id, project_id)
);

INSERT INTO projects VALUES
(1, 'Payments Platform'),
(2, 'Mobile App'),
(3, 'Hiring Portal');

INSERT INTO assignments VALUES
(1, 1, 120),
(2, 1, 200),
(2, 2,  80),
(3, 2, 160),
(4, 1, 100),
(5, 3,  40),
(8, 3,  90),
(9, 3, 110);

-- ------------------------------------------------------------
-- Sanity checks (expected: 10, 4, 3, 8)
-- ------------------------------------------------------------
SELECT COUNT(*) AS employees   FROM employees;     -- 10
SELECT COUNT(*) AS departments FROM departments;   -- 4
SELECT COUNT(*) AS projects    FROM projects;      -- 3
SELECT COUNT(*) AS assignments FROM assignments;   -- 8
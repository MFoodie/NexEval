SET NAMES utf8mb4 COLLATE utf8mb4_0900_ai_ci;

SOURCE /docker-entrypoint-initdb.d/batch_import/courses.sql;
SOURCE /docker-entrypoint-initdb.d/batch_import/teachers.sql;
SOURCE /docker-entrypoint-initdb.d/batch_import/students.sql;
SOURCE /docker-entrypoint-initdb.d/batch_import/classes.sql;
SOURCE /docker-entrypoint-initdb.d/batch_import/SClist.sql;

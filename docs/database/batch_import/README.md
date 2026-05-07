# batch_import

这里放的是从根目录 5 个 Excel 生成的可重复导入 SQL。

导入顺序建议：
1. courses.sql
2. teachers.sql
3. students.sql
4. classes.sql
5. SClist.sql

说明：
- students.sql 和 teachers.sql 都会先导入 users 表，再导入 student / teacher 表。
- 文件里已经加了 `SET NAMES utf8mb4`，适合 Docker MySQL 直接执行。
- 如果需要重置数据库，执行 `docker compose -f infra/docker-compose.yml down -v` 后再 `up -d` 即可，`docs/database/99_batch_import.sql` 会在首次初始化时自动执行这里的脚本。

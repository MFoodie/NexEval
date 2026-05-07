# NexEval Docker（Windows + Docker Desktop）

本目录提供前端、后端、MySQL、Redis 的一体化容器编排。

## 文件说明

- `docker-compose.yml`：整体编排
- `client.Dockerfile`：Vue 前端生产镜像（Nginx 托管）
- `server.Dockerfile`：Spring Boot 后端镜像
- `nginx.conf`：前端静态资源与 `/ws`、`/avatar` 反向代理

## 启动前准备

1. 启动 Docker Desktop（确保左下角显示 Engine running）
2. 确保 80 / 8080 / 3306 / 6379 端口未被占用

## 一键启动

在仓库根目录执行：

```powershell
cd D:\NexEval
docker compose -f infra/docker-compose.yml up -d --build
```

## 访问地址

- 前端：http://localhost
- 后端（容器内 HTTP）：http://localhost:8080
- MySQL：localhost:3306
- Redis：localhost:6379

## 数据库初始化说明

`docker-compose.yml` 已挂载 `../docs/database` 到 MySQL 的 `/docker-entrypoint-initdb.d`：

- 第一次创建数据卷时会自动执行 SQL 脚本
- 如果你之前已经启动过 MySQL 容器并保留了旧数据卷，脚本不会再次自动执行

如需重置并重新初始化：

```powershell
cd D:\NexEval
docker compose -f infra/docker-compose.yml down -v
docker compose -f infra/docker-compose.yml up -d --build
```

## 常用命令

查看状态：

```powershell
docker compose -f infra/docker-compose.yml ps
```

查看日志：

```powershell
docker compose -f infra/docker-compose.yml logs -f frontend
docker compose -f infra/docker-compose.yml logs -f backend
docker compose -f infra/docker-compose.yml logs -f mysql
```

停止并移除容器：

```powershell
docker compose -f infra/docker-compose.yml down
docker compose -f infra/docker-compose.yml down -v    # 停止并删除容器，同时删除数据库数据卷（重置 DB 用）
```

后台启动（不重建镜像）：

```powershell
docker compose -f infra/docker-compose.yml up -d
```

若需要强制重建镜像并启动：

```powershell
docker compose -f infra/docker-compose.yml up -d --build
```

## 新功能提示

- 支持题目带图片（题库表新增 `image_path` 与 `image_mode`，`image_mode` 为 DECIMAL(3,2)），容器映射会把 `docs/database` 挂载到 MySQL 初始化目录以执行建表脚本。
- 大题（主观题）答案可包含图片，后端保存答案图片路径（`answerImagePath`），前端可在答题页上传并预览图片（需启用后端上传端点且容器内目录有写权限）。

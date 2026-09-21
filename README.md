# 校园社区信息交流平台

一个采用 Vue 3 + Spring Boot 构建的校园社区平台，包含注册登录、频道发帖、Markdown 正文、图片上传、点赞评论、举报和后台管理。

## 本轮更新

- 发帖正文支持 Markdown，并提供编辑/预览切换和常用格式快捷按钮。
- 帖子详情会安全渲染 Markdown；用户输入的原始 HTML 不会直接执行。
- 桌面端社区首页扩大了帖子流，帖子详情和评论区也使用更宽的阅读布局。
- 图片存储支持本地目录与 MinIO 切换；生产配置默认使用 MinIO。
- 增加 GitHub Pages 自动发布流程，前端可通过公开网址访问。
- 本轮没有重新设计移动端布局。

## 技术栈

- 后端：Java 17、Spring Boot 3.5、Spring Security、MyBatis-Plus、Flyway
- 前端：Vue 3、TypeScript、Vite、Vue Router、Pinia
- 数据库：开发环境默认 H2，生产环境支持 MySQL 8
- 图片存储：本地目录或 MinIO/S3 兼容对象存储

## 本地运行

### 1. 启动后端

```powershell
cd backend
mvn spring-boot:run
```

默认地址为 `http://localhost:8080`，健康检查为 `http://localhost:8080/api/health`。

开发环境默认不会创建管理员。需要开发管理员时，请在启动后端前设置
`DEV_ADMIN_ENABLED=true`、`DEV_ADMIN_USERNAME` 和 `DEV_ADMIN_PASSWORD`；密码只保存在本机环境变量中。

### 2. 启动前端

```powershell
cd frontend
npm install
npm run dev
```

默认地址为 `http://localhost:5173`，开发服务器会将 API 请求转发到后端。

## 使用 MinIO

项目根目录的 `docker-compose.yml` 已包含 MySQL 和 MinIO。启动基础服务：

```powershell
Copy-Item .env.example .env
# 编辑 .env，为数据库和 MinIO 填写各自的强密码
docker compose up -d mysql minio
```

MinIO API 默认位于 `http://localhost:9000`，管理控制台位于 `http://localhost:9001`。

本地用 MinIO 启动后端：

```powershell
$env:STORAGE_TYPE="minio"
$env:MINIO_ENDPOINT="http://localhost:9000"
$env:MINIO_ACCESS_KEY="<与 .env 中 MINIO_ROOT_USER 一致>"
$env:MINIO_SECRET_KEY="<与 .env 中 MINIO_ROOT_PASSWORD 一致>"
$env:MINIO_BUCKET="campus-community"
cd backend
mvn spring-boot:run
```

后端会在首次上传时自动创建存储桶。存储桶无需开放匿名访问，浏览器通过后端的公开图片读取接口获取帖子图片。

生产环境必须替换示例密钥，并为 MinIO 数据目录配置持久化和备份。

## GitHub 托管与网址访问

可以把完整代码存放在 GitHub，但 GitHub Pages 只能运行构建后的静态前端，不能运行 Spring Boot、MySQL 或 MinIO。因此完整上线结构是：

```text
GitHub 仓库
  └─ GitHub Pages：Vue 前端
       ├─ HTTPS 请求 → 已部署的 Spring Boot API
       ├─ API → MySQL
       └─ API → MinIO
```

仓库已经包含 `.github/workflows/deploy-pages.yml`。配置步骤：

1. 将项目初始化为 Git 仓库并推送到 GitHub 的 `main` 分支。
2. 先把后端、MySQL 和 MinIO 部署到一台具有 HTTPS 公网地址的服务器或云平台。
3. 在 GitHub 仓库的 `Settings → Secrets and variables → Actions → Variables` 中添加：
   - `VITE_API_BASE_URL`：后端公开地址，例如 `https://api.example.edu`。
   - 可选 `VITE_BASE_PATH`：普通项目 Pages 默认会自动使用 `/仓库名/`；自定义域名时设置为 `/`。
4. 在 `Settings → Pages` 中把发布来源设为 `GitHub Actions`。
5. 后端设置 `CORS_ALLOWED_ORIGINS` 为 Pages 地址，例如 `https://your-name.github.io`。
6. 推送到 `main` 后，Actions 会自动构建并发布前端。

普通仓库的访问地址通常是：

```text
https://你的用户名.github.io/仓库名/
```

工作流使用 Hash 路由，帖子详情等深层页面在 GitHub Pages 上刷新时不会出现静态站点 404。

## 生产环境变量

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/campus_community?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME="campus"
$env:DB_PASSWORD="replace-me"
$env:JWT_SECRET="replace-with-a-random-secret-at-least-32-characters"
$env:CORS_ALLOWED_ORIGINS="https://your-name.github.io"
$env:STORAGE_TYPE="minio"
$env:MINIO_ENDPOINT="http://localhost:9000"
$env:MINIO_ACCESS_KEY="replace-me"
$env:MINIO_SECRET_KEY="replace-me"
$env:MINIO_BUCKET="campus-community"
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=prod"
```

不要把数据库密码、JWT 密钥或 MinIO 密钥提交到 GitHub。

# GSHMD（无数据库版本）

一个基于 Spring Boot 静态托管的文章查看器：

- **不依赖数据库**（无 MySQL / MyBatis）。
- 文章与评论优先使用**浏览器本地存储**。
- 可选使用 **GitHub Gist 文件同步**做跨设备数据共享。
- 列表页与详情页均支持移动端优化交互。

## 功能亮点

- 📰 文章列表分页加载（读取静态 `posts-*.json`）
- 🔎 关键词搜索 + 搜索历史
- ✍️ 添加文章：自动草稿、快捷键发布（Ctrl/Cmd + Enter）
- ☁️ 可选 Gist 文件同步（非必须）
- 💬 详情页评论发布与排序（最早/最新）

## 本地运行

```bash
./mvnw spring-boot:run
```

访问：`http://localhost:8080/index.html`

## 数据存储策略

- **静态只读数据**：`src/main/resources/static/posts-*.json`、`comments/*.json`
- **用户新增文章**：浏览器 `localStorage`（键：`gshmd_local_posts_v3`）
- **用户新增评论**：浏览器 `localStorage`（键：`gshmd_local_comments_v1`）
- **草稿**：浏览器 `localStorage`（键：`gshmd_post_draft_v1`）
- **可选云端同步**：GitHub Gist 文件（需手动配置 token）

## 说明

该项目现已改造为静态优先架构，适合部署在 GitHub Pages 或任意静态托管环境。

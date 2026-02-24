# GSHMD 帖子查看器

一个基于 Spring Boot + Thymeleaf 的帖子查看器，支持静态部署到 GitHub Pages。

## 📖 功能

- 📋 帖子列表展示
- 🔍 内容搜索
- 💬 评论查看
- 📱 响应式设计

## 🚀 在线访问

[GitHub Pages](https://你的用户名.github.io/gshmd/)

## 🛠 本地开发

### 环境要求
- Java 17+
- Maven 3.6+
- MySQL 8.0+

### 运行步骤

1. 创建数据库
```sql
CREATE DATABASE gshmd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置数据库连接
编辑 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/gshmd
    username: your_username
    password: your_password
```

3. 运行应用
```bash
./mvnw spring-boot:run
```

4. 访问 http://localhost:8080

## 📦 部署到 GitHub Pages

### 1. 导出数据库数据

```bash
./mvnw spring-boot:run -Dspring-boot.run.mainClass=com.example.viewer.ExportData
```

### 2. 提交并推送

```bash
git add .
git commit -m "Deploy to GitHub Pages"
git push origin main
```

GitHub Actions 会自动构建并部署到 GitHub Pages。

## 📁 项目结构

```
src/
├── main/
│   ├── java/com/example/viewer/
│   │   ├── GshmdApplication.java    # 主应用
│   │   ├── controller/               # 控制器
│   │   ├── entity/                   # 实体类
│   │   └── mapper/                   # MyBatis Mapper
│   └── resources/
│       ├── static/                   # 静态文件 (GitHub Pages)
│       │   ├── index.html           # 列表页
│       │   ├── post.html            # 详情页
│       │   └── data.json            # 导出数据
│       ├── templates/                # Thymeleaf 模板
│       └── application.yml           # 配置文件
```

## 📝 许可证

MIT

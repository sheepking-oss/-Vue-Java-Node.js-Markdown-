# 知识文库 - 多人协作 Markdown 知识库系统

一个基于 Vue 3 + Spring Boot + Node.js 的现代化多人协作 Markdown 知识库平台。

## 技术栈

### 前端
- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - Vue 3 组件库
- **Pinia** - 状态管理
- **Vue Router** - 路由管理
- **Axios** - HTTP 客户端
- **Socket.IO Client** - WebSocket 客户端
- **Marked** - Markdown 解析器
- **DOMPurify** - HTML 安全过滤
- **Highlight.js** - 代码高亮
- **Day.js** - 日期处理

### 后端 (Java)
- **Spring Boot 3.2** - 应用框架
- **Spring Security** - 安全框架
- **Spring Data JPA** - 数据访问
- **Hibernate Search + Lucene** - 全文搜索
- **MySQL** - 关系型数据库
- **JWT** - 身份认证
- **Lombok** - 代码简化

### 实时服务 (Node.js)
- **Node.js** - 服务端运行时
- **Express** - Web 框架
- **Socket.IO** - WebSocket 服务
- **JSON Web Token** - JWT 验证
- **CORS** - 跨域支持

## 核心功能

### 📝 文档管理
- **Markdown 编辑器** - 实时编辑和预览
- **目录树** - 层级化文档结构
- **文档草稿** - 自动保存未完成内容
- **自动保存** - 定时自动保存
- **版本历史** - 完整版本记录
- **版本回滚** - 一键恢复任意版本

### 🏷️ 标签与搜索
- **标签管理** - 创建、编辑、删除标签
- **文档标签** - 多标签分类
- **全文搜索** - 基于标题和内容搜索
- **搜索结果高亮** - 快速定位关键词

### 💬 协作与评论
- **在线编辑状态** - 实时显示协作者
- **协作者头像** - 显示当前编辑人员
- **评论系统** - 文档评论和回复
- **实时通知** - 新评论和保存通知

### 🔐 权限管理
- **团队空间** - 独立工作空间
- **角色权限** - 所有者/管理员/编辑者/查看者
- **成员管理** - 添加、移除、更改角色
- **权限控制** - 细粒度的访问控制

### 🔗 文档分享
- **分享链接** - 生成公开访问链接
- **权限设置** - 仅查看/可编辑
- **密码保护** - 可选访问密码
- **过期时间** - 设置链接有效期

### 🗑️ 回收站
- **软删除** - 文档移至回收站
- **恢复功能** - 从回收站恢复
- **永久删除** - 彻底清除数据

## 项目结构

```
knowledge-base/
├── client/                    # Vue 3 前端
│   ├── src/
│   │   ├── components/       # 公共组件
│   │   ├── layouts/          # 布局组件
│   │   │   └── MainLayout.vue
│   │   ├── router/           # 路由配置
│   │   │   └── index.js
│   │   ├── services/         # 服务层
│   │   │   ├── api.js        # API 服务
│   │   │   └── websocket.js  # WebSocket 服务
│   │   ├── stores/           # 状态管理
│   │   │   ├── auth.js
│   │   │   ├── document.js
│   │   │   ├── space.js
│   │   │   └── ui.js
│   │   ├── styles/           # 样式文件
│   │   │   └── index.scss
│   │   ├── views/            # 页面视图
│   │   │   ├── Login.vue
│   │   │   ├── Register.vue
│   │   │   ├── Spaces.vue
│   │   │   ├── SpaceDetail.vue
│   │   │   ├── DocumentList.vue
│   │   │   ├── DocumentEditor.vue
│   │   │   ├── Trash.vue
│   │   │   ├── Members.vue
│   │   │   ├── SpaceSettings.vue
│   │   │   └── ShareAccess.vue
│   │   ├── App.vue
│   │   └── main.js
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
├── server/                    # Java 后端
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/knowledge/base/
│   │   │   │   ├── controller/      # 控制器
│   │   │   │   │   ├── AuthController.java
│   │   │   │   │   ├── DocumentController.java
│   │   │   │   │   ├── SpaceController.java
│   │   │   │   │   ├── TagController.java
│   │   │   │   │   ├── CommentController.java
│   │   │   │   │   └── ShareController.java
│   │   │   │   ├── dto/              # 数据传输对象
│   │   │   │   ├── entity/           # 实体类
│   │   │   │   │   ├── Document.java
│   │   │   │   │   ├── DocumentVersion.java
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Space.java
│   │   │   │   │   ├── Tag.java
│   │   │   │   │   ├── Comment.java
│   │   │   │   │   ├── Share.java
│   │   │   │   │   └── SpaceMember.java
│   │   │   │   ├── repository/       # 数据访问层
│   │   │   │   ├── security/         # 安全配置
│   │   │   │   │   ├── SecurityConfig.java
│   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   ├── UserPrincipal.java
│   │   │   │   │   └── CustomUserDetailsService.java
│   │   │   │   ├── service/          # 业务逻辑层
│   │   │   │   │   ├── DocumentService.java
│   │   │   │   │   ├── SpaceService.java
│   │   │   │   │   ├── TagService.java
│   │   │   │   │   ├── CommentService.java
│   │   │   │   │   └── ShareService.java
│   │   │   │   └── KnowledgeBaseApplication.java
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   └── pom.xml
└── websocket/                 # Node.js 实时服务
    ├── server.js              # WebSocket 服务器
    ├── package.json
    └── .env
```

## 快速开始

### 环境要求
- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Maven 3.8+

### 步骤 1: 创建数据库

```sql
CREATE DATABASE knowledge_base CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 步骤 2: 配置数据库

修改 `server/src/main/resources/application.yml` 中的数据库连接信息：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/knowledge_base?useSSL=false&serverTimezone=UTC
    username: your_username
    password: your_password
```

### 步骤 3: 启动 Java 后端

```bash
cd server
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动。

### 步骤 4: 启动 Node.js 实时服务

```bash
cd websocket
npm install
npm run dev
```

WebSocket 服务将在 `http://localhost:3001` 启动。

### 步骤 5: 启动 Vue 前端

```bash
cd client
npm install
npm run dev
```

前端将在 `http://localhost:3000` 启动。

### 访问应用

打开浏览器访问 `http://localhost:3000`

## API 文档

### 认证接口
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `GET /api/auth/me` - 获取当前用户

### 文档接口
- `GET /api/documents/space/{spaceId}` - 获取空间文档列表
- `GET /api/documents/{id}` - 获取文档详情
- `POST /api/documents` - 创建文档
- `PUT /api/documents/{id}` - 更新文档
- `POST /api/documents/{id}/auto-save` - 自动保存
- `POST /api/documents/{id}/restore-draft` - 恢复草稿
- `POST /api/documents/{id}/trash` - 移至回收站
- `POST /api/documents/{id}/restore` - 从回收站恢复
- `DELETE /api/documents/{id}` - 永久删除
- `POST /api/documents/{id}/rollback` - 回滚版本
- `GET /api/documents/search` - 搜索文档

### 版本接口
- `GET /api/documents/{id}/versions` - 获取版本历史
- `GET /api/documents/{id}/versions/{version}` - 获取指定版本

### 空间接口
- `GET /api/spaces` - 获取用户空间列表
- `GET /api/spaces/{id}` - 获取空间详情
- `POST /api/spaces` - 创建空间
- `PUT /api/spaces/{id}` - 更新空间
- `DELETE /api/spaces/{id}` - 删除空间

### 成员管理
- `GET /api/spaces/{id}/members` - 获取空间成员
- `POST /api/spaces/{id}/members` - 添加成员
- `PUT /api/spaces/{id}/members/{userId}` - 更新成员角色
- `DELETE /api/spaces/{id}/members/{userId}` - 移除成员

### 标签接口
- `GET /api/tags/space/{spaceId}` - 获取空间标签
- `POST /api/tags` - 创建标签
- `PUT /api/tags/{id}` - 更新标签
- `DELETE /api/tags/{id}` - 删除标签

### 评论接口
- `GET /api/comments/document/{documentId}` - 获取文档评论
- `POST /api/comments` - 创建评论
- `PUT /api/comments/{id}` - 更新评论
- `DELETE /api/comments/{id}` - 删除评论

### 分享接口
- `GET /api/shares/document/{documentId}` - 获取文档分享列表
- `POST /api/shares` - 创建分享
- `PUT /api/shares/{id}/toggle` - 启用/禁用分享
- `DELETE /api/shares/{id}` - 删除分享
- `POST /api/shares/access` - 访问分享文档

## WebSocket 事件

### 客户端发送事件
- `join-document` - 加入文档协作
- `leave-document` - 离开文档协作
- `edit-content` - 编辑内容
- `cursor-move` - 光标移动
- `auto-save` - 自动保存
- `new-comment` - 新评论
- `update-comment` - 更新评论
- `delete-comment` - 删除评论
- `document-saved` - 文档已保存
- `document-renamed` - 文档已重命名
- `send-message` - 发送消息
- `typing-start` - 开始输入
- `typing-stop` - 停止输入

### 服务端发送事件
- `user-joined` - 用户加入
- `user-left` - 用户离开
- `active-editors` - 活跃编辑者列表
- `content-edited` - 内容已编辑
- `cursor-moved` - 光标已移动
- `auto-save-notification` - 自动保存通知
- `comment-added` - 评论已添加
- `comment-updated` - 评论已更新
- `comment-deleted` - 评论已删除
- `document-saved` - 文档已保存
- `document-renamed` - 文档已重命名
- `notification` - 通知消息
- `direct-message` - 私信
- `user-typing` - 用户输入状态

## 角色权限说明

| 角色 | 权限 |
|------|------|
| **OWNER (所有者)** | 完全控制，可删除空间，管理所有成员 |
| **ADMIN (管理员)** | 管理成员、文档、标签，编辑文档 |
| **EDITOR (编辑者)** | 创建、编辑、删除文档，添加评论 |
| **VIEWER (查看者)** | 仅查看文档和评论，无编辑权限 |

## 核心特性详解

### 文档草稿与自动保存
- 文档编辑时每 30 秒自动保存草稿
- 草稿独立于正式版本
- 下次打开时可选择恢复草稿
- 显示草稿保存时间提示

### 版本控制
- 每次保存自动创建新版本
- 记录变更说明
- 版本号递增
- 支持一键回滚到任意历史版本
- 回滚会创建新版本而非覆盖

### 团队协作
- 实时显示当前编辑人员
- 协作者头像显示
- 在线状态实时更新
- 新评论和保存实时通知

### 文档分享
- 生成唯一分享码
- 可选密码保护
- 可设置过期时间
- 支持仅查看/可编辑两种权限
- 可随时启用/禁用分享链接

## 开发说明

### 前端开发

```bash
cd client
npm run dev    # 开发模式
npm run build  # 生产构建
npm run preview # 预览生产构建
```

### 后端开发

```bash
cd server
mvn spring-boot:run          # 运行
mvn clean package             # 打包
mvn test                      # 运行测试
```

### WebSocket 服务

```bash
cd websocket
npm run dev    # 开发模式 (nodemon)
npm start      # 生产模式
```

## 部署建议

### 生产环境配置

1. **数据库**: MySQL 主从复制
2. **缓存**: Redis 用于会话和缓存
3. **文件存储**: MinIO 或云存储
4. **负载均衡**: Nginx 反向代理
5. **容器化**: Docker + Docker Compose

### 环境变量

**Java 后端** (application.yml):
```yaml
jwt:
  secret: your-secure-jwt-secret-key
```

**Node.js 服务** (.env):
```
PORT=3001
JWT_SECRET=your-secure-jwt-secret-key
JAVA_API_URL=http://java-backend:8080
```

## License

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

---

**知识文库** - 让团队协作更简单高效

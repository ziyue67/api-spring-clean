# Youth Night School API (Spring Boot)

青春夜校后端 API - Spring Boot 3.4 版本

## 技术栈

| 组件 | 版本 |
|------|------|
| Java | 21 |
| Spring Boot | 3.4.3 |
| Spring Data JPA | Hibernate 6.6 |
| Spring Security | JWT (JJWT 0.12.6) |
| 数据库 | PostgreSQL |
| 缓存 | Redis |
| 限流 | Bucket4j 8.7.0 |

## 项目结构

```
api-spring-clean/
├── pom.xml
├── Dockerfile
├── README.md
├── src/main/java/com/youthnightschool/
│   ├── ApiSpringApplication.java
│   ├── config/          # AppProperties, Security, Redis, WebMvc
│   ├── entity/          # JPA 实体 (User, Course, Article, etc.)
│   ├── repository/      # Spring Data JPA repositories
│   ├── dto/             # 请求/响应 DTO
│   ├── controller/       # REST 控制器 (8个)
│   ├── service/         # 业务逻辑 (11个)
│   ├── security/        # JWT 认证
│   ├── interceptor/     # 限流、日志、性能拦截器
│   └── exception/       # 全局异常处理
├── src/main/resources/
│   └── application.yml   # 配置
└── frontend/
    ├── app/              # 微信小程序 (Taro + React)
    │   ├── src/
    │   ├── package.json
    │   └── .env.*         # API 环境变量
    └── admin/            # 管理后台 (React + Vite)
        ├── src/
        ├── package.json
        └── .env.example
```

## API 端点

### 公共端点

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api` | 服务状态 |
| GET | `/api/v1/health` | 健康检查 |
| GET | `/api/v1/articles` | 文章列表 |
| GET | `/api/v1/articles/recent` | 最近文章 |
| GET | `/api/v1/courses` | 课程列表 |
| GET | `/api/v1/courses/months` | 课程月份 |
| GET | `/api/v1/courses/search` | 搜索课程 |

### 认证端点

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/auth/wechat-login` | 微信登录 |

### 用户端点 (JWT)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/users/me` | 获取当前用户 |
| PATCH | `/api/v1/users/me` | 更新用户信息 |
| PATCH | `/api/v1/users/me/phone` | 更新手机号 |

### 签到端点 (JWT)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/sign/status` | 签到状态 |
| POST | `/api/v1/sign` | 签到 |

### 课程端点 (JWT)

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/courses/signup-list` | 我的报名 |
| POST | `/api/v1/courses/signup` | 报名课程 |
| POST | `/api/v1/courses/cancel-signup` | 取消报名 |

### 管理端点 (JWT + Admin)

| 方法 | 路径 | 描述 |
|------|------|------|
| POST | `/api/v1/articles/sync` | 同步文章 |
| POST | `/api/v1/courses/sync` | 同步课程 |
| GET | `/api/v1/admin/stats/overview` | 统计概览 |
| GET | `/api/v1/admin/stats/courses` | 课程统计 |
| GET | `/api/v1/admin/stats/sign-trends` | 签到趋势 |
| GET | `/api/v1/admin/stats/colleges` | 学院统计 |
| GET | `/api/v1/admin/users` | 用户列表 |
| GET | `/api/v1/admin/courses/{id}/signups` | 课程报名列表 |

## 环境变量

```bash
# Server
PORT=3000

# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/youth_night_school
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password

# Redis
REDIS_URL=redis://localhost:6379
REDIS_TLS=false

# JWT (256-bit secret)
JWT_SECRET=your-256-bit-secret-key-here

# WeChat Mini Program
WECHAT_APP_ID=your_wechat_app_id
WECHAT_APP_SECRET=your_wechat_app_secret

# Session Encryption (32+ bytes)
SESSION_KEY_ENCRYPTION_KEY=your-32-byte-encryption-key

# Admin
ADMIN_OPENIDS=openid1,openid2

# CORS
ALLOWED_ORIGINS=http://localhost:5173,http://localhost:10086
```

## 本地开发

### 前置条件

- JDK 21+
- Maven 3.9+
- PostgreSQL 16+
- Redis 7+

### 编译

```bash
mvn clean compile
```

### 运行

```bash
mvn spring-boot:run
```

或使用指定配置：

```bash
JWT_SECRET=your-secret \
DATABASE_URL=jdbc:postgresql://localhost:5432/youth_night_school \
DATABASE_USERNAME=postgres \
DATABASE_PASSWORD=123456 \
REDIS_URL=redis://localhost:6379 \
WECHAT_APP_ID=your-app-id \
WECHAT_APP_SECRET=your-secret \
mvn spring-boot:run
```

## Docker 部署

```bash
# 构建
mvn clean package -DskipTests

# 运行
docker run -d -p 3000:3000 \
  -e DATABASE_URL=jdbc:postgresql://host:5432/youth_night_school \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=xxx \
  -e REDIS_URL=redis://host:6379 \
  -e JWT_SECRET=xxx \
  -e WECHAT_APP_ID=xxx \
  -e WECHAT_APP_SECRET=xxx \
  -e SESSION_KEY_ENCRYPTION_KEY=xxx \
  -e ADMIN_OPENIDS=xxx \
  api-spring:0.0.1-SNAPSHOT
```

## 数据库

使用 Prisma schema 创建表结构：

```bash
# 在 packages/api 目录
npx prisma db push
```

## 安全特性

- **JWT 认证**: HS256 算法，7天过期
- **速率限制**: Bucket4j + Redis，每端点独立限流
- **角色权限**: user / admin 两种角色
- **Admin 白名单**: 通过 openid 白名单控制管理员权限
- **输入校验**: Jakarta Bean Validation
- **安全头**: Spring Security 默认安全头

## 响应格式

### 成功

```json
{
  "success": true,
  "data": { ... }
}
```

### 错误

```json
{
  "success": false,
  "message": "错误信息",
  "path": "/api/v1/..."
}
```

## 前端开发

### 微信小程序 (app)

```bash
cd frontend/app

# 安装依赖
npm install

# 开发模式
npm run dev

# 微信开发者工具导入 dist 目录
```

### 管理后台 (admin)

```bash
cd frontend/admin

# 安装依赖
npm install

# 开发模式
npm run dev

# 生产构建
npm run build
```

### API 环境变量

**微信小程序** (`frontend/app/.env.*`):

```bash
TARO_APP_API_BASE_URL="http://127.0.0.1:3000/api/v1"
TARO_APP_ENV="development"
```

**管理后台** (`frontend/admin/.env.example`):

```bash
VITE_API_BASE_URL=/api/v1
```

## License

MIT

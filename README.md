# 企业访客预约小程序

> 基于 Spring Boot 3 + 微信小程序原生开发的企业访客预约管理系统，集成 AI 接待话术智能生成能力。

企业访客预约系统是一种利用微信小程序平台开发的访客管理解决方案，旨在帮助企业高效地管理访客预约、接待和信息记录，提升企业的服务水平和效率。通过微信小程序，访客可以方便快捷地进行预约操作，企业可以实时获取访客信息并做好接待准备，通过智能化的预约管理和安全控制，为企业提供便捷、安全、高效的访客管理方式。

---

## 目录

- [功能特性](#功能特性)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [项目结构](#项目结构)
- [角色说明](#角色说明)
- [快速开始](#快速开始)
- [数据库设计](#数据库设计)
- [接口文档](#接口文档)
- [AI 功能说明](ai-功能说明)
- [默认账号](#默认账号)
- [部署方案](#部署方案)

---

## 功能特性

系统共有四个角色：管理员、访问人（访客）、被访问人（被访人）、门岗，各角色功能模块如下：

### 管理员模块

- **系统设置**：公司名称、Logo、联系电话、地址、工作时间、访客须知等配置
- **角色权限**：基于角色（super/admin/host/visitor/guard）的权限标识管理
- **管理员管理**：管理员的增删改查
- **员工管理**：员工的条件查询、增删改查、批量操作（删除/启用/停用）
- **部门管理**：树形部门结构的增删改查
- **节假日设置**：法定/公司节假日的维护
- **访客记录**：含时间区间统计、来访单位统计、员工部门统计
- **访客申请审核**：对所有待审核预约进行审批
- **通知管理**：通知公告的发布、编辑、删除（支持按角色发布）

### 被访人模块

- **被访记录**：查看本人的被访记录，支持时间区间统计与来访单位统计
- **访客申请审核**：审批指向本人的预约申请，通过时自动返回 AI 迎接话术
- **辅助预约**：代访客发起预约
- **个人资料**：查看与修改个人资料

### 访客模块

- **预约申请**：填写访客信息、被访人、来访事由、时间等提交预约
- **预约记录**：查看本人预约记录，支持预约二维码、撤销预约、再次预约
- **查看通知**：查看通知公告与进入须知
- **消息中心**：审批结果、欢迎莅临等弹窗通知
- **个人资料**：查看与修改个人资料

### 门岗模块

- **扫码核验**：扫描预约二维码核验访客身份
- **确认放行**：核验通过后确认放行，触发"欢迎莅临"通知

### AI 功能（必选任务）

- **AI 接待话术智能生成**：访客预约审核通过后，后端调用 DeepSeek 大模型 API，根据访客姓名、来访单位和事由自动生成个性化迎接话术建议（欢迎语、座位安排提示、注意事项），通过小程序推送给被访人提前准备。

---

## 技术栈

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.4 | 核心框架 |
| MyBatis-Plus | 3.5.7 | ORM 持久层 |
| MySQL | - | 关系型数据库 |
| JWT (jjwt) | 0.12.5 | 登录鉴权 |
| ZXing | 3.5.3 | 二维码生成 |
| Spring WebFlux | - | 调用大模型 API（WebClient） |
| SpringDoc OpenAPI | 2.5.0 | 接口文档（Swagger UI） |
| Lombok | - | 简化实体类 |
| Java | 17 | 运行环境 |

### 前端

| 技术 | 说明 |
|------|------|
| 微信小程序原生 | WXML / WXSS / JS |
| wx.login() | 微信登录换 JWT |
| wx.scanCode() | 门岗扫码核验 |

### AI

- 调用 DeepSeek 大模型 API（兼容 OpenAI 接口格式）

---

## 系统架构

```
┌──────────────────────────────────────────────────────────┐
│                    微信小程序（前端）                       │
│  登录 / 访客 / 被访人 / 管理员 / 门岗 / 个人中心           │
└────────────────────────┬─────────────────────────────────┘
                         │ HTTP + JWT (Authorization: Bearer)
┌────────────────────────▼─────────────────────────────────┐
│                  Spring Boot 后端 (8080)                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐ │
│  │Controller│→│ Service  │→│ Mapper   │ │ AuthInterceptor│ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────────┘ │
│       │            │                          │            │
│       │       ┌────▼─────┐             ┌──────▼──────┐    │
│       │       │DeepSeek  │             │  JwtUtil    │    │
│       │       │ Client   │             └─────────────┘    │
│       │       └────┬─────┘                                │
│       │            │                                      │
│  ┌────▼────────────▼──────────────────────────────────┐  │
│  │          ZXing 二维码生成 / 统一异常处理 / Result   │  │
│  └────────────────────────────────────────────────────┘  │
└────────────────────────┬─────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
   ┌────────────┐  ┌──────────┐  ┌─────────────┐
   │   MySQL    │  │ DeepSeek │  │ Swagger UI  │
   │visitor_sys │  │   API    │  │/swagger-ui  │
   └────────────┘  └──────────┘  └─────────────┘
```

统一响应格式：

```json
{ "code": 200, "msg": "success", "data": { ... } }
```

统一请求头：`Authorization: Bearer <token>`

---

## 项目结构

```
E3-backendanddb/
├── backend/                        # 后端 Spring Boot 工程
│   ├── pom.xml                     # Maven 依赖配置
│   ├── app.err                     # 错误日志
│   └── src/main/
│       ├── java/com/visitor/
│       │   ├── VisitorApplication.java        # 启动类
│       │   ├── ai/
│       │   │   └── DeepSeekClient.java        # AI 大模型调用客户端
│       │   ├── auth/
│       │   │   ├── AuthInterceptor.java       # 鉴权拦截器
│       │   │   └── JwtUtil.java               # JWT 工具类
│       │   ├── common/
│       │   │   ├── Result.java                # 统一响应
│       │   │   ├── PageResult.java            # 分页响应
│       │   │   └── exception/                 # 全局异常处理
│       │   ├── config/
│       │   │   ├── MyBatisPlusConfig.java     # MyBatis-Plus 配置
│       │   │   ├── SpringDocConfig.java       # Swagger 配置
│       │   │   └── WebMvcConfig.java          # Web MVC 配置（注册拦截器）
│       │   ├── controller/                    # 控制器层（13 个）
│       │   │   ├── LoginController.java       # 登录认证
│       │   │   ├── AppointmentController.java # 预约管理（核心）
│       │   │   ├── GreetingController.java    # AI 话术
│       │   │   ├── GuardController.java       # 门岗核验
│       │   │   ├── UserController.java        # 用户/员工
│       │   │   ├── DepartmentController.java  # 部门
│       │   │   ├── HolidayController.java     # 节假日
│       │   │   ├── NotificationController.java# 通知公告
│       │   │   ├── UserNotificationController.java # 用户消息
│       │   │   ├── QRCodeController.java      # 二维码
│       │   │   ├── RolePermissionController.java  # 角色权限
│       │   │   ├── SystemController.java      # 系统设置/统计
│       │   │   └── ProfileController.java     # 个人资料
│       │   ├── service/                       # 业务服务层
│       │   ├── entity/                        # 实体类（10 个）
│       │   ├── mapper/                        # MyBatis-Plus Mapper（10 个）
│       │   └── dto/                           # 数据传输对象
│       │       ├── LoginReq.java
│       │       ├── LoginResp.java
│       │       └── RegisterReq.java
│       └── resources/
│           ├── application.yml                # 主配置
│           └── application-dev.yml            # 开发环境配置
│
├── demofront/                      # 微信小程序前端
│   ├── 项目接口文档.md              # 接口文档
│   ├── project.config.json
│   └── miniprogram/
│       ├── app.js / app.json / app.wxss
│       ├── components/             # 公共组件
│       ├── images/                 # 图片资源
│       ├── utils/
│       │   ├── request.js          # 网络请求封装
│       │   ├── api.js              # API 接口定义
│       │   └── data-service.js     # 数据服务
│       └── pages/
│           ├── login/              # 登录页
│           ├── register/           # 注册页
│           ├── visitor/            # 访客页面
│           │   ├── appoint/        # 预约申请
│           │   ├── records/        # 预约记录
│           │   ├── notices/        # 通知公告
│           │   └── notification-center/  # 消息中心
│           ├── host/               # 被访人页面
│           │   ├── visited/        # 被访记录
│           │   ├── approve/        # 访客审批
│           │   └── helper/         # 辅助预约
│           ├── admin/              # 管理员页面
│           │   ├── employees/      # 员工管理
│           │   ├── departments/    # 部门管理
│           │   ├── admins/         # 管理员管理
│           │   ├── roles/          # 角色权限
│           │   ├── approve/        # 访客审核
│           │   ├── stats/          # 数据统计
│           │   ├── system/         # 系统设置
│           │   ├── holiday/        # 节假日设置
│           │   └── notify-mgr/     # 通知管理
│           ├── guard/              # 门岗页面
│           │   └── scan/           # 扫码核验
│           └── personal/           # 个人中心
│               └── profile/        # 个人资料
│
├── forSql/                         # 数据库脚本
│   ├── init.sql                    # 建库建表脚本
│   ├── insert.sql                  # 初始化数据
│   └── alter_notification_null.sql # 表结构修补脚本
│
└── task.md                         # 项目任务说明
```

---

## 角色说明

| 角色 | role 值 | 主要职责 |
|------|---------|---------|
| 管理员 | `admin` | 人员管理、预约审批、通知发布、数据统计、系统配置 |
| 被访人 | `host` | 审批指向本人的预约、查看被访记录、辅助预约 |
| 访客 | `visitor` | 预约申请、撤销/再次预约、查看通知与个人资料 |
| 门岗 | `guard` | 扫码核验、确认放行 |

预约状态流转：`pending`（待审核）→ `approved`（已通过）→ `confirmed`（已核验）；或 `pending` → `rejected`（已拒绝）/ `cancelled`（已取消）。

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- 微信开发者工具

### 1. 初始化数据库

```bash
# 登录 MySQL 后依次执行
mysql -u root -p < forSql/init.sql
mysql -u root -p < forSql/insert.sql
mysql -u root -p < forSql/alter_notification_null.sql
```

### 2. 配置后端

编辑 `backend/src/main/resources/application-dev.yml`，配置数据库连接：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/visitor_system?...
    username: root
    password: 你的密码
```

`application.yml` 中已配置 DeepSeek AI 调用参数（`deepseek.api-key`、`api-url`、`model`），如无需 AI 功能可留空，系统将自动回退到模板话术。

### 3. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动后访问：

- 服务地址：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- API 文档：`http://localhost:8080/api-docs`

### 4. 启动前端

1. 打开微信开发者工具，导入 `demofront` 目录
2. 在「详情 → 本地设置」勾选「不校验合法域名...」
3. 确认 `utils/request.js` 中的 `baseUrl` 指向 `http://localhost:8080/api`
4. 编译运行

---

## 数据库设计

数据库名：`visitor_system`，共 10 张表：

| 表名 | 说明 |
|------|------|
| `sys_department` | 部门表（树形结构，parent_id 关联） |
| `sys_user` | 用户表（管理员/被访人/访客/门岗共用，role 区分） |
| `app_appointment` | 预约表（核心业务表） |
| `app_greeting` | AI 迎接话术记录表 |
| `app_visit_record` | 门岗核验记录表 |
| `not_notification` | 通知公告表 |
| `not_holiday` | 节假日设置表 |
| `sys_role_permission` | 角色权限配置表 |
| `sys_setting` | 系统配置表（键值对） |
| `sys_user_notification` | 用户通知表（弹窗提醒，支持 approved/rejected/greeting/announcement 类型） |

---

## 接口文档

完整接口文档见 [`demofront/项目接口文档.md`](demofront/项目接口文档.md)，Base URL：`http://localhost:8080/api`。

主要模块接口概览：

| 模块 | 路径前缀 | 主要功能 |
|------|---------|---------|
| 认证 | `/auth` | 微信登录、账号密码登录（开发用） |
| 访客预约 | `/appointment` | 提交/撤销/再次预约、我的预约、AI 话术 |
| 被访人 | `/appointment/host` | 被访记录、统计、待审批、审批、辅助预约 |
| 管理员 | `/admin` | 员工/部门/管理员/角色/节假日/审核/统计/通知 |
| 门岗 | `/guard` | 扫码核验、确认放行 |
| 用户 | `/user` | 个人资料 |
| 通知 | `/notification` | 通知公告列表 |
| 用户消息 | `/user-notification` | 弹窗消息、已读标记 |
| AI | `/ai` | 生成迎接话术 |
| 二维码 | `/qrcode` | 预约二维码生成 |

---

## AI 功能说明

### AI 接待话术智能生成（必选任务）

- **触发时机**：预约审核通过后自动触发，也可通过 `POST /ai/greeting/{appointmentId}` 手动调用。
- **实现位置**：`backend/src/main/java/com/visitor/ai/DeepSeekClient.java`
- **工作流程**：
  1. 根据访客姓名、来访单位、访问事由、被访人构造 Prompt
  2. 通过 WebClient 调用 DeepSeek 大模型 API（兼容 OpenAI 接口）
  3. 解析返回的 JSON，提取 `greeting`（欢迎语）与 `notes`（注意事项）
  4. 话术存入 `app_greeting` 表，并通过 `sys_user_notification` 推送给被访人
- **容错机制**：API Key 未配置或调用失败时，自动回退到模板话术（`source: "mock"`），保证业务不中断。
- **返回字段**：`greeting`（迎接语）、`seatSuggestion`（座位建议）、`notes`（注意事项）、`source`（ai/mock）。

---

## 默认账号

初始化数据预置以下账号（账号/密码）：

| 账号 | 密码 | 角色 | 姓名 |
|------|------|------|------|
| `admin` | `admin123` | 管理员 | 系统管理员 |
| `admin2` | `123456` | 管理员 | 副管理员 |
| `zhangsan` | `123456` | 被访人 | 张三 |
| `lisi` | `123456` | 被访人 | 李四 |
| `wangwu` | `123456` | 被访人 | 王五 |
| `zhaoliu` | `123456` | 被访人 | 赵六 |
| `visitor1` | `123456` | 访客 | 刘访客 |
| `visitor2` | `123456` | 访客 | 陈访客 |
| `visitor3` | `123456` | 访客 | 吴访客 |
| `guard` | `123456` | 门岗 | 王门岗 |

> 开发模式下可通过 `POST /auth/login/bypass` 使用账号密码直接登录获取 token，无需微信授权。

---

## 部署方案

**开发阶段**：微信开发者工具勾选「不校验合法域名」，后端直接 `http://localhost:8080`。

**演示阶段**：使用 natapp 或 ngrok 内网穿透，将本地 Spring Boot 映射为 HTTPS 地址；或部署到 Railway / Zeabur 等免费云托管平台。

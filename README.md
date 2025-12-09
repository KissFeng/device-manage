# 光影资产 - 媒体设备借用管理系统

## 项目说明

本项目是 **Java 实训项目**，是一个基于 Spring Boot + 前端原生技术的媒体设备借用管理系统。系统主要用于管理媒体设备的分类、设备信息、用户借用申请和审批流程，实现设备借用的全生命周期管理。

## 技术栈

### 后端
- **框架**: Spring Boot 3.5.8
- **数据库**: MySQL 8.0+
- **持久层**: MyBatis Plus 3.5.9
- **安全认证**: Spring Security + JWT (0.12.3)
- **Java版本**: JDK 17
- **构建工具**: Maven

### 前端
- **技术**: 原生 HTML + CSS + JavaScript
- **样式**: 自定义 CSS（支持毛玻璃效果、响应式布局）
- **API通信**: Fetch API + Promise/Async-Await

## 项目用途

本系统适用于学校、企业、工作室等需要管理媒体设备（如相机、镜头、灯光设备等）借用的场景，提供：

- **设备管理**: 设备的增删改查、分类管理、状态跟踪
- **借用管理**: 用户申请借用、管理员审批、归还确认
- **用户管理**: 用户信息管理、角色权限控制
- **数据统计**: 设备总数、用户总数、待审批申请等数据展示

## 核心功能

### 管理员功能
- ✅ **仪表盘**: 查看系统概览数据（设备总数、用户总数、待审批申请）
- ✅ **设备浏览**: 浏览所有设备，支持申请借用
- ✅ **设备管理**: 设备的增删改查、状态管理
- ✅ **分类管理**: 设备分类的增删改查
- ✅ **用户管理**: 用户的增删改查、角色管理
- ✅ **借用审批**: 审批借用申请、确认设备归还

### 普通用户功能
- ✅ **设备浏览**: 浏览可用设备，查看设备详情
- ✅ **申请借用**: 提交设备借用申请（自动分配可用实例）
- ✅ **我的借用**: 查看个人借用记录及状态

### 系统特性
- ✅ **JWT 认证**: 基于 Token 的身份认证机制
- ✅ **角色权限**: 管理员和普通用户权限分离
- ✅ **设备实例**: 支持设备实例管理，自动分配可用实例
- ✅ **状态管理**: 设备状态自动计算（正常、借用中、维修中、已报废）
- ✅ **级联删除**: 删除设备/用户时自动删除相关借用记录
- ✅ **响应式设计**: 支持不同屏幕尺寸的适配

## 启动方式

### 环境要求
- JDK 17 或更高版本
- Maven 3.6+
- MySQL 8.0+
- 现代浏览器（Chrome、Firefox、Edge 等）

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE IF NOT EXISTS `device_manage` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 执行建表语句（参考项目中的数据库脚本）

3. 修改后端配置文件 `DeviceManage-backend/src/main/resources/application.properties`：
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/device_manage?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=your_password
```

### 后端启动

```bash
# 进入后端目录
cd DeviceManage-backend

# 使用 Maven 启动
mvn spring-boot:run

# 或使用 IDE 直接运行 DeviceManageBackendApplication.java
```

后端服务默认运行在 `http://localhost:8080`

### 前端启动

前端为静态页面，可以使用以下方式启动：

**方式一：使用 Python 简单 HTTP 服务器**
```bash
# 进入前端目录
cd DeviceManage-frontend

# Python 3
python -m http.server 8000

# Python 2
python -m SimpleHTTPServer 8000
```

**方式二：使用 Node.js http-server**
```bash
# 安装 http-server（如果未安装）
npm install -g http-server

# 进入前端目录并启动
cd DeviceManage-frontend
http-server -p 8000
```

**方式三：使用 VS Code Live Server 插件**
- 安装 Live Server 插件
- 右键点击 `index.html`，选择 "Open with Live Server"

前端页面默认运行在 `http://localhost:8000`

## 服务器部署

### 部署前准备

1. **打包后端项目**：
```bash
cd DeviceManage-backend
mvn clean package
```

打包完成后，在 `target/` 目录下会生成 `DeviceManage-backend-0.0.1-SNAPSHOT.jar` 文件。

2. **上传文件到服务器**：
   - 上传 JAR 包到服务器
   - 上传前端文件到服务器

3. **配置数据库**：
   - 确保服务器上的 MySQL 已启动
   - 修改 `application.properties` 中的数据库连接信息（如果使用外部配置文件）

### 启动服务

#### 启动后端服务

```bash
# 使用 nohup 在后台启动后端服务，日志输出到 backend.log
nohup /www/server/java/jdk-17.0.8/bin/java -jar DeviceManage-backend-0.0.1-SNAPSHOT.jar > backend.log 2>&1 &
```

**说明**：
- 使用 `nohup` 确保进程在终端关闭后继续运行
- `> backend.log 2>&1` 将标准输出和错误输出都重定向到日志文件
- `&` 表示在后台运行

#### 启动前端服务

```bash
# 进入前端目录
cd DeviceManage-frontend

# 使用 Python 启动 HTTP 服务器（Python 2）
nohup python -m SimpleHTTPServer 9012 > ../frontend.log 2>&1 &

# 或使用 Python 3
# nohup python3 -m http.server 9012 > ../frontend.log 2>&1 &
```

### 服务管理

#### 查看端口占用情况

```bash
# 查看 9011 端口（后端）占用情况
netstat -tulpn | grep 9011

# 查看 9012 端口（前端）占用情况
netstat -tulpn | grep 9012
```

#### 停止服务

**停止后端服务（9011端口）**：
```bash
# 第一步：获取 9011 端口的 PID
PID_9011=$(netstat -tulpn | grep 9011 | awk '{print $7}' | cut -d '/' -f1)

# 第二步：优雅终止进程（推荐先使用 -15）
kill -15 $PID_9011

# 若进程未终止，使用强制终止
# kill -9 $PID_9011
```

**停止前端服务（9012端口）**：
```bash
# 第一步：获取 9012 端口的 PID
PID_9012=$(netstat -tulpn | grep 9012 | awk '{print $7}' | cut -d '/' -f1)

# 第二步：优雅终止进程
kill -15 $PID_9012

# 强制终止（如果优雅终止失败）
# kill -9 $PID_9012
```

#### 查看服务日志

```bash
# 查看后端日志
tail -f backend.log

# 查看前端日志
tail -f frontend.log

# 查看最近 100 行日志
tail -n 100 backend.log
```

### 部署注意事项

1. **防火墙配置**：
   - 确保服务器防火墙开放 9011（后端）和 9012（前端）端口
   - 如果使用云服务器，需要在安全组中配置端口规则

2. **CORS 配置**：
   - 后端已配置允许所有来源的跨域请求，无需额外配置
   - 如需限制来源，可修改 `SecurityConfig.java` 中的 CORS 配置

3. **服务器地址配置**：
   - 后端默认监听 `0.0.0.0:9011`，允许外部访问
   - 前端 API 地址会自动根据访问域名选择（开发环境使用 localhost，生产环境使用服务器 IP）

4. **进程管理**：
   - 建议使用 `systemd` 或 `supervisor` 等进程管理工具管理服务
   - 或使用 `screen` 或 `tmux` 保持会话

## 项目结构

```
project/
├── DeviceManage-backend/          # 后端项目
│   ├── src/main/java/            # Java 源代码
│   │   ├── controller/          # 控制器层
│   │   ├── service/             # 业务逻辑层
│   │   ├── mapper/              # 数据访问层
│   │   ├── entity/              # 实体类
│   │   ├── dto/                 # 数据传输对象
│   │   ├── config/              # 配置类
│   │   └── util/                # 工具类
│   └── src/main/resources/      # 资源文件
│       ├── application.properties
│       └── mapper/              # MyBatis XML 映射文件
│
└── DeviceManage-frontend/        # 前端项目
    ├── admin/                    # 管理员页面
    ├── user/                     # 用户页面
    ├── css/                      # 样式文件
    ├── js/                       # JavaScript 文件
    ├── static/                   # 静态资源（图片等）
    └── index.html                # 登录页面
```

## 贡献者

- 赵杰
- 徐爱姣
- 左鑫月
- 孟佳诺
- 徐冰楠

## 注意事项

- 确保 MySQL 服务已启动
- 确保后端服务（8080端口）已启动后再访问前端
- 首次使用需要初始化数据库表结构和测试数据
- 生产环境请修改 JWT 密钥和数据库密码

## 接口文档

详细的 API 接口文档请参考 `docs/接口文档.md`

---

**项目版本**: v1.0  
**最后更新**: 2025年12月


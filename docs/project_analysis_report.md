# 项目分析与改进建议报告

## 1. 项目概览
本项目是一个标准的 B/S 架构全栈应用，采用 **Spring Boot 3.5.8** (后端) 和 **原生 HTML/JS** (前端)。
- **优点**：
  - 目录结构清晰，分层合理（Controller, Service, Mapper, Entity）。
  - 使用了 MyBatis Plus 简化数据库操作。
  - 实现了统一的响应结果封装 (`Result<T>`) 和全局异常处理 (`GlobalExceptionHandler`)。
  - 前端 API 封装 (`api.js`) 结构良好，包含了拦截器逻辑。

## 2. 核心改进建议 (Critical Improvements)

### ⚠️ 后端性能与逻辑优化 (DeviceController.java)
在 `DeviceController.java` 中的 `getDeviceList` 和 `countNormalDevices` 方法存在严重的性能隐患。
- **问题**：目前通过**内存过滤**的方式来实现“仅显示正常状态设备”的功能。代码会循环查询每一页数据，然后在内存中过滤，直到凑够一页数据。如果数据库中大量设备状态不正常，这会导致频繁的数据库 IO 查询，甚至查询整张表。
- **建议**：**将过滤逻辑下沉到数据库层**。
  - 在 MyBatis Plus 的 `QueryWrapper` 中直接添加过滤条件 `.eq("overall_status", "NORMAL")`（注意：如果 `overallStatus` 是计算属性不存库，则需优化数据库设计或在 SQL 层关联查询）。
  - 如果 `overallStatus` 是动态计算的（例如基于子表状态），建议使用 SQL 关联查询 (Join) 或视图，而不是在 Java 层循环查询。

### 🛡️ 安全性增强
- **JWT 密钥硬编码**：`application.properties` 中 `jwt.secret` 是硬编码的。
  - **建议**：在生产环境中，应从环境变量或更加安全的配置中心读取密钥，防止源码泄露导致的安全风险。
- **数据库密码**：同上，数据库密码也不应直接明文写在配置文件中。

### 🏗️ 前端架构优化
- **硬编码 URL**：`api.js` 中虽然做了 localhost 判断，但 IP `43.138.28.205` 是硬编码的。
  - **建议**：提取到单独的配置文件 `config.js` 中，或者使用相对路径 `/api` 并配合 Nginx 反向代理，这样前端代码部署时不需要修改代码。

## 3. 代码规范与维护
- **魔法值**：代码中存在一些字符串字面量（如 `"NORMAL"`）。建议统一使用常量类或枚举（`DeviceStatus` 枚举已存在，应在 Service/Controller 中严格使用枚举的 `.name()`）。
- **注释**：核心业务逻辑注释较为清晰，继续保持。

## 总结
项目整体基础良好，尤其适合作为实训或教学项目。最优先需要解决的是 **`DeviceController` 的分页查询逻辑**，这直接影响系统在数据量增大后的性能。

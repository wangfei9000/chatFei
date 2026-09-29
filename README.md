# ChatFei

匿名 Android 聊天应用，支持私聊、群聊、历史消息、图片和视频消息。

## 目录

- `backend/`：Java 21 + Spring Boot + PostgreSQL
- `android/`：Kotlin + Jetpack Compose Android 客户端

## 本地启动

1. 启动数据库：`docker compose up -d db`
2. 启动后端：`cd backend && mvn spring-boot:run`
3. 用 Android Studio 打开 `android/`
4. 默认连接后端 `http://121.40.244.102:8082`，也可在应用“我的 → 服务器地址”中修改

身份是匿名设备身份。卸载 App 或清除数据后，无法恢复原身份。

## 已实现的第一阶段

- 自动创建并在设备保存匿名身份
- 在线用户列表与昵称实时广播
- 私聊、创建群聊、会话历史和实时消息
- 聊天中可继续接收其他会话消息
- 图片/视频 multipart 上传，二进制使用 PostgreSQL `BYTEA` 保存
- 媒体访问会校验发送者或会话成员身份
- Android `remoteMessaging` 前台服务维持在线连接

## 当前约束

- Android 默认后端地址为 `http://121.40.244.102:8082`；可在应用“我的 → 服务器地址”中修改并持久保存。
- 媒体当前限制为图片 10MB、视频 50MB。
- 开发期使用 Hibernate 自动建表，正式部署前应改为 Flyway 迁移脚本。
- App 清除数据或卸载后无法找回匿名身份。

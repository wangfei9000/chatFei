# ChatFei

匿名 Android 聊天应用，支持私聊、群聊、历史消息、图片和视频消息。

## 目录

- `backend/`：Java 21 + Spring Boot + PostgreSQL
- `android/`：Kotlin + Jetpack Compose Android 客户端

## 文件说明

### 根目录

- `README.md`：项目介绍、运行方法、加密配置和开发约束。
- `compose.yaml`：使用 Docker Compose 启动本地 PostgreSQL 16 数据库，并保存数据库卷。
- `tu.jpg`：项目中的 512×512 JPEG 图片素材，目前没有被 Android 或后端源码直接引用。

### Android 客户端

- `android/settings.gradle.kts`：声明项目名称、模块以及 Gradle 插件和依赖仓库。
- `android/build.gradle.kts`：配置 Android Gradle Plugin 和 Kotlin Compose 插件版本。
- `android/app/build.gradle.kts`：配置 App 的包名、Android SDK、版本、Compose 和 OkHttp 等依赖。
- `android/gradle.properties`：配置 Gradle 内存、AndroidX 和 Kotlin 代码风格。
- `android/local.properties`：本机 Android SDK 路径，仅供当前开发环境使用，不应作为跨机器配置。
- `android/gradlew`、`android/gradlew.bat`：Linux/macOS 和 Windows 使用的 Gradle Wrapper 启动脚本。
- `android/gradle/wrapper/gradle-wrapper.properties`：指定项目使用的 Gradle 版本和下载地址。
- `android/gradle/wrapper/gradle-wrapper.jar`：Gradle Wrapper 的启动程序。
- `android/gradle/gradle-daemon-jvm.properties`：指定 Gradle Daemon 使用的 JVM 条件。
- `android/app/src/main/AndroidManifest.xml`：声明应用入口、网络和通知权限、前台在线服务以及允许 HTTP 明文流量。
- `android/app/src/main/java/com/chatfei/app/MainActivity.kt`：Compose UI 入口，包含聊天列表、在线用户、个人设置、聊天页面、图片选择器和消息输入栏。
- `android/app/src/main/java/com/chatfei/app/ChatViewModel.kt`：管理页面状态和业务流程，包括初始化会话、刷新列表、收发消息、上传媒体及修改配置。
- `android/app/src/main/java/com/chatfei/app/ApiClient.kt`：封装后端 HTTP API，包括身份、会话、消息、昵称和媒体上传请求。
- `android/app/src/main/java/com/chatfei/app/RealtimeClient.kt`：建立和维护 WebSocket 连接，接收实时事件并通知页面刷新。
- `android/app/src/main/java/com/chatfei/app/OnlineService.kt`：Android 前台服务，维持用户在线状态并定时尝试恢复 WebSocket。
- `android/app/src/main/java/com/chatfei/app/AesTextCrypto.kt`：使用 AES-256-GCM 加密和解密文本，并校验 Base64 密钥。
- `android/app/src/main/java/com/chatfei/app/SessionStore.kt`：通过 SharedPreferences 保存匿名身份、服务器地址和 AES 密钥。
- `android/app/src/main/java/com/chatfei/app/Models.kt`：定义客户端使用的会话、用户、聊天和消息数据模型。
- `android/app/src/main/res/values/styles.xml`：定义应用基础主题、字体和状态栏样式。
- `android/app/src/main/res/mipmap-*/ic_launcher.png`：适配不同屏幕密度的应用启动图标。

### Spring Boot 后端

- `backend/pom.xml`：Maven 项目配置，声明 Java 21、Spring Boot、WebSocket、JPA 和 PostgreSQL 等依赖。
- `backend/src/main/resources/application.yml`：配置数据库、上传大小、服务端口、媒体限制和 AES 密钥。
- `backend/src/main/java/com/chatfei/ChatFeiApplication.java`：Spring Boot 程序启动入口。

API 层：

- `api/SessionController.java`：创建匿名身份、查询当前用户、修改昵称和获取在线用户。
- `api/ConversationController.java`：创建私聊或群聊、查询会话、发送消息和读取历史消息。
- `api/MediaController.java`：上传、下载图片和视频，并校验媒体访问权限和大小。
- `api/ApiException.java`：携带 HTTP 状态码的业务异常。
- `api/ApiExceptionHandler.java`：将业务异常转换成统一的 JSON 错误响应。

业务层：

- `service/AuthService.java`：生成匿名用户及令牌，对令牌做 SHA-256 哈希，并完成请求身份验证。
- `service/ChatService.java`：处理私聊、群聊、成员权限、消息保存、历史消息和媒体消息校验。
- `service/PresenceService.java`：在内存中维护用户和 WebSocket 会话的在线关系。
- `service/AesTextCrypto.java`：使用服务端配置的 AES-256-GCM 密钥解密客户端密文，以便保存明文。

实时通信层：

- `realtime/WebSocketConfig.java`：把 WebSocket 处理器注册到 `/ws` 地址。
- `realtime/ChatWebSocketHandler.java`：验证 WebSocket token，处理上线、离线和 PING/PONG。
- `realtime/RealtimeGateway.java`：向单个用户、会话成员或全部在线用户推送 JSON 实时事件。

数据库实体：

- `domain/ChatUser.java`：用户表实体，保存昵称、token 哈希和活跃时间。
- `domain/Conversation.java`：会话表实体，保存私聊/群聊类型、群名、所有者和最后消息。
- `domain/ConversationMember.java`：会话成员实体，保存成员角色、加入/退出和已读位置。
- `domain/ChatMessage.java`：消息实体，保存消息类型、旧版明文、AES 密文、新明文和媒体 ID。
- `domain/MediaFile.java`：媒体实体，将图片、视频和缩略图以 PostgreSQL `BYTEA` 保存。

数据访问层：

- `repo/UserRepository.java`：按 ID 或 token 哈希查询用户。
- `repo/ConversationRepository.java`：会话的基础增删改查。
- `repo/MemberRepository.java`：查询用户会话、会话成员和成员资格。
- `repo/MessageRepository.java`：分页查询历史消息、实现消息幂等和查找媒体所属消息。
- `repo/MediaRepository.java`：媒体文件的基础增删改查。

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
- 文本消息使用 AES-256-GCM 加密传输

## 文本消息加密

ChatFei 当前采用共享密钥方案。Android 客户端在发送前加密文本，服务端收到密文后解密一次并保存明文，同时保留原始密文。服务端向历史消息接口和 WebSocket 转发原始密文，不会为接收方重新加密；接收方使用相同密钥在本地解密。

加密参数：

- 算法：`AES/GCM/NoPadding`
- 密钥长度：256 位（32 字节）
- IV：每条消息随机生成 12 字节
- 认证标签：128 位
- 传输编码：`Base64(IV + Ciphertext + AuthenticationTag)`

默认 Base64 密钥：

```text
Q2hhdEZlaS1BRVMtMjU2LUtleS0yMDI2LTAwMDAwMDE=
```

Android 可在“我的 → 消息 AES 密钥”中修改并持久保存。密钥必须是 Base64 编码的 32 字节数据，所有客户端和服务端必须配置成相同值。

服务端推荐通过环境变量配置：

```bash
export CHATFEI_AES_KEY_BASE64='你的Base64编码32字节密钥'
cd backend
mvn spring-boot:run
```

可以使用 OpenSSL 生成随机密钥：

```bash
openssl rand -base64 32
```

也可以在 `backend/src/main/resources/application.yml` 中修改 `chatfei.crypto.aes-key-base64`。环境变量优先，配置文件包含的默认值只适合开发和联调。

### 消息字段与兼容性

新版客户端发送文本消息时使用 `encryptedText`，不再发送明文 `text`：

```json
{
  "clientMessageId": "UUID",
  "type": "TEXT",
  "encryptedText": "Base64密文"
}
```

数据库中的文本消息字段：

- `encrypted_content`：客户端提交的原始密文，用于直接返回和转发。
- `plain_text_content`：服务端解密得到的明文。
- `text_content`：旧版消息的明文字段，为兼容历史数据而保留。

兼容规则：

- 新版 App 优先读取并解密 `encryptedText`。
- 历史消息没有 `encryptedText` 时，App 继续显示旧 `text`。
- 服务端仍接受旧版 App 提交的明文 `text`。
- AES 密钥不一致或密文被修改时，GCM 认证失败，消息不会入库。
- 客户端无法用当前密钥解密历史密文时，会显示密钥检查提示。

部署时必须先更新服务端，再发布新版 Android App。由于启用了 Hibernate `ddl-auto: update`，启动新版服务端时会自动增加密文字段和明文字段。

> 该实现是共享密钥加密，不是端到端加密。服务端持有 AES 密钥并保存明文，获得服务端权限或 App 配置的人可以读取消息。生产环境还必须使用 HTTPS/WSS，不能用 AES 替代 TLS。

## 当前约束

- Android 默认后端地址为 `http://121.40.244.102:8082`；可在应用“我的 → 服务器地址”中修改并持久保存。
- 媒体当前限制为图片 10MB、视频 50MB。
- 开发期使用 Hibernate 自动建表，正式部署前应改为 Flyway 迁移脚本。
- App 清除数据或卸载后无法找回匿名身份。

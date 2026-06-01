# 第一阶段：构建
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# 复制 Maven 配置（使用阿里云镜像加速）
COPY pom.xml .
COPY .mvn .mvn

# 下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -q -s .mvn/settings.xml

# 复制源码并编译
COPY src src
RUN mvn package -DskipTests -q -s .mvn/settings.xml

# 第二阶段：运行
FROM eclipse-temurin:17-jre

WORKDIR /app

# 复制 JAR
COPY --from=builder /build/target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 启动（可通过环境变量 SPRING_PROFILES_ACTIVE 切换 profile）
ENTRYPOINT ["java", "-Xms256m", "-Xmx1024m", "-jar", "app.jar"]

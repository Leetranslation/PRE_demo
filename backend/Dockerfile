# Token ERP 后端 —— 多阶段构建
#
# 为什么要多阶段：
#   构建需要 Maven + JDK + 一堆依赖（~700MB），运行只需要 JRE（~200MB）。
#   分两个阶段，最终镜像里不含 Maven 仓库和源码。

# ---------- 阶段 1：构建 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 先只拷 pom，单独一层把依赖拉下来。
# 好处：以后改 Java 代码不会重新下载依赖 —— 第二次构建能快好几分钟。
# （代价：改 pom 时这一层会失效重来，这是正常的）
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- 阶段 2：运行 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# ⚠️ jar 名写死为 `<artifactId>-<version>.jar`（见 pom.xml）。
#    改 pom 的 version 时**必须同步改这一行**，否则构建会报「找不到文件」。
COPY --from=build /app/target/token-erp-backend-1.0.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

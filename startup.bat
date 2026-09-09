@echo off
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
cd /d d:\erp\backend
mvn spring-boot:run -DskipTests > d:\erp\startup.log 2>&1

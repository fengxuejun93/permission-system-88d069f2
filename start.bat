@echo off
chcp 65001 >nul 2>&1
echo ========================================
echo   校内网社交原型 - 启动脚本
echo ========================================
echo.

set JAVA_HOME=C:\auto-tool\jdk17\jdk-17.0.19+10
set PATH=%JAVA_HOME%\bin%;C:\auto-tool\apache-maven-3.9.6\bin;%PATH%

echo [1/2] 检查环境...
java -version 2>&1 | findstr "version" >nul
if errorlevel 1 (
    echo 错误: 未找到 Java，请确认 JAVA_HOME 设置正确
    pause
    exit /b 1
)

echo [2/2] 启动应用...
echo 启动成功后，请在浏览器访问: http://localhost:8080
echo 按 Ctrl+C 可停止服务
echo.
cd /d %~dp0
mvn spring-boot:run

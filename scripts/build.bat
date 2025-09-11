@echo off
echo ========================================
echo Building Solicitudes Service
echo ========================================

echo.
echo [1/3] Cleaning previous builds...
call gradlew.bat clean

echo.
echo [2/3] Building JAR file...
call gradlew.bat bootJar

echo.
echo [3/3] Copying JAR to deployment folder...
copy "applications\app-service\build\libs\*.jar" "deployment\"

echo.
echo ========================================
echo Build completed successfully!
echo JAR file ready for Docker build
echo ========================================

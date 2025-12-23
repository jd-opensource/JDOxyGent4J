@echo off
setlocal enabledelayedexpansion

echo "Starting IDEA project default run template configuration..."

REM Check if the current directory is the root of a Maven project
if not exist "pom.xml" (
    echo "Error: Please run this script in the project root directory (containing pom.xml)"
    pause
    exit /b 1
)

echo.
echo "Please configure LLM-related environment variables (press Enter to use placeholder values)"

set /p LLM_API_KEY=OXY_LLM_API_KEY ^(Your LLM API key^):
set /p LLM_BASE_URL=OXY_LLM_BASE_URL ^(Your LLM API base URL^):
set /p LLM_MODEL_NAME=OXY_LLM_MODEL_NAME ^(Model name you want to use^):

echo.
echo "Environment variable configuration"
echo    API_KEY: !LLM_API_KEY!
echo    BASE_URL: !LLM_BASE_URL!
echo    MODEL_NAME: !LLM_MODEL_NAME!
echo.

REM JVM arguments ...
set "JVM_ARGS=--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/sun.util.calendar=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED"

if not exist ".run" mkdir ".run"

echo "create Template Application.run.xml..."

REM Generate XML file (use a temporary block to avoid escaping issues with echo)
set "XML_FILE=.run\Template Application.run.xml"
(
echo ^<component name="ProjectRunConfigurationManager"^>
echo   ^<configuration default="true" type="Application" factoryName="Application"^>
echo     ^<option name="ALTERNATIVE_JRE_PATH" value="17" /^>
echo     ^<option name="ALTERNATIVE_JRE_PATH_ENABLED" value="true" /^>
echo     ^<envs^>
echo       ^<env name="OXY_LLM_API_KEY" value="!LLM_API_KEY!" /^>
echo       ^<env name="OXY_LLM_BASE_URL" value="!LLM_BASE_URL!" /^>
echo       ^<env name="OXY_LLM_MODEL_NAME" value="!LLM_MODEL_NAME!" /^>
echo     ^</envs^>
echo     ^<module name="oxygent-core" /^>
echo     ^<option name="VM_PARAMETERS" value="!JVM_ARGS!" /^>
echo     ^<method v="2"^>
echo       ^<option name="Make" enabled="true" /^>
echo     ^</method^>
echo   ^</configuration^>
echo ^</component^>
) > "%XML_FILE%"

echo.
echo "IDEA project template configuration completed!"
echo.
echo "Generated files:"
echo     ".run/Template Application.run.xml - Java application default template"
echo.
echo "Usage instructions:"
echo    "1. Restart IDEA (recommended)"
echo    "2. When creating new Java application run configurations, this template will be used automatically"
echo    "3. VM parameters and environment variables are pre-configured"
echo.

REM FIXED: Corrected condition logic
if /i "!LLM_API_KEY!"=="EMPTY" (
    echo "Note: You used default placeholder values. Please modify the environment variables in IDEA run configurations as needed!"
) else (
    echo "Configuration completed! New run configurations will include your configured environment variables and necessary VM parameters!"
)

pause
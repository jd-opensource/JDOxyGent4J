#!/bin/bash

# IDEA 项目简化配置脚本 - 只生成 .run 模板配置
# 只创建 Template Application.run.xml 文件，包含默认 VM 参数和环境变量

set -e

echo "🔧 开始配置 IDEA 项目默认运行模板..."

# 检查是否在 Maven 项目根目录
if [ ! -f "pom.xml" ]; then
    echo "❌ 错误：请在项目根目录（含 pom.xml）下运行此脚本"
    exit 1
fi

echo ""
echo "📋 请配置 LLM 相关环境变量（直接回车则使用占位符值）："

read -p "🔑 OXY_LLM_API_KEY (您的 LLM API 密钥): " LLM_API_KEY
read -p "🌐 OXY_LLM_BASE_URL (您的 LLM API 基础 URL): " LLM_BASE_URL
read -p "🤖 OXY_LLM_MODEL_NAME (您要使用的模型名称): " LLM_MODEL_NAME

echo ""
echo "✅ 环境变量配置："
echo "   API_KEY: ${LLM_API_KEY}"
echo "   BASE_URL: ${LLM_BASE_URL}"
echo "   MODEL_NAME: ${LLM_MODEL_NAME}"
echo ""

JVM_ARGS="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/sun.util.calendar=ALL-UNNAMED --add-opens java.base/java.math=ALL-UNNAMED"

# 创建 .run 目录
RUN_TEMPLATE_DIR=".run"
mkdir -p "$RUN_TEMPLATE_DIR"

echo "📝 创建 Template Application.run.xml..."

# 生成 Template Application.run.xml
cat > "$RUN_TEMPLATE_DIR/Template Application.run.xml" <<EOF
<component name="ProjectRunConfigurationManager">
  <configuration default="true" type="Application" factoryName="Application">
    <option name="ALTERNATIVE_JRE_PATH" value="17" />
    <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="true" />
    <envs>
      <env name="OXY_LLM_API_KEY" value="$LLM_API_KEY" />
      <env name="OXY_LLM_BASE_URL" value="$LLM_BASE_URL" />
      <env name="OXY_LLM_MODEL_NAME" value="$LLM_MODEL_NAME" />
    </envs>
    <module name="oxygent-core" />
    <option name="VM_PARAMETERS" value="$JVM_ARGS" />
    <method v="2">
      <option name="Make" enabled="true" />
    </method>
  </configuration>
</component>
EOF

echo ""
echo "✅ IDEA项目模板配置完成！"
echo ""
echo "📋 生成的文件："
echo "   📄 .run/Template Application.run.xml - Java应用程序默认模板"
echo ""
echo "📖 使用说明："
echo "   1. 🔄 重启IDEA（推荐）"
echo "   2. 🆕 新建Java应用运行配置时将自动使用此模板"
echo "   3. ✅ VM参数和环境变量都已预配置"
echo ""

if [[ "$LLM_API_KEY" != "EMPTY" ]]; then
    echo "🎯 已完成配置！新的运行配置将包含您设置的环境变量和必要的VM参数！"
else
    echo "⚠️  注意：您使用了默认占位符值，请根据需要在IDEA中修改运行配置的环境变量！"
fi
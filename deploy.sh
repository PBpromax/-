#!/bin/bash
# ============================================
# CampusHub 生产环境一键部署脚本
# 用法: bash deploy.sh
# ============================================

set -e

echo "=== CampusHub 生产环境部署 ==="
echo ""

# 1. 检查 Docker 环境
if ! command -v docker &> /dev/null; then
    echo "[错误] 请先安装 Docker"
    exit 1
fi

if ! docker compose version &> /dev/null; then
    echo "[错误] 请先安装 Docker Compose v2"
    exit 1
fi

# 2. 切换到项目目录
cd "$(dirname "$0")"

if [ -f ".env" ]; then
    set -a
    # shellcheck disable=SC1091
    . ./.env
    set +a
fi

# 3. 拉取最新代码（如使用 git 部署）
if [ -d ".git" ]; then
    echo "[1/4] 拉取最新代码..."
    git pull "${DEPLOY_REMOTE:-origin}" "${DEPLOY_BRANCH:-main}"
else
    echo "[1/4] 跳过 git pull（非 git 部署）"
fi

# 4. 检查环境变量（不在仓库中保存的敏感信息）
if [ -z "${CAMPUSHUB_JWT_SECRET:-}" ]; then
    export CAMPUSHUB_JWT_SECRET="$(openssl rand -base64 32)"
    echo "[提示] 未设置 CAMPUSHUB_JWT_SECRET，已为本次部署生成随机值。"
fi

missing_vars=""
for var_name in DB_USERNAME DB_PASSWORD DB_ROOT_PASSWORD CAMPUSHUB_JWT_SECRET; do
    if [ -z "${!var_name:-}" ]; then
        missing_vars="$missing_vars $var_name"
    fi
done

if [ -n "$missing_vars" ]; then
    echo "[错误] 缺少必要环境变量:$missing_vars"
    echo "请复制 .env.example 为 .env 并填写强密码后重试。"
    exit 1
fi

echo ""
echo "[2/4] 构建 Docker 镜像..."
docker compose -f docker-compose-prod.yml build

echo ""
echo "[3/4] 启动服务..."
docker compose -f docker-compose-prod.yml up -d

echo ""
echo "[4/4] 等待服务就绪..."
sleep 15

# 5. 健康检查
echo ""
echo "=== 健康检查 ==="
if curl -sf http://localhost/api/v1/health > /dev/null 2>&1; then
    echo "[OK] 后端服务正常"
else
    echo "[警告] 后端还未就绪，查看日志: docker logs campushub-backend"
fi

if curl -sf http://localhost/ > /dev/null 2>&1; then
    echo "[OK] 前端页面可访问"
else
    echo "[警告] 前端还未就绪，查看日志: docker logs campushub-nginx"
fi

echo ""
echo "=== 部署完成 ==="
echo "访问地址: http://$(curl -s ifconfig.me 2>/dev/null || echo 'YOUR_IP')"
echo ""
echo "常用命令:"
echo "  查看日志: docker compose -f docker-compose-prod.yml logs -f"
echo "  重启服务: docker compose -f docker-compose-prod.yml restart"
echo "  停止服务: docker compose -f docker-compose-prod.yml down"

# 本地 Kubernetes 一键部署脚本（面向 minikube / kind，Windows PowerShell）
# 用法：在 mao-ai-code-mother-master 目录下执行  .\k8s\deploy.ps1
# 前置：已安装 minikube、kubectl、docker、maven，且 minikube 已启动（minikube start）
param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"

function Require-Command($cmd) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        Write-Error "未找到命令: $cmd，请先安装并加入 PATH"
        exit 1
    }
}

Require-Command minikube
Require-Command kubectl
Require-Command docker
Require-Command mvn

$K8S_DIR = $PSScriptRoot
$ROOT = Resolve-Path (Join-Path $K8S_DIR "..")
$MICRO = Join-Path $ROOT "mao-ai-code-mother-microservice"

# 1. 将 Docker 客户端指向 minikube 内置守护进程（镜像构建进集群，无需推送到远端仓库）
Write-Host "==> 切换 Docker 环境到 minikube" -ForegroundColor Cyan
& minikube docker-env --shell powershell | Invoke-Expression
if ($LASTEXITCODE -ne 0) { Write-Error "无法切换到 minikube docker 环境"; exit 1 }

# 2. Maven 打包所有微服务模块
if (-not $SkipBuild) {
    Write-Host "==> Maven 打包微服务（跳过测试）" -ForegroundColor Cyan
    & mvn -f "$MICRO\pom.xml" -pl mao-ai-code-common,mao-ai-code-model,mao-ai-code-client,mao-ai-code-user,mao-ai-code-app,mao-ai-code-ai,mao-ai-code-screenshot,mao-ai-code-gateway -am package -DskipTests -q
    if ($LASTEXITCODE -ne 0) { Write-Error "Maven 构建失败"; exit 1 }
}

# 3. 构建各微服务 Docker 镜像
$modules = @(
    @{ dir = "mao-ai-code-user";       img = "yu-ai-code/user:latest" },
    @{ dir = "mao-ai-code-app";        img = "yu-ai-code/app:latest" },
    @{ dir = "mao-ai-code-screenshot"; img = "yu-ai-code/screenshot:latest" },
    @{ dir = "mao-ai-code-gateway";    img = "yu-ai-code/gateway:latest" }
)
foreach ($m in $modules) {
    $d = Join-Path $MICRO $m.dir
    Write-Host "==> 构建镜像 $($m.img)" -ForegroundColor Cyan
    & docker build -t $m.img "$d"
    if ($LASTEXITCODE -ne 0) { Write-Error "镜像构建失败: $($m.img)"; exit 1 }
}

# 4. 部署到 Kubernetes
Write-Host "==> 应用 Kubernetes 清单" -ForegroundColor Cyan
& kubectl apply -f "$K8S_DIR\00-namespace.yaml"
& kubectl apply -f "$K8S_DIR\01-config.yaml"
& kubectl apply -f "$K8S_DIR\infra"
& kubectl apply -f "$K8S_DIR\services"
if ($LASTEXITCODE -ne 0) { Write-Error "kubectl apply 失败"; exit 1 }

Write-Host "" 
Write-Host "==> 部署完成" -ForegroundColor Green
Write-Host "查看 Pod 状态:   kubectl -n yu-ai-code get pods" -ForegroundColor Yellow
Write-Host "查看网关路由:     kubectl -n yu-ai-code get svc mao-ai-code-gateway" -ForegroundColor Yellow
Write-Host "本地访问后端:     kubectl -n yu-ai-code port-forward svc/mao-ai-code-gateway 8123:8123" -ForegroundColor Yellow
Write-Host "  （之后前端 dev server 的 /api 代理即可指向 http://localhost:8123）" -ForegroundColor Yellow
Write-Host "RabbitMQ 管理台:  kubectl -n yu-ai-code port-forward svc/rabbitmq 15672:15672  -> http://localhost:15672 (guest/guest)" -ForegroundColor Yellow

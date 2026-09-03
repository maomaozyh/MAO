$root = if ($PSScriptRoot) { $PSScriptRoot } else { Split-Path -Parent $MyInvocation.MyCommand.Path }
$logs = Join-Path $root "logs"
New-Item -ItemType Directory -Force -Path $logs | Out-Null

# 文件锁：避免重复/并发启动导致多个实例抢端口
$lock = Join-Path $root "start.lock"
if (Test-Path $lock) {
    $age = (Get-Date) - (Get-Item $lock).LastWriteTime
    if ($age.TotalSeconds -lt 120) {
        Write-Host "another start is already running (lock exists), exit."
        exit
    }
}
New-Item -ItemType File -Path $lock -Force | Out-Null

try {
    # 1) 先杀掉所有旧 java 进程，保证不会重复
    Get-CimInstance Win32_Process -Filter "Name='java.exe'" | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
    Start-Sleep -Seconds 3

    # 2) 启动 4 个服务（各 1 个实例）
    $jvm = @("-Xms128m","-Xmx512m","-XX:+TieredCompilation","-XX:TieredStopAtLevel=1","-noverify")
    foreach ($m in @("user","app","screenshot","gateway")) {
        $jar = Join-Path $root "mao-ai-code-$m/target/mao-ai-code-$m-1.0-SNAPSHOT.jar"
        if (-not (Test-Path $jar)) { Write-Host "MISSING $jar"; continue }
        Start-Process -FilePath "java" -WorkingDirectory $root `
            -ArgumentList ($jvm + @("-jar",$jar)) `
            -RedirectStandardOutput (Join-Path $logs "$m.out.log") `
            -RedirectStandardError (Join-Path $logs "$m.err.log") `
            -WindowStyle Hidden
        Write-Host "launched $m"
    }
    Start-Sleep -Seconds 1
    Write-Host "current java processes: $((Get-CimInstance Win32_Process -Filter "Name='java.exe'").Count)"
} finally {
    Remove-Item $lock -Force -ErrorAction SilentlyContinue
}

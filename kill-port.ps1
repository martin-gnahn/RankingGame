param(
    [Parameter(Mandatory = $true)]
    [int]$Port
)

$processId = (Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue).OwningProcess

if ($processId) {
    Stop-Process -Id $processId -Force
    Write-Host "Killed process $processId using port $Port"
} else {
    Write-Host "No process found using port $Port"
}
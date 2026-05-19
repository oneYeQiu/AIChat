$file = "backend/src/main/resources/application.yml"
if (Test-Path $file) {
    $content = Get-Content $file -Raw
    $content = $content -replace 'sk-f96d3b0492b7433dbb35a0353bf2a8b8', 'YOUR-API-KEY-HERE'
    Set-Content $file -Value $content -NoNewline
}

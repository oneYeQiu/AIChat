@echo off
SET FILTER_BRANCH_SQUELCH_WARNING=1
git filter-branch --force --tree-filter "powershell -Command \"if (Test-Path 'backend/src/main/resources/application.yml') { (Get-Content 'backend/src/main/resources/application.yml' -Raw) -replace 'sk-f96d3b0492b7433dbb35a0353bf2a8b8', 'YOUR-API-KEY-HERE' | Set-Content 'backend/src/main/resources/application.yml' -NoNewline }\"" HEAD

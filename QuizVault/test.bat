@echo off
REM Register
curl.exe -s -X POST -d "username=admin&password=admin&role=Quizmaster" http://localhost:8080/api/register
echo.

REM Login - save full response to temp file
curl.exe -s -X POST -d "username=admin&password=admin" http://localhost:8080/api/login > _login.tmp
type _login.tmp
echo.

REM Use PowerShell to extract token cleanly
for /f %%T in ('powershell -NoProfile -Command "(Get-Content _login.tmp | ConvertFrom-Json).token"') do set TOKEN=%%T
echo TOKEN=%TOKEN%
del _login.tmp

REM Add 3 questions
curl.exe -s -X POST -H "X-Token: %TOKEN%" -d "text=What+is+1+plus+1&answer=2&tags=Math&diff=Easy" http://localhost:8080/api/questions/add
echo.
curl.exe -s -X POST -H "X-Token: %TOKEN%" -d "text=What+is+2+plus+2&answer=4&tags=Math&diff=Easy" http://localhost:8080/api/questions/add
echo.
curl.exe -s -X POST -H "X-Token: %TOKEN%" -d "text=Capital+of+France&answer=Paris&tags=Geography&diff=Medium" http://localhost:8080/api/questions/add
echo.

REM Fetch all questions
echo.
echo ALL QUESTIONS:
curl.exe -s http://localhost:8080/api/questions
echo.

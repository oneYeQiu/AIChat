$body='{"username":"testbot","password":"bot123456"}'
$r=Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method Post -Body $body -ContentType "application/json" -TimeoutSec 5
$token=$r.data.token
$uid=$r.data.userId
Write-Host "Login OK uid=$uid"

$h=@{Authorization="***"}
$f=Invoke-RestMethod -Uri http://localhost:8080/api/friends -Headers $h -TimeoutSec 5
$ai=$f.data | Where-Object {$_.username -eq "ai_bot"} | Select-Object -First 1
$aiId=$ai.id
Write-Host "AI Bot id=$aiId"

$mb='{"receiverId":'+$aiId+',"content":"晚上好！你在干什么呀？"}'
$h2=@{Authorization="***";"Content-Type"="application/json"}
$s=Invoke-RestMethod -Uri http://localhost:8080/api/messages/send -Method Post -Body $mb -Headers $h2 -TimeoutSec 10
Write-Host "Sent: id=$($s.data.id)"

Start-Sleep 10

$hist=Invoke-RestMethod -Uri "http://localhost:8080/api/messages/$aiId" -Headers $h -TimeoutSec 5
Write-Host "Total messages: $($hist.data.totalElements)"
foreach($msg in ($hist.data.content | Sort-Object createdAt)) {
  $dir=if($msg.senderId -eq $uid){"[ME ]"}else{"[AI ]"}
  Write-Host "$dir $($msg.content)"
}
Write-Host "=== DONE ==="

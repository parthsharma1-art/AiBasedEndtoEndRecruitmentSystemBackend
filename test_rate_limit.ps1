$url = "http://localhost:8081/api/test/any"
for ($i = 1; $i -le 55; $i++) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -ErrorAction Stop
        Write-Host "Request $i - Status: $($response.StatusCode)"
    } catch {
        Write-Host "Request $i - Status: $($_.Exception.Response.StatusCode.value__)"
    }
}

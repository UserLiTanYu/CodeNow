param(
    [string]$BackendUrl = "http://localhost:18080",
    [string]$FrontendUrl = "http://localhost:18081",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "123456"
)

$ErrorActionPreference = "Stop"

function Invoke-CodeNowJson {
    param(
        [Parameter(Mandatory)] [string]$Method,
        [Parameter(Mandatory)] [string]$Uri,
        [object]$Body,
        [hashtable]$Headers = @{}
    )

    $parameters = @{ Method = $Method; Uri = $Uri; Headers = $Headers }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json; charset=utf-8"
        $parameters.Body = $Body | ConvertTo-Json -Depth 8 -Compress
    }
    $response = Invoke-RestMethod @parameters
    if ($response.code -ne 200) {
        throw "Request failed: $Method $Uri (code=$($response.code), message=$($response.message))"
    }
    return $response
}

$ready = $false
for ($attempt = 1; $attempt -le 60; $attempt++) {
    try {
        Invoke-WebRequest -UseBasicParsing -Uri "$FrontendUrl/blog" | Out-Null
        Invoke-CodeNowJson -Method Get -Uri "$BackendUrl/api/blog/articles?pageNum=1&pageSize=1" | Out-Null
        $ready = $true
        break
    } catch {
        Start-Sleep -Seconds 2
    }
}
if (-not $ready) { throw "Services did not become ready" }

$login = Invoke-CodeNowJson -Method Post -Uri "$BackendUrl/api/auth/login" -Body @{
    username = $AdminUsername
    password = $AdminPassword
}
$headers = @{ Authorization = $login.data.token }

$suffix = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$categoryName = "RehearsalCategory-$suffix"
$tagName = "RehearsalTag-$suffix"
$articleTitle = "RehearsalArticle-$suffix"
$commentContent = "RehearsalComment-$suffix"

Invoke-CodeNowJson -Method Post -Uri "$BackendUrl/api/categories" -Headers $headers -Body @{
    name = $categoryName; description = "deployment rehearsal"; sort = 99
} | Out-Null
Invoke-CodeNowJson -Method Post -Uri "$BackendUrl/api/tags" -Headers $headers -Body @{
    name = $tagName
} | Out-Null

$categories = Invoke-CodeNowJson -Method Get -Uri "$BackendUrl/api/categories" -Headers $headers
$tags = Invoke-CodeNowJson -Method Get -Uri "$BackendUrl/api/tags" -Headers $headers
$categoryId = ($categories.data | Where-Object name -eq $categoryName).id
$tagId = ($tags.data | Where-Object name -eq $tagName).id
if (-not $categoryId -or -not $tagId) { throw "Created category or tag was not found" }

Invoke-CodeNowJson -Method Post -Uri "$BackendUrl/api/articles" -Headers $headers -Body @{
    title = $articleTitle
    content = "# Docker Compose deployment rehearsal"
    summary = "automated smoke test"
    categoryId = $categoryId
    status = 1
    tagIds = @($tagId)
} | Out-Null

$articles = Invoke-CodeNowJson -Method Get -Uri "$BackendUrl/api/blog/articles?pageNum=1&pageSize=100"
$articleId = ($articles.data.records | Where-Object { $_.article.title -eq $articleTitle }).article.id
if (-not $articleId) { throw "Published article was not found in the public API" }

Invoke-CodeNowJson -Method Post -Uri "$BackendUrl/api/comments" -Headers $headers -Body @{
    articleId = $articleId; parentId = 0; content = $commentContent; nickname = "Rehearsal"
} | Out-Null
$comments = Invoke-CodeNowJson -Method Get -Uri "$BackendUrl/api/comments?pageNum=1&pageSize=100&articleId=$articleId" -Headers $headers
if (-not ($comments.data.records | Where-Object content -eq $commentContent)) {
    throw "Created comment was not found"
}

$imagePath = Join-Path ([System.IO.Path]::GetTempPath()) "codenow-smoke-$suffix.png"
try {
    [System.IO.File]::WriteAllBytes($imagePath, [Convert]::FromBase64String("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII="))
    $uploadRaw = & curl.exe --fail --silent --show-error -H "Authorization: $($login.data.token)" -F "file=@$imagePath;type=image/png" "$BackendUrl/api/upload/image"
    if ($LASTEXITCODE -ne 0) { throw "Image upload request failed" }
    $upload = $uploadRaw | ConvertFrom-Json
    if ($upload.code -ne 200 -or -not $upload.data.url) { throw "Image upload response was invalid" }
    Invoke-WebRequest -UseBasicParsing -Uri "$FrontendUrl$($upload.data.url)" | Out-Null
} finally {
    Remove-Item -LiteralPath $imagePath -Force -ErrorAction SilentlyContinue
}

$article = Invoke-CodeNowJson -Method Get -Uri "$BackendUrl/api/blog/articles/$articleId"
if ($article.data.article.title -ne $articleTitle) { throw "Public article detail did not match" }

Write-Output "Smoke test passed: article=$articleId upload=$($upload.data.url)"

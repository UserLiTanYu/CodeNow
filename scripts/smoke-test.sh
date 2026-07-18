#!/usr/bin/env bash
set -euo pipefail

BACKEND_URL="${BACKEND_URL:-http://localhost:8080}"
FRONTEND_URL="${FRONTEND_URL:-http://localhost}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-123456}"

for attempt in $(seq 1 60); do
  if curl --fail --silent "$FRONTEND_URL/blog" >/dev/null && curl --fail --silent "$BACKEND_URL/api/blog/articles?pageNum=1&pageSize=1" >/dev/null; then
    break
  fi
  if [ "$attempt" -eq 60 ]; then
    echo "services did not become ready" >&2
    exit 1
  fi
  sleep 2
done

captcha_response=$(curl --fail --silent --show-error "$BACKEND_URL/api/auth/captcha")
read -r captcha_id captcha_code < <(printf '%s' "$captcha_response" | python3 -c '
import base64, json, re, sys
captcha = json.load(sys.stdin)["data"]
svg = base64.b64decode(captcha["image"].split(",", 1)[1]).decode()
numbers = re.search(r"(\d+) \+ (\d+) = \?", svg)
if not numbers:
    raise SystemExit("unable to parse captcha")
print(captcha["captchaId"], int(numbers.group(1)) + int(numbers.group(2)))
')

login_response=$(curl --fail --silent --show-error \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\",\"captchaId\":\"$captcha_id\",\"captchaCode\":\"$captcha_code\"}" \
  "$BACKEND_URL/api/auth/login")
token=$(printf '%s' "$login_response" | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["token"])')
auth_header_name="Authorization"

suffix=$(date +%s)
category_name="CI分类-$suffix"
tag_name="CI标签-$suffix"
article_title="CI端到端文章-$suffix"

curl --fail --silent --show-error -H "${auth_header_name}: ${token}" -H 'Content-Type: application/json' \
  -d "{\"name\":\"$category_name\",\"description\":\"部署演练\",\"sort\":99}" \
  "$BACKEND_URL/api/categories" >/dev/null
curl --fail --silent --show-error -H "${auth_header_name}: ${token}" -H 'Content-Type: application/json' \
  -d "{\"name\":\"$tag_name\"}" "$BACKEND_URL/api/tags" >/dev/null

categories=$(curl --fail --silent --show-error -H "${auth_header_name}: ${token}" "$BACKEND_URL/api/categories")
tags=$(curl --fail --silent --show-error -H "${auth_header_name}: ${token}" "$BACKEND_URL/api/tags")
category_id=$(printf '%s' "$categories" | CATEGORY_NAME="$category_name" python3 -c 'import json,os,sys; print(next(x["id"] for x in json.load(sys.stdin)["data"] if x["name"] == os.environ["CATEGORY_NAME"]))')
tag_id=$(printf '%s' "$tags" | TAG_NAME="$tag_name" python3 -c 'import json,os,sys; print(next(x["id"] for x in json.load(sys.stdin)["data"] if x["name"] == os.environ["TAG_NAME"]))')

curl --fail --silent --show-error -H "${auth_header_name}: ${token}" -H 'Content-Type: application/json' \
  -d "{\"title\":\"$article_title\",\"content\":\"# Docker Compose smoke test\",\"summary\":\"自动化部署演练\",\"categoryId\":$category_id,\"status\":1,\"tagIds\":[$tag_id]}" \
  "$BACKEND_URL/api/articles" >/dev/null

articles=$(curl --fail --silent --show-error "$BACKEND_URL/api/blog/articles?pageNum=1&pageSize=100")
article_id=$(printf '%s' "$articles" | ARTICLE_TITLE="$article_title" python3 -c 'import json,os,sys; print(next(x["article"]["id"] for x in json.load(sys.stdin)["data"]["records"] if x["article"]["title"] == os.environ["ARTICLE_TITLE"]))')

curl --fail --silent --show-error -H "${auth_header_name}: ${token}" -H 'Content-Type: application/json' \
  -d "{\"articleId\":$article_id,\"parentId\":0,\"content\":\"部署演练评论\",\"nickname\":\"CI\"}" \
  "$BACKEND_URL/api/comments" >/dev/null

comments=$(curl --fail --silent --show-error -H "${auth_header_name}: ${token}" "$BACKEND_URL/api/comments?pageNum=1&pageSize=100&articleId=$article_id")
printf '%s' "$comments" | python3 -c 'import json,sys; assert any(x["content"] == "部署演练评论" for x in json.load(sys.stdin)["data"]["records"])'

image_file=$(mktemp --suffix=.png)
trap 'rm -f "$image_file"' EXIT
printf 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=' | base64 --decode > "$image_file"
upload_response=$(curl --fail --silent --show-error -H "${auth_header_name}: ${token}" -F "file=@$image_file;type=image/png" "$BACKEND_URL/api/upload/image")
upload_url=$(printf '%s' "$upload_response" | python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["url"])')
curl --fail --silent --show-error "$FRONTEND_URL$upload_url" >/dev/null

curl --fail --silent --show-error "$BACKEND_URL/api/blog/articles/$article_id" | \
  ARTICLE_TITLE="$article_title" python3 -c 'import json,os,sys; assert json.load(sys.stdin)["data"]["article"]["title"] == os.environ["ARTICLE_TITLE"]'

echo "smoke test passed: article=$article_id upload=$upload_url"

import requests
import json

url = "http://localhost:8080/api/auth/login"
params = {
    "username": "testuser",
    "password": "123456"
}

try:
    response = requests.post(url, params=params)
    print(f"状态码: {response.status_code}")
    print(f"响应: {json.dumps(response.json(), ensure_ascii=False, indent=2)}")
except Exception as e:
    print(f"错误: {e}")

import requests

BASE_URL = "http://localhost:8080/api/auth"

def test_register():
    url = f"{BASE_URL}/register"
    data = {
        "username": "202235020845",
        "email": "2890205717@qq.com",
        "password": "123456",
        "confirmPassword": "123456"
    }
    
    print(f"Testing registration: {data['username']} / {data['email']}")
    
    try:
        response = requests.post(url, json=data)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.text}")
        
        result = response.json()
        if result.get('code') == 200:
            print("✓ Registration successful!")
        else:
            print(f"✗ Registration failed: {result.get('message')}")
    except Exception as e:
        print(f"✗ Error: {e}")

if __name__ == "__main__":
    test_register()

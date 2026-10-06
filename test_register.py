import requests
import json

BASE_URL = "http://localhost:8080/api/auth"

# Test registration with a new user
def test_register():
    url = f"{BASE_URL}/register"
    data = {
        "username": "testuser12345",
        "email": "test12345@example.com",
        "password": "password123",
        "confirmPassword": "password123"
    }
    
    print(f"Testing registration: {data['username']}")
    print(f"URL: {url}")
    
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

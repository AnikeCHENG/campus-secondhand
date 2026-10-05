import requests
import json

BASE_URL = "http://localhost:8080/api/auth"

def test_login():
    url = f"{BASE_URL}/login"
    data = {
        "username": "testuser12345",
        "password": "password123"
    }
    
    print(f"Testing login: {data['username']}")
    
    try:
        response = requests.post(url, data=data)
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.text}")
        
        result = response.json()
        if result.get('code') == 200:
            print("✓ Login successful!")
            token = result.get('data', {}).get('token')
            if token:
                print(f"Token received: {token[:50]}...")
        else:
            print(f"✗ Login failed: {result.get('message')}")
    except Exception as e:
        print(f"✗ Error: {e}")

if __name__ == "__main__":
    test_login()

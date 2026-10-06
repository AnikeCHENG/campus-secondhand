import os
import requests
import json

# 图片文件夹路径
images_dir = "frontend/campus-secondhand/src/images"
# API基础URL
api_base = "http://localhost:8080/api/products"

# 获取图片列表
def get_images():
    images = []
    if os.path.exists(images_dir):
        for filename in os.listdir(images_dir):
            if filename.endswith('.avif'):
                # 提取商品名称（去掉扩展名）
                product_name = os.path.splitext(filename)[0]
                # 构建图片路径
                image_path = f"/src/images/{filename}"
                images.append((product_name, image_path))
    return images

# 创建商品
def create_product(product_name, image_path):
    try:
        # 构造商品数据
        data = {
            "title": product_name,
            "description": f"这是一个{product_name}",
            "price": "99.99",
            "category": "other",
            "condition": "good",
            "images": image_path
        }
        # 发送创建请求
        headers = {
            "Content-Type": "application/json"
        }
        response = requests.post(f"{api_base}/create", json=data, headers=headers)
        if response.status_code == 200:
            data = response.json()
            if data.get('code') == 200:
                print(f"创建商品 {product_name} 成功")
                return True
            else:
                print(f"创建商品 {product_name} 失败: {data.get('message')}")
        else:
            print(f"创建商品 {product_name} 失败，状态码: {response.status_code}")
    except Exception as e:
        print(f"创建商品 {product_name} 时发生错误: {e}")
    return False

# 主函数
def main():
    print("开始创建商品...")
    
    # 获取图片列表
    images = get_images()
    print(f"找到 {len(images)} 张图片")
    
    # 创建商品
    for product_name, image_path in images:
        create_product(product_name, image_path)
    
    print("创建完成！")

if __name__ == "__main__":
    main()

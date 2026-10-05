import os
import requests
import json

***REMOVED*** 图片文件夹路径
images_dir = "frontend/campus-secondhand/src/images"
***REMOVED*** API基础URL
api_base = "http://localhost:8080/api/products"

***REMOVED*** 获取图片列表
def get_images():
    images = []
    image_names = []
    if os.path.exists(images_dir):
        for filename in os.listdir(images_dir):
            if filename.endswith('.avif'):
                ***REMOVED*** 提取商品名称（去掉扩展名）
                product_name = os.path.splitext(filename)[0]
                image_names.append(product_name)
                images.append((product_name, filename))
    return images, image_names

***REMOVED*** 批量更新商品图片和删除没有对应图片的商品
def update_images_batch(image_names):
    try:
        ***REMOVED*** 构造请求数据
        data = {
            "imageNames": image_names
        }
        ***REMOVED*** 发送请求
        headers = {
            "Content-Type": "application/json"
        }
        response = requests.post(f"{api_base}/update-images", json=data, headers=headers)
        if response.status_code == 200:
            data = response.json()
            if data.get('code') == 200:
                print(f"批量操作成功: 更新了 {data.get('data', {}).get('updated', 0)} 个商品，删除了 {data.get('data', {}).get('deleted', 0)} 个商品")
                return True
            else:
                print(f"批量操作失败: {data.get('message')}")
        else:
            print(f"批量操作失败，状态码: {response.status_code}")
    except Exception as e:
        print(f"批量操作时发生错误: {e}")
    return False

***REMOVED*** 主函数
def main():
    print("开始处理商品图片...")
    
    ***REMOVED*** 获取图片列表
    images, image_names = get_images()
    print(f"找到 {len(images)} 张图片")
    print(f"图片名称列表: {image_names}")
    
    ***REMOVED*** 批量更新商品图片和删除没有对应图片的商品
    if image_names:
        update_images_batch(image_names)
    else:
        print("没有找到图片，无法执行操作")
    
    print("处理完成！")

if __name__ == "__main__":
    main()

import os
import requests
import json

***REMOVED*** API基础URL
api_base = "http://localhost:8080/api/products"

***REMOVED*** 分类映射字典，包含关键词和对应的分类
category_mapping = {
    'books': ['书', '教材', '课本', '小说', '简史', '传', '四世同堂', '在细雨中呼喊', '堂吉坷德', '明朝那些事儿', '活着', '狂人日记', '许三观卖血记', '资治通鉴'],
    'electronics': ['手机', '电脑', '平板', '耳机', '相机', '电子'],
    'transport': ['自行车', '电动车', '出行', '车'],
    'gaming': ['游戏', '手柄', '键盘', '鼠标', '游戏机'],
    'clothing': ['衣服', '裤子', '鞋子', '帽子', '服饰'],
    'living': ['生活用品', '家具', '被子', '枕头', '厨具'],
    'other': []
}

***REMOVED*** 获取商品列表
def get_products():
    try:
        response = requests.get(f"{api_base}/list")
        if response.status_code == 200:
            data = response.json()
            if data.get('code') == 200:
                return data.get('data', [])
    except Exception as e:
        print(f"获取商品列表失败: {e}")
    return []

***REMOVED*** 根据商品名称获取分类
def get_category(product_name):
    for category, keywords in category_mapping.items():
        for keyword in keywords:
            if keyword in product_name:
                return category
    return 'other'  ***REMOVED*** 默认分类

***REMOVED*** 更新商品分类
def update_product_category(product_id, category):
    try:
        ***REMOVED*** 构造更新数据
        data = {
            "category": category
        }
        ***REMOVED*** 发送更新请求
        headers = {
            "Content-Type": "application/json"
        }
        response = requests.put(f"{api_base}/update/{product_id}", json=data, headers=headers)
        if response.status_code == 200:
            data = response.json()
            if data.get('code') == 200:
                print(f"更新商品 {product_id} 分类为 {category} 成功")
                return True
            else:
                print(f"更新商品 {product_id} 分类失败: {data.get('message')}")
        else:
            print(f"更新商品 {product_id} 分类失败，状态码: {response.status_code}")
    except Exception as e:
        print(f"更新商品 {product_id} 分类时发生错误: {e}")
    return False

***REMOVED*** 主函数
def main():
    print("开始自动分类商品...")
    
    ***REMOVED*** 获取商品列表
    products = get_products()
    print(f"找到 {len(products)} 个商品")
    
    ***REMOVED*** 处理商品分类
    for product in products:
        product_id = product.get('id')
        product_title = product.get('title')
        
        ***REMOVED*** 获取分类
        category = get_category(product_title)
        print(f"商品 {product_title} (ID: {product_id}) 被分类为 {category}")
        
        ***REMOVED*** 更新商品分类
        update_product_category(product_id, category)
    
    print("分类完成！")

if __name__ == "__main__":
    main()

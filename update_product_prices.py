import os
import requests
import json

# API基础URL
api_base = "http://localhost:8080/api/products"

# 商品类型对应的价格范围（元）
price_ranges = {
    'books': {'min': 5, 'max': 50},  # 书籍：5-50元
    'electronics': {'min': 100, 'max': 2000},  # 电子产品：100-2000元
    'transport': {'min': 200, 'max': 1000},  # 出行工具：200-1000元
    'gaming': {'min': 50, 'max': 1000},  # 游戏数码：50-1000元
    'clothing': {'min': 20, 'max': 300},  # 服饰穿搭：20-300元
    'living': {'min': 10, 'max': 500},  # 生活用品：10-500元
    'other': {'min': 10, 'max': 200}  # 其他：10-200元
}

# 特定商品的具体价格
specific_prices = {
    '乒乓球': 10,           # 乒乓球：10元
    '乒乓球拍': 50,         # 乒乓球拍：50元
    '人类简史': 35,         # 人类简史：35元
    '保龄球': 150,          # 保龄球：150元
    '匹克球拍': 80,         # 匹克球拍：80元
    '名人传': 25,           # 名人传：25元
    '四世同堂': 30,         # 四世同堂：30元
    '在细雨中呼喊': 28,     # 在细雨中呼喊：28元
    '堂吉坷德': 40,         # 堂吉坷德：40元
    '小哑铃': 60,           # 小哑铃：60元
    '明朝那些事儿': 45,      # 明朝那些事儿：45元
    '活着': 25,             # 活着：25元
    '狂人日记': 20,         # 狂人日记：20元
    '篮球': 80,             # 篮球：80元
    '网球': 30,             # 网球：30元
    '网球拍': 120,          # 网球拍：120元
    '羽毛球': 20,           # 羽毛球：20元
    '羽毛球拍': 90,         # 羽毛球拍：90元
    '许三观卖血记': 25,     # 许三观卖血记：25元
    '资治通鉴': 100,        # 资治通鉴：100元
    '足球': 100,            # 足球：100元
    '跳绳': 15              # 跳绳：15元
}

# 获取商品列表
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

# 更新商品价格
def update_product_price(product_id, price):
    try:
        # 构造更新数据
        data = {
            "price": str(price)
        }
        # 发送更新请求
        headers = {
            "Content-Type": "application/json"
        }
        response = requests.put(f"{api_base}/update/{product_id}", json=data, headers=headers)
        if response.status_code == 200:
            data = response.json()
            if data.get('code') == 200:
                print(f"更新商品 {product_id} 价格为 {price} 元成功")
                return True
            else:
                print(f"更新商品 {product_id} 价格失败: {data.get('message')}")
        else:
            print(f"更新商品 {product_id} 价格失败，状态码: {response.status_code}")
    except Exception as e:
        print(f"更新商品 {product_id} 价格时发生错误: {e}")
    return False

# 主函数
def main():
    print("开始更新商品价格...")
    
    # 获取商品列表
    products = get_products()
    print(f"找到 {len(products)} 个商品")
    
    # 处理商品价格
    for product in products:
        product_id = product.get('id')
        product_title = product.get('title')
        product_category = product.get('category')
        
        # 优先使用特定商品的价格
        if product_title in specific_prices:
            price = specific_prices[product_title]
            print(f"商品 {product_title} (ID: {product_id}) 价格设置为 {price} 元")
        else:
            # 根据分类设置价格
            if product_category in price_ranges:
                # 这里简化处理，使用价格范围的中间值
                price_range = price_ranges[product_category]
                price = (price_range['min'] + price_range['max']) // 2
                print(f"商品 {product_title} (ID: {product_id}, 分类: {product_category}) 价格设置为 {price} 元")
            else:
                # 默认价格
                price = 50
                print(f"商品 {product_title} (ID: {product_id}, 分类: {product_category}) 价格设置为 {price} 元")
        
        # 更新商品价格
        update_product_price(product_id, price)
    
    print("价格更新完成！")

if __name__ == "__main__":
    main()

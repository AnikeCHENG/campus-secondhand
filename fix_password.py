import os
import pymysql

connection = pymysql.connect(
    host='localhost',
    user=os.environ.get('DB_USERNAME', 'root'),
    # 口令不再硬编码：此前 '123456' 与下方 bcrypt 哈希一并提交到公开仓库
    password=os.environ['DB_PASSWORD'],
    database=os.environ.get('DB_NAME', 'campus_secondhand'),
    charset='utf8mb4'
)

try:
    with connection.cursor() as cursor:
        # 该哈希曾用于重置 admin/testuser 的口令，不可再硬编码在仓库中
        password_hash = os.environ['PASSWORD_HASH']
        
        sql = "UPDATE users SET password = %s WHERE username IN ('testuser', 'admin', 'zjh123', 'zjh')"
        cursor.execute(sql, (password_hash,))
        
        connection.commit()
        
        cursor.execute("SELECT username, password FROM users WHERE username IN ('testuser', 'admin', 'zjh123', 'zjh')")
        results = cursor.fetchall()
        
        print("Updated passwords:")
        for row in results:
            print(f"  {row[0]}: {row[1]}")
            
finally:
    connection.close()

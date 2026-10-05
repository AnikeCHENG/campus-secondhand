import pymysql

connection = pymysql.connect(
    host='localhost',
    user='root',
    password='123456',
    database='waste_recycle_platform',
    charset='utf8mb4'
)

try:
    with connection.cursor() as cursor:
        password_hash = '$2b$12$BBQElYCGIVIK0lRCZOVfG.mqRV5H.SEfgadqX/xkIMr6/f2J.8BSS'
        
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

# -*- coding: utf-8 -*-
import pymysql, os

conn = pymysql.connect(host='127.0.0.1', port=3306, user='root', password='123456', charset='utf8mb4')
cur = conn.cursor()
cur.execute('USE token_erp')

base = os.path.dirname(os.path.abspath(__file__))
files = ['schema.sql', 'schema_biz.sql', 'schema_inventory.sql', 'init_data.sql']

for fn in files:
    path = os.path.join(base, fn)
    with open(path, 'r', encoding='utf-8') as f:
        sql = f.read()
    # 按分号拆分执行（跳过注释）
    statements = []
    for st in sql.split(';'):
        st = st.strip()
        if st and not st.startswith('--'):
            statements.append(st)
    for st in statements:
        try:
            cur.execute(st)
        except Exception as e:
            print('ERROR in', fn, ':', e)
            print('SQL:', st[:100])
    conn.commit()
    print('EXECUTED', fn)

# 统计表数量
cur.execute("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='token_erp'")
print('TOTAL TABLES:', cur.fetchone()[0])
conn.close()

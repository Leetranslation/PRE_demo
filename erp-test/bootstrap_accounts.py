#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
ERP 测试账号/角色/权限 初始化脚本（阶段0）
- 只作用于 token_erp_test（经 backend-test 8081 的接口操作）
- 幂等：角色/用户已存在则跳过；权限分配每次按规则全量重算
- 复用 ERP 自身 UserService/RoleService（BCrypt 哈希、事务）
"""
import json
import urllib.request
import urllib.error

BASE = "http://127.0.0.1:8081"

def call(method, path, token=None, body=None, query=None):
    url = BASE + path
    if query:
        url += "?" + "&".join(f"{k}={v}" for k, v in query.items())
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return json.loads(r.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode("utf-8"))

def login(username, password):
    res = call("POST", "/api/auth/login", body={"username": username, "password": password})
    return res.get("data", {}).get("token")

# 角色 -> 权限前缀规则
ROLE_RULES = {
    "SALES":      ["sales:"],
    "PURCHASE":   ["purchase:"],
    "PRODUCTION": ["production:", "mrp:"],
    "WAREHOUSE":  ["inventory:"],
}
ROLE_NAMES = {
    "SALES": "销售", "PURCHASE": "采购", "PRODUCTION": "生产", "WAREHOUSE": "仓库",
}
ACCOUNTS = [
    ("sales01", "Sales@123", "销售测试", "SALES"),
    ("purchase01", "Purchase@123", "采购测试", "PURCHASE"),
    ("production01", "Prod@123", "生产测试", "PRODUCTION"),
    ("warehouse01", "Wh@123", "仓库测试", "WAREHOUSE"),
]

def main():
    token = login("admin", "admin123")
    if not token:
        print("FAIL: admin 登录失败")
        return
    print("admin 登录成功")

    # 1. 拉取全部权限
    perms = call("GET", "/api/system/role/permission-list", token=token).get("data", [])
    print(f"权限总数: {len(perms)}")

    # 2. 现有角色
    roles = call("GET", "/api/system/role/list", token=token).get("data", [])
    role_by_code = {r["code"]: r for r in roles}

    # 3. 创建角色 + 分配权限
    for code, prefixes in ROLE_RULES.items():
        if code in role_by_code:
            role = role_by_code[code]
            print(f"角色 {code} 已存在 id={role['id']}")
        else:
            role = call("POST", "/api/system/role/create", token=token,
                        body={"code": code, "name": ROLE_NAMES[code], "dataScope": "self", "status": 1}).get("data")
            print(f"角色 {code} 创建 id={role['id']}")

        # 该角色专属权限
        ids = [p["id"] for p in perms if any(p["code"].startswith(pfx) for pfx in prefixes)]
        # 所有业务角色共享 base:* 菜单查看权限（type=='menu'）
        ids += [p["id"] for p in perms if p.get("type") == "menu" and p["code"].startswith("base:")]
        ids = sorted(set(ids))
        call("POST", "/api/system/role/assign", token=token, body=ids, query={"roleId": role["id"]})
        print(f"角色 {code} 分配权限 {len(ids)} 项")

    # 4. 创建用户 + 分配角色
    users = call("GET", "/api/system/user/list", token=token, query={"page": 1, "pageSize": 500}).get("data", {}).get("records", [])
    user_by_name = {u["username"]: u for u in users}
    fresh_roles = call("GET", "/api/system/role/list", token=token).get("data", [])
    role_id = {r["code"]: r["id"] for r in fresh_roles}

    for username, password, real_name, code in ACCOUNTS:
        if username in user_by_name:
            uid = user_by_name[username]["id"]
            print(f"用户 {username} 已存在 id={uid}")
        else:
            u = call("POST", "/api/system/user/create", token=token,
                     body={"username": username, "password": password, "realName": real_name, "status": 1}).get("data")
            uid = u["id"]
            print(f"用户 {username} 创建 id={uid}")
        call("POST", "/api/system/user/assign-roles", token=token, body=[role_id[code]], query={"userId": uid})
        print(f"用户 {username} 绑定角色 {code}")

    # 5. 验证每个账号能登录
    print("--- 登录验证 ---")
    for username, password, _, _ in ACCOUNTS:
        t = login(username, password)
        print(f"{username}: {'OK' if t else 'FAIL'}")

if __name__ == "__main__":
    main()

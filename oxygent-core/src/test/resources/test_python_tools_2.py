import requests
import pandas as pd
from urllib.parse import urlparse
import os
import sys

for path in sys.path:
    print path
# 下载文件
url = "http://outspace.s3-internal.cn-north-1.jdcloud-oss.com/test/stock_20260313132045-wzh.xlsx"
filename = "stock_20260313132045-wzh.xlsx"

try:
    print("正在下载库存数据文件...")
    response = requests.get(url, timeout=300)
    response.raise_for_status()

    with open(filename, 'wb') as f:
        f.write(response.content)

    print("文件下载成功: " + filename)
    print("文件大小: " + str(len(response.content) / 1024 / 1024) + " MB")

    # 检查文件基本信息
    df = pd.read_excel(filename)
    print("数据概览:")
    print("总行数: " + str(len(df)))
    print("列名: " + str(list(df.columns)))
    print("前5行数据:")
    print(df.head())

except Exception as e:
    print("下载或读取文件失败: " + str(e))
    raise e
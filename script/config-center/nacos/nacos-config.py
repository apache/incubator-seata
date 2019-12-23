#!/usr/bin/env python3
#  -*- coding: UTF-8 -*-

import http.client
import sys

if len(sys.argv) != 2:
    print ('python nacos-config.py nacosAddr')
    exit()

headers = {
    'content-type': "application/x-www-form-urlencoded"
}

hasError = False
for line in open('../config.txt'):
    pair = line.split('=')
    if len(pair) < 2:
        continue
    print (line),
    url_prefix = sys.argv[1]
    conn = http.client.HTTPConnection(url_prefix)
    if len(sys.argv) == 3:
        namespace=sys.argv[2]
        url_postfix = '/nacos/v1/cs/configs?dataId={0}&group=SEATA_GROUP&content={1}&tenant={2}'.format(str(pair[0]),str(line[line.index('=')+1:]).strip(),namespace)
    else:
        url_postfix = '/nacos/v1/cs/configs?dataId={}&group=SEATA_GROUP&content={}'.format(str(pair[0]),str(line[line.index('=')+1:])).strip()
    conn.request("POST", url_postfix, headers=headers)
    res = conn.getresponse()
    data = res.read()
    if data.decode("utf-8") != "true":
        hasError = True
if hasError:
    print ("init nacos config fail.")
else:
    print ("init nacos config finished, please start seata-server.")
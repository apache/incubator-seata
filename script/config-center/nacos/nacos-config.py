
#  -*- coding: UTF-8 -*-

import http.client
import sys
import getopt as opts
import urllib.parse


def get_params() -> dict:
    params = {
        '-n': 'public',
        '-u': '',
        '-p': ''
    }
    inputs, args = opts.getopt(sys.argv[2:], 'n:u:p:')
    for k, v in inputs:
        params[k] = v
    print(params)
    return params


if len(sys.argv) < 2:
    print(
        'python nacos-config.py host:port [-n namespace] [-u username] [-p password]')
    exit()

headers = {
    'content-type': "application/x-www-form-urlencoded"
}

hasError = False

params = get_params()
url_prefix = sys.argv[1]
namespace = params['-n']
username = params['-u']
password = params['-p']
url_postfix_base = f'/nacos/v1/cs/configs?group=SEATA_GROUP&tenant={namespace}'
if username != '' and password != '':
    url_postfix_base += f'&username={username}&password={password}'

for line in open('../config.txt'):
    pair = line.rstrip("\n").split('=')
    if len(pair) < 2 or pair[0] == '' or pair[1] == '':
        continue
    url_postfix = url_postfix_base + f'&dataId={urllib.parse.quote(str(pair[0]))}&content={urllib.parse.quote(str(pair[1])).strip()}'
    conn = http.client.HTTPConnection(url_prefix)
    conn.request("POST", url_postfix, headers=headers)
    res = conn.getresponse()
    data = res.read()
    if data.decode("utf-8") != "true":
        hasError = True
    print(f"{pair[0]}={pair[1]} {'fail' if hasError else 'success'}")
if hasError:
    print("init nacos config fail.")
else:
    print("init nacos config finished, please start seata-server.")

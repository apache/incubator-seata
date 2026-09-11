#!/usr/bin/env python3
#  -*- coding: UTF-8 -*-
# @Author : wangyeuwen
import http.client
import urllib.parse
import re


def get_params() -> dict:
    params = {
        'host': '127.0.0.1',
        'port': '8848',
        'tenant': '',
        'group': 'SEATA_GROUP',
        'username': '',
        'password': ''
    }
    host = input("Please enter the host of nacos.\n请输入nacos的host [localhost]\n>>> ")
    port = input("Please enter the port of nacos.\n请输入nacos的port [8848]\n>>> ")
    group = input("Please enter the group of nacos.\n请输入nacos的group [SEATA_GROUP]\n>>> ")
    tenant = input("Please enter the tenant of nacos.\n请输入nacos的tenant\n>>> ")
    username = input("Please enter the username of nacos.\n请输入nacos的username\n>>> ")
    password = input("Please enter the password of nacos.\n请输入nacos的password\n>>> ")
    confirm = input("Are you sure to continue? [y/n]")
    if confirm[0] == 'y' or confirm[0] == 'Y':
        if len(host) != 0: params['host'] = host
        if len(port) != 0: params['port'] = port
        if len(group) != 0: params['group'] = group
        if len(tenant) != 0: params['tenant'] = tenant
        if len(username) != 0: params['username'] = username
        if len(password) != 0: params['password'] = password
    elif confirm[0] == 'n' or confirm[0] == 'N':
        exit()
    else:
        print("Just enter y or n, please.")
        exit()

    return params


def error_exit():
    print(' init nacos config fail.')
    exit()


def get_pair(line: str) -> tuple:
    res = re.match(r"([\.\w]+)=(.*)", line)
    return res.groups() if res is not None else ['', '']


headers = {
    'content-type': "application/x-www-form-urlencoded"
}

hasError = False

params = get_params()

url_prefix = f"{params['host']}:{params['port']}"
tenant = params['tenant']
username = params['username']
password = params['password']
group = params['group']
url_postfix_base = f'/nacos/v1/cs/configs?group={group}&tenant={tenant}'

if username != '' and password != '':
    url_postfix_base += f'&username={username}&password={password}'

if url_prefix == ':':
    error_exit()

for line in open('../config.txt'):
    pair = get_pair(line.rstrip("\n"))
    if len(pair) < 2 or pair[0] == '' or pair[0].startswith("#") or pair[1] == '':
        continue
    url_postfix = url_postfix_base + f'&dataId={urllib.parse.quote(str(pair[0]))}&content={urllib.parse.quote(str(pair[1])).strip()}'
    conn = http.client.HTTPConnection(url_prefix)
    conn.request("POST", url_postfix, headers=headers)
    res = conn.getresponse()
    data = res.read().decode("utf-8")
    if data != "true":
        hasError = True
    print(f"{pair[0]}={pair[1]} {data if hasError else 'success'}")

if hasError:
    print("init nacos config fail.")
else:
    print("init nacos config finished, please start seata-server.")

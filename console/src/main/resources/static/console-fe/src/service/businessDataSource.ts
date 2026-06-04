/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import request from '@/utils/request';

export type BusinessDataSourceInfo = {
  name: string;
  resourceId: string;
  databaseName: string;
  datasource: string;
  dynamic: boolean;
  enabled: boolean;
};

export type BusinessDataSourceForm = {
  name: string;
  url: string;
  username: string;
  password: string;
  passwordSecretRef: string;
  datasource: string;
  minConn: number;
  maxConn: number;
  maxWait: number;
};

export type BusinessDataSourceTestResult = {
  success: boolean;
  message: string;
  validationQuery: string;
  elapsedMs: number;
};

let passwordPublicKeyPromise: Promise<string> | null = null;

function base64ToArrayBuffer(base64: string): ArrayBuffer {
  const binary = window.atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  const chunks: string[] = [];
  for (let i = 0; i < bytes.length; i += 0x8000) {
    chunks.push(String.fromCharCode.apply(null, Array.from(bytes.subarray(i, i + 0x8000))));
  }
  return window.btoa(chunks.join(''));
}

async function fetchPasswordPublicKey(): Promise<string> {
  if (!passwordPublicKeyPromise) {
    passwordPublicKeyPromise = request('/businessDataSources/password/publicKey', {
      method: 'get',
    }).then(result => result.data);
  }
  return passwordPublicKeyPromise;
}

async function encryptPassword(password: string): Promise<string> {
  if (!password || password.startsWith('rsa:')) {
    return password;
  }
  if (!window.crypto || !window.crypto.subtle) {
    throw new Error('The current browser does not support password encryption');
  }
  const publicKey = await fetchPasswordPublicKey();
  const cryptoKey = await window.crypto.subtle.importKey(
    'spki',
    base64ToArrayBuffer(publicKey),
    {
      name: 'RSA-OAEP',
      hash: 'SHA-256',
    },
    false,
    ['encrypt'],
  );
  const ciphertext = await window.crypto.subtle.encrypt(
    { name: 'RSA-OAEP' },
    cryptoKey,
    new TextEncoder().encode(password),
  );
  return `rsa:${arrayBufferToBase64(ciphertext)}`;
}

async function encryptBusinessDataSourcePassword(data: BusinessDataSourceForm): Promise<BusinessDataSourceForm> {
  return {
    ...data,
    password: await encryptPassword(data.password),
  };
}

export async function fetchBusinessDataSources(): Promise<BusinessDataSourceInfo[]> {
  const result = await request('/businessDataSources', {
    method: 'get',
  });
  return result.data || [];
}

export async function registerBusinessDataSource(data: BusinessDataSourceForm): Promise<string> {
  const encryptedData = await encryptBusinessDataSourcePassword(data);
  const result = await request('/businessDataSources', {
    method: 'post',
    data: encryptedData,
  });
  return result.data;
}

export async function testBusinessDataSource(data: BusinessDataSourceForm): Promise<BusinessDataSourceTestResult> {
  const encryptedData = await encryptBusinessDataSourcePassword(data);
  const result = await request('/businessDataSources/test', {
    method: 'post',
    data: encryptedData,
  });
  return result.data;
}

export async function unregisterBusinessDataSource(name: string): Promise<string> {
  const result = await request(`/businessDataSources/${encodeURIComponent(name)}`, {
    method: 'delete',
  });
  return result.data;
}

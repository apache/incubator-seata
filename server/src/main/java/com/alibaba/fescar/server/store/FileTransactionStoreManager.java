/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.alibaba.fescar.server.store;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.alibaba.fescar.common.thread.NamedThreadFactory;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.server.session.BranchSession;
import com.alibaba.fescar.server.session.GlobalSession;
import com.alibaba.fescar.server.session.SessionCondition;
import com.alibaba.fescar.server.session.SessionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type File transaction store manager.
 */
public class FileTransactionStoreManager implements TransactionStoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileTransactionStoreManager.class);
    private BlockingQueue<TransactionWriteFuture> transactionWriteFutureQueue
        = new LinkedBlockingQueue<TransactionWriteFuture>();
    private static final long MAX_ENQUEUE_MILLS = 5 * 1000;
    private static final int MAX_THREAD_WRITE = 1;
    private ExecutorService fileWriteExecutor;
    private volatile boolean stopping = false;
    private static final int MAX_SHUTDOWN_RETRY = 3;
    private static final int SHUTDOWN_CHECK_INTERNAL = 1 * 1000;
    private static final int MAX_WRITE_RETRY = 5;
    private static final String HIS_DATA_FILENAME_POSTFIX = ".1";
    private static final AtomicLong FILE_TRX_NUM = new AtomicLong(0);
    private static final AtomicLong FILE_FLUSH_NUM = new AtomicLong(0);
    private static final int MARK_SIZE = 4;
    private static final int MAX_POOL_TIME_MILLS = 2 * 1000;
    private static final int MAX_FLUSH_TIME_MILLS = 2 * 1000;
    private static final int MAX_FLUSH_NUM = 10;
    private static int PER_FILE_BLOCK_SIZE = 65535 * 8;
    private static long MAX_TRX_TIMEOUT_MILLS = 30 * 60 * 1000;
    private static volatile long trxStartTimeMills = System.currentTimeMillis();
    private static final boolean ENABLE_SCHEDULE_FLUSH = true;
    private File currDataFile;
    private RandomAccessFile currRaf;
    private FileChannel currFileChannel;
    private static long recoverCurrOffset = 0;
    private static long recoverHisOffset = 0;
    private SessionManager sessionManager;
    private String currFullFileName;
    private String hisFullFileName;

    /**
     * Instantiates a new File transaction store manager.
     *
     * @param fullFileName   the dir path
     * @param sessionManager the session manager
     */
    public FileTransactionStoreManager(String fullFileName, SessionManager sessionManager) throws IOException {
        initFile(fullFileName);
        fileWriteExecutor = new ThreadPoolExecutor(MAX_THREAD_WRITE, MAX_THREAD_WRITE,
            Integer.MAX_VALUE, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
            new NamedThreadFactory("fileTransactionStore", MAX_THREAD_WRITE, true));
        fileWriteExecutor.submit(new WriteDataFileRunnable());
        this.sessionManager = sessionManager;
    }

    private void initFile(String fullFileName) throws IOException {
        this.currFullFileName = fullFileName;
        this.hisFullFileName = fullFileName + HIS_DATA_FILENAME_POSTFIX;
        try {
            currDataFile = new File(currFullFileName);
            if (!currDataFile.exists()) {
                currDataFile.createNewFile();
                trxStartTimeMills = System.currentTimeMillis();
            } else {
                trxStartTimeMills = currDataFile.lastModified();
            }
            currRaf = new RandomAccessFile(currDataFile, "rw");
            currRaf.seek(currDataFile.length());
            currFileChannel = currRaf.getChannel();
        } catch (IOException exx) {
            LOGGER.error("init file error," + exx.getMessage());
            throw exx;
        }
    }

    @Override
    public boolean writeSession(LogOperation logOperation, SessionStorable session) {
        TransactionWriteFuture writeFutureRequest = new TransactionWriteFuture(session, logOperation);
        try {
            if (transactionWriteFutureQueue.offer(writeFutureRequest, MAX_ENQUEUE_MILLS, TimeUnit.MILLISECONDS)) {
                return writeFutureRequest.get();
            }
        } catch (InterruptedException exx) {
            LOGGER.error("write data file error," + exx.getMessage());
        }
        return false;
    }

    @Override
    public void shutdown() {
        if (null != fileWriteExecutor) {
            fileWriteExecutor.shutdown();
            stopping = true;
            int retry = 0;
            while (!fileWriteExecutor.isTerminated() && retry < MAX_SHUTDOWN_RETRY) {
                ++retry;
                try {
                    Thread.sleep(SHUTDOWN_CHECK_INTERNAL);
                } catch (InterruptedException exx) {}
            }
            if (retry >= MAX_SHUTDOWN_RETRY) {
                fileWriteExecutor.shutdownNow();
            }
        }
    }

    @Override
    public List<TransactionWriteStore> readWriteStoreFromFile(int readSize, boolean isHistory) {
        File file = null;
        long currentOffset = 0;
        if (isHistory) {
            file = new File(hisFullFileName);
            currentOffset = recoverHisOffset;
        } else {
            file = new File(currFullFileName);
            currentOffset = recoverCurrOffset;
        }
        if (file.exists()) {
            return parseDataFile(file, readSize, currentOffset);
        }
        return null;
    }

    @Override
    public boolean hasRemaining(boolean isHistory) {
        File file = null;
        RandomAccessFile raf = null;
        long currentOffset = 0;
        if (isHistory) {
            file = new File(hisFullFileName);
            currentOffset = recoverHisOffset;
        } else {
            file = new File(currFullFileName);
            currentOffset = recoverCurrOffset;
        }
        try {
            raf = new RandomAccessFile(file, "r");
            return currentOffset < raf.length();

        } catch (IOException exx) {
        } finally {
            closeFile(raf, null);
        }
        return false;
    }

    private List<TransactionWriteStore> parseDataFile(File file, int readSize, long currentOffset) {
        List<TransactionWriteStore> transactionWriteStores = new ArrayList<>(readSize);
        RandomAccessFile raf = null;
        FileChannel fileChannel = null;
        try {
            raf = new RandomAccessFile(file, "r");
            raf.seek(currentOffset);
            fileChannel = raf.getChannel();
            fileChannel.position(currentOffset);
            long size = raf.length();
            ByteBuffer buffSize = ByteBuffer.allocate(MARK_SIZE);
            while (fileChannel.position() < size) {
                try {
                    buffSize.clear();
                    int avilReadSize = fileChannel.read(buffSize);
                    if (avilReadSize != MARK_SIZE) { break; }
                    buffSize.flip();
                    int bodySize = buffSize.getInt();
                    byte[] byBody = new byte[bodySize];
                    ByteBuffer buffBody = ByteBuffer.wrap(byBody);
                    avilReadSize = fileChannel.read(buffBody);
                    if (avilReadSize != bodySize) { break; }
                    TransactionWriteStore writeStore = new TransactionWriteStore();
                    writeStore.decode(byBody);
                    transactionWriteStores.add(writeStore);
                    if (transactionWriteStores.size() == readSize) {
                        break;
                    }
                } catch (Exception ex) {
                    LOGGER.error("decode data file error:" + ex.getMessage());
                    break;
                }
            }
            return transactionWriteStores;
        } catch (IOException exx) {
            LOGGER.error("parse data file error:" + exx.getMessage() + ",file:" + file.getName());
            return null;
        } finally {
            try {
                if (null != fileChannel) {
                    if (isHisFile(file)) {
                        recoverHisOffset = fileChannel.position();
                    } else {
                        recoverCurrOffset = fileChannel.position();
                    }
                }
                closeFile(raf, fileChannel);
            } catch (IOException exx) {
                LOGGER.error("file close error," + exx.getMessage());
            }
        }

    }

    private boolean isHisFile(File file) {

        return file.getName().endsWith(HIS_DATA_FILENAME_POSTFIX);
    }

    private void closeFile(RandomAccessFile raf, FileChannel fileChannel) {
        try {
            if (null != fileChannel) {
                fileChannel.close();
                fileChannel = null;
            }
            if (null != raf) {
                raf.close();
                raf = null;
            }
        } catch (IOException exx) {
            LOGGER.error("file close error," + exx.getMessage());
        }
    }

    /**
     * The type Write data file runnable.
     */
    class WriteDataFileRunnable implements Runnable {

        @Override
        public void run() {
            while (!stopping) {
                try {
                    TransactionWriteFuture transactionWriteFuture = transactionWriteFutureQueue.poll(MAX_POOL_TIME_MILLS,
                        TimeUnit.MILLISECONDS);
                    if (null == transactionWriteFuture) {
                        flushOnCondition();
                        continue;
                    }
                    if (transactionWriteFuture.isTimeout()) {
                        transactionWriteFuture.setResult(Boolean.FALSE);
                        continue;
                    }
                    if (writeDataFile(transactionWriteFuture.getWriteStore().encode())) {
                        transactionWriteFuture.setResult(Boolean.TRUE);
                        FILE_TRX_NUM.incrementAndGet();
                        flushOnCondition();
                    } else {
                        transactionWriteFuture.setResult(Boolean.FALSE);
                    }
                    if (FILE_TRX_NUM.get() % PER_FILE_BLOCK_SIZE == 0
                        && (System.currentTimeMillis() - trxStartTimeMills) > MAX_TRX_TIMEOUT_MILLS) {
                        saveHistory();
                    }
                } catch (InterruptedException exx) {
                    stopping = true;
                } catch (Exception exx) {
                    LOGGER.error(exx.getMessage());
                }
            }

        }

        private boolean writeDataFile(byte[] bs) {
            int retry = 0;
            byte[] byWrite = new byte[bs.length + 4];
            ByteBuffer byteBuffer = ByteBuffer.wrap(byWrite);
            byteBuffer.putInt(bs.length);
            byteBuffer.put(bs);
            for (; retry < MAX_WRITE_RETRY; retry++) {
                try {
                    byteBuffer.flip();
                    while (byteBuffer.hasRemaining()) {
                        currFileChannel.write(byteBuffer);
                    }
                    return true;
                } catch (IOException exx) {
                    LOGGER.error("write data file error:" + exx.getMessage());
                }
            }
            LOGGER.error("write dataFile failed,retry more than :" + MAX_WRITE_RETRY);
            return false;
        }

        private void flushOnCondition() {
            if (!ENABLE_SCHEDULE_FLUSH) { return; }
            long diff = FILE_TRX_NUM.get() - FILE_FLUSH_NUM.get();
            if (diff == 0) { return; }
            if (diff % MAX_FLUSH_NUM == 0
                || System.currentTimeMillis() - currDataFile.lastModified() > MAX_FLUSH_TIME_MILLS) {
                try {
                    currFileChannel.force(false);
                } catch (IOException exx) {
                    LOGGER.error("flush error:" + exx.getMessage());
                }
                FILE_FLUSH_NUM.addAndGet(diff);
            }
        }

        private void saveHistory() throws IOException {
            try {
                List<GlobalSession> globalSessionsOverMaxTimeout = sessionManager.findGlobalSessions(
                    new SessionCondition(
                        GlobalStatus.Begin, MAX_TRX_TIMEOUT_MILLS));
                if (null != globalSessionsOverMaxTimeout) {
                    for (GlobalSession globalSession : globalSessionsOverMaxTimeout) {
                        TransactionWriteStore globalWriteStore = new TransactionWriteStore(globalSession,
                            LogOperation.GLOBAL_ADD);
                        writeDataFile(globalWriteStore.encode());
                        List<BranchSession> branchSessIonsOverMaXTimeout = globalSession.getSortedBranches();
                        if (null != branchSessIonsOverMaXTimeout) {
                            for (BranchSession branchSession : branchSessIonsOverMaXTimeout) {
                                TransactionWriteStore branchWriteStore = new TransactionWriteStore(branchSession,
                                    LogOperation.BRANCH_ADD);
                                writeDataFile(branchWriteStore.encode());
                            }
                        }
                    }
                }
                currFileChannel.force(true);
                File hisDataFile = new File(hisFullFileName);
                if (hisDataFile.exists()) {
                    hisDataFile.delete();
                }
                closeFile(currRaf, currFileChannel);
                currDataFile.renameTo(new File(hisFullFileName));
            } catch (IOException exx) {
                LOGGER.error("save history data file error," + exx.getMessage());
            } finally {
                initFile(currFullFileName);
            }

        }
    }
}

package com.alibaba.fescar.config.zookeeper;

import org.I0Itec.zkclient.IZkDataListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author crazier.huang
 * @date 2019/2/18
 */
public class ZKConfigListener implements IZkDataListener {
    public static Logger logger = LoggerFactory.getLogger("ZKConfigListener");

    @Override
    public void handleDataChange(String dataPath, Object data) throws Exception {
        logger.info("ZKConfigListener handleDataChange dataPath {} info {}",dataPath,data.toString());
    }

    @Override
    public void handleDataDeleted(String dataPath) throws Exception {
        logger.info("ZKConfigListener handleDataDeleted  dataPath {}",dataPath);
    }
}

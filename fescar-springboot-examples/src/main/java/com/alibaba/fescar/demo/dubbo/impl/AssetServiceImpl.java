package com.alibaba.fescar.demo.dubbo.impl;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.demo.dubbo.AssetService;
import com.alibaba.fescar.demo.sys.domain.Asset;
import com.alibaba.fescar.demo.sys.repository.AssetRepository;

/**
 * @Description
 * @author 张国豪
 */
@Service(interfaceClass = AssetService.class, timeout = 10000)
@Component
public class AssetServiceImpl implements AssetService {

	public static final Logger LOGGER = LoggerFactory.getLogger(AssetService.class);

	public static final String ASSET_ID = "DF001";

	@Autowired
	private AssetRepository assetRepository;

	@Override
	public int increase() {
		LOGGER.info("Asset Service Begin ... xid: " + RootContext.getXID() + "\n");

		Asset asset = assetRepository.findById(ASSET_ID).get();
		asset.setAmount(asset.getAmount().add(new BigDecimal("1")));
		assetRepository.save(asset);
		throw new RuntimeException("test exception for fescar");
	}
}

package com.alibaba.fescar.demo.service.impl;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.demo.dubbo.AssetService;
import com.alibaba.fescar.demo.service.AssignService;
import com.alibaba.fescar.demo.sys.domain.AssetAssign;
import com.alibaba.fescar.demo.sys.repository.AssignRepository;
import com.alibaba.fescar.spring.annotation.GlobalTransactional;

@Service
public class AssignServiceImpl implements AssignService {
	private static final Logger LOGGER = LoggerFactory.getLogger(AssignServiceImpl.class);

	@Autowired
	private AssignRepository assignRepository;

	@Reference(check = false)
	private AssetService AssetService;

	@Override
	@Transactional
	@GlobalTransactional
	public AssetAssign increaseAmount(String id) {
		LOGGER.info("Assign Service Begin ... xid: " + RootContext.getXID() + "\n");
		AssetAssign assetAssign = assignRepository.findById(id).get();
		assetAssign.setStatus("2");
		assignRepository.save(assetAssign);

		// remote call asset service
		AssetService.increase();
		return assetAssign;
	}

}

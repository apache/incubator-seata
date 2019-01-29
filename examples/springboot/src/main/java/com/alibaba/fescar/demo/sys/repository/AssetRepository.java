package com.alibaba.fescar.demo.sys.repository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import com.alibaba.fescar.demo.sys.domain.Asset;

@Repository
public interface AssetRepository extends PagingAndSortingRepository<Asset, String>, JpaSpecificationExecutor<Asset> {
}
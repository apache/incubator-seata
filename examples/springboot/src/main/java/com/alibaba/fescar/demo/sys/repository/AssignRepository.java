package com.alibaba.fescar.demo.sys.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.alibaba.fescar.demo.sys.domain.AssetAssign;


@Repository
public interface AssignRepository
		extends PagingAndSortingRepository<AssetAssign, String>, JpaSpecificationExecutor<AssetAssign> {

	@Query(value = "update t_asset_assign set id= :newId, status = '05' where id = :id ", nativeQuery = true)
	@Modifying
	void updateNewId(@Param("id") String id, @Param("newId") String newId);

	List<AssetAssign> findByAssetIdInAndStatusNot(List<String> assetIds, String status);
}
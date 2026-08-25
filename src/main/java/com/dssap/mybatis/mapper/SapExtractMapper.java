package com.dssap.mybatis.mapper;

import com.dssap.batch.entity.SapExtractEntity;
import com.dssap.mybatis.ExtractSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SapExtractMapper {

    int insert(SapExtractEntity entity);

    int insertAll(@Param("list") List<SapExtractEntity> list);

    long countAll();

    List<SapExtractEntity> findRecent(@Param("limit") int limit);

    List<SapExtractEntity> search(ExtractSearchCondition condition);

    SapExtractEntity findById(@Param("id") Long id);
}

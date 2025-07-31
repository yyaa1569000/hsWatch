package com.example.hs.repository;

import com.example.hs.entity.HsItem;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HsRepository extends JpaRepository<HsItem, Long> {
    List<HsItem> findByName(String name);

    List<HsItem> findByNewProductTrue();

    // 模糊查詢
    @Query("SELECT h FROM HsItem h " +
            "WHERE h.price BETWEEN :min AND :max " +
            "AND (" +
            "LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(h.series) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(h.title) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
            ")")
    List<HsItem> searchWithKeyword(@Param("min") Long min,
            @Param("max") Long max,
            @Param("keyword") String keyword);

    // 僅查價格範圍
    @Query("SELECT h FROM HsItem h WHERE h.price BETWEEN :min AND :max")
    List<HsItem> searchWithoutKeyword(@Param("min") Long min,
            @Param("max") Long max);

}

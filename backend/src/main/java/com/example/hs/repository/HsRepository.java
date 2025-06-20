package com.example.hs.repository;

import com.example.hs.entity.HsItem;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HsRepository extends JpaRepository<HsItem, Long> {
    List<HsItem> findByName(String name);
}

package com.security.securitymall.repository;

import com.security.securitymall.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByNameContainingIgnoreCase(String name);
    List<Item> findByCategory(String category);

    // 중복 없는 카테고리 목록만 가져오기
    @Query("SELECT DISTINCT i.category FROM Item i WHERE i.category IS NOT NULL AND i.category <> ''")
    List<String> findDistinctCategories();
}
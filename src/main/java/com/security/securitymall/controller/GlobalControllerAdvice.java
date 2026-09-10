package com.security.securitymall.controller;

import com.security.securitymall.repository.ItemRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final ItemRepository itemRepository;

    public GlobalControllerAdvice(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // 모든 HTML 뷰에 'categories' 변수를 자동으로 주입합니다.
    @ModelAttribute("categories")
    public List<String> populateCategories() {
        return itemRepository.findDistinctCategories();
    }
}
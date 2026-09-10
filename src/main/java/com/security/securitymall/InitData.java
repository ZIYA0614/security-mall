package com.security.securitymall;

import com.security.securitymall.domain.*;
import com.security.securitymall.repository.*;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class InitData {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public InitData(UserRepository userRepository, ItemRepository itemRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("1234");
            admin.setName("관리자");
            admin.setRole("ADMIN");
            userRepository.save(admin);

            User user = new User();
            user.setUsername("user1");
            user.setPassword("1234");
            user.setName("일반유저");
            user.setRole("USER");
            userRepository.save(user);
        }

        if (itemRepository.count() == 0) {
            createItem("보안 전용 노트북 Pro", 1850000, 10, "노트북", "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500");
            createItem("고성능 해킹분석 PC", 2400000, 5, "컴퓨터", "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=500");
            createItem("울트라 슬림 노트북", 1350000, 12, "노트북", "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=500");
            createItem("보안 관제용 듀얼 PC", 2100000, 7, "컴퓨터", "https://images.unsplash.com/photo-1547082299-de196ea013d6?w=500");
        }
    }

    private void createItem(String name, int price, int stock, String category, String imageUrl) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(price);
        item.setStock(stock);
        item.setCategory(category);
        item.setImageUrl(imageUrl);
        itemRepository.save(item);
    }
}
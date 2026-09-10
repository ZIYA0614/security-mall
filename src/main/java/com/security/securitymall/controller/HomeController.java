package com.security.securitymall.controller;

import com.security.securitymall.domain.*;
import com.security.securitymall.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Controller
public class HomeController {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final BoardRepository boardRepository;
    private final OrderRepository orderRepository;

    public HomeController(UserRepository userRepository, ItemRepository itemRepository,
                          BoardRepository boardRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.boardRepository = boardRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String category,
                       HttpSession session, Model model) {
        List<Item> items;
        if (keyword != null && !keyword.trim().isEmpty()) {
            items = itemRepository.findByNameContainingIgnoreCase(keyword.trim());
        } else if (category != null && !category.trim().isEmpty()) {
            items = itemRepository.findByCategory(category.trim());
        } else {
            items = itemRepository.findAll();
        }

        model.addAttribute("items", items);
        model.addAttribute("categories", itemRepository.findDistinctCategories());
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        return "index";
    }

    @GetMapping("/signup")
    public String signupForm() { return "signup"; }

    @PostMapping("/signup")
    public String signup(User user) {
        user.setRole("USER");
        userRepository.save(user);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, HttpSession session) {
        User user = userRepository.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            session.setAttribute("loginUser", user);
            return "redirect:/";
        }
        return "redirect:/login?error";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @GetMapping("/item/{id}")
    public String itemDetail(@PathVariable Long id, Model model, HttpSession session) {
        Item item = itemRepository.findById(id).orElse(null);
        model.addAttribute("item", item);
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        return "item_detail";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long itemId, @RequestParam(defaultValue = "1") int quantity, HttpSession session) {
        if (session.getAttribute("loginUser") == null) return "redirect:/login";

        List<Map<String, Object>> cart = (List<Map<String, Object>>) session.getAttribute("cart");
        if (cart == null) cart = new ArrayList<>();

        Item item = itemRepository.findById(itemId).orElse(null);
        if (item != null) {
            Map<String, Object> cartItem = new HashMap<>();
            cartItem.put("item", item);
            cartItem.put("quantity", quantity);
            cart.add(cartItem);
            session.setAttribute("cart", cart);
        }
        return "redirect:/cart";
    }

    @GetMapping("/cart")
    public String cartView(HttpSession session, Model model) {
        List<Map<String, Object>> cart = (List<Map<String, Object>>) session.getAttribute("cart");
        int totalPrice = 0;
        if (cart != null) {
            for (Map<String, Object> c : cart) {
                Item item = (Item) c.get("item");
                int qty = (int) c.get("quantity");
                totalPrice += item.getPrice() * qty;
            }
        }
        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        return "cart";
    }

    @GetMapping("/order/checkout")
    public String checkout(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null) return "redirect:/login";

        List<Map<String, Object>> cart = (List<Map<String, Object>>) session.getAttribute("cart");
        if (cart == null || cart.isEmpty()) return "redirect:/cart";

        int totalPrice = 0;
        for (Map<String, Object> c : cart) {
            Item item = (Item) c.get("item");
            int qty = (int) c.get("quantity");
            totalPrice += item.getPrice() * qty;
        }

        model.addAttribute("cart", cart);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("loginUser", loginUser);
        return "order_checkout";
    }

    // 1단계: 카카오페이 결제 준비 요청 (Ready API)
    @PostMapping("/order/kakaopay/ready")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> kakaopayReady(@RequestBody Map<String, Object> params, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        List<Map<String, Object>> cart = (List<Map<String, Object>>) session.getAttribute("cart");

        String merchantUid = "ORD_" + System.currentTimeMillis();
        int totalPrice = Integer.parseInt(params.get("totalPrice").toString());

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY DEV_SECRET_KEY");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("cid", "TC0ONETIME");
        body.put("partner_order_id", merchantUid);
        body.put("partner_user_id", loginUser.getUsername());
        body.put("item_name", "보안 실습 쇼핑몰 상품 결제");
        body.put("quantity", 1);
        body.put("total_amount", totalPrice);
        body.put("tax_free_amount", 0);
        body.put("approval_url", "http://localhost:8080/order/kakaopay/approve");
        body.put("cancel_url", "http://localhost:8080/order/kakaopay/cancel");
        body.put("fail_url", "http://localhost:8080/order/kakaopay/fail");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity("https://open-api.kakaopay.com/online/v1/payment/ready", request, Map.class);
            Map<String, Object> resBody = response.getBody();

            session.setAttribute("tid", resBody.get("tid"));
            session.setAttribute("merchantUid", merchantUid);
            session.setAttribute("buyerName", params.get("buyerName"));
            session.setAttribute("buyerPhone", params.get("buyerPhone"));
            session.setAttribute("buyerAddr", params.get("buyerAddr"));
            session.setAttribute("totalPrice", totalPrice);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("next_redirect_pc_url", resBody.get("next_redirect_pc_url"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(result);
        }
    }

    // 2단계: 카카오페이 결제 승인 처리 (Approve API)
    @GetMapping("/order/kakaopay/approve")
    public String kakaopayApprove(@RequestParam("pg_token") String pgToken, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        String tid = (String) session.getAttribute("tid");
        String merchantUid = (String) session.getAttribute("merchantUid");

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY DEV_SECRET_KEY");
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("cid", "TC0ONETIME");
        body.put("tid", tid);
        body.put("partner_order_id", merchantUid);
        body.put("partner_user_id", loginUser.getUsername());
        body.put("pg_token", pgToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        try {
            restTemplate.postForEntity("https://open-api.kakaopay.com/online/v1/payment/approve", request, Map.class);

            // DB 주문 생성 및 결제 완료 처리
            Order order = new Order();
            order.setMerchantUid(merchantUid);
            order.setImpUid(tid);
            order.setBuyerName((String) session.getAttribute("buyerName"));
            order.setBuyerPhone((String) session.getAttribute("buyerPhone"));
            order.setBuyerAddr((String) session.getAttribute("buyerAddr"));
            order.setTotalPrice((int) session.getAttribute("totalPrice"));
            order.setStatus(OrderStatus.PAID);
            order.setUser(loginUser);

            List<Map<String, Object>> cart = (List<Map<String, Object>>) session.getAttribute("cart");
            if (cart != null) {
                for (Map<String, Object> c : cart) {
                    Item item = (Item) c.get("item");
                    int qty = (int) c.get("quantity");
                    Item dbItem = itemRepository.findById(item.getId()).orElse(null);
                    if (dbItem != null && dbItem.getStock() >= qty) {
                        dbItem.setStock(dbItem.getStock() - qty);
                        itemRepository.save(dbItem);
                    }
                    order.addOrderItem(new OrderItem(dbItem, item.getPrice(), qty));
                }
            }

            Order savedOrder = orderRepository.save(order);
            session.removeAttribute("cart");
            return "redirect:/order/complete/" + savedOrder.getId();
        } catch (Exception e) {
            return "redirect:/order/kakaopay/fail";
        }
    }

    @GetMapping("/order/kakaopay/cancel")
    public String kakaopayCancel(HttpSession session) {
        saveFailedOrder(session, OrderStatus.CANCELLED);
        return "redirect:/cart?error=cancel";
    }

    @GetMapping("/order/kakaopay/fail")
    public String kakaopayFail(HttpSession session) {
        saveFailedOrder(session, OrderStatus.FAILED);
        return "redirect:/cart?error=fail";
    }

    private void saveFailedOrder(HttpSession session, OrderStatus status) {
        String merchantUid = (String) session.getAttribute("merchantUid");
        User loginUser = (User) session.getAttribute("loginUser");
        if (merchantUid != null && loginUser != null) {
            Order order = new Order();
            order.setMerchantUid(merchantUid);
            order.setStatus(status);
            order.setUser(loginUser);
            orderRepository.save(order);
        }
    }

    @GetMapping("/order/complete/{id}")
    public String orderComplete(@PathVariable Long id, Model model, HttpSession session) {
        Order order = orderRepository.findById(id).orElse(null);
        model.addAttribute("order", order);
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        return "order_complete";
    }

    @GetMapping("/board")
    public String board(Model model, HttpSession session) {
        model.addAttribute("boards", boardRepository.findAll());
        model.addAttribute("loginUser", session.getAttribute("loginUser"));
        return "board";
    }

    @GetMapping("/board/{id}")
    public String boardDetail(@PathVariable Long id, Model model, HttpSession session) {
        Board board = boardRepository.findById(id).orElse(null);
        if (board != null) {
            board.setViews(board.getViews() + 1);
            boardRepository.save(board);
            model.addAttribute("board", board);
            model.addAttribute("loginUser", session.getAttribute("loginUser"));
            return "board_detail";
        }
        return "redirect:/board";
    }

    @PostMapping("/board/write")
    public String writeBoard(Board board, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            board.setWriter(loginUser.getName());
            board.setWriterUsername(loginUser.getUsername());
            boardRepository.save(board);
        }
        return "redirect:/board";
    }

    @GetMapping("/board/edit/{id}")
    public String editBoardForm(@PathVariable Long id, Model model, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        Board board = boardRepository.findById(id).orElse(null);
        if (board != null && loginUser != null && loginUser.getUsername().equals(board.getWriterUsername())) {
            model.addAttribute("board", board);
            return "board_edit";
        }
        return "redirect:/board";
    }

    @PostMapping("/board/edit")
    public String editBoard(Board board, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        Board existingBoard = boardRepository.findById(board.getId()).orElse(null);
        if (existingBoard != null && loginUser != null && loginUser.getUsername().equals(existingBoard.getWriterUsername())) {
            existingBoard.setTitle(board.getTitle());
            existingBoard.setContent(board.getContent());
            boardRepository.save(existingBoard);
        }
        return "redirect:/board";
    }

    @PostMapping("/board/delete/{id}")
    public String deleteBoard(@PathVariable Long id, HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        Board board = boardRepository.findById(id).orElse(null);
        if (board != null && loginUser != null) {
            if ("ADMIN".equals(loginUser.getRole()) || loginUser.getUsername().equals(board.getWriterUsername())) {
                boardRepository.deleteById(id);
            }
        }
        return "redirect:/board";
    }

    @GetMapping("/admin")
    public String admin(HttpSession session, Model model) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser == null || !"ADMIN".equals(loginUser.getRole())) return "redirect:/";
        model.addAttribute("users", userRepository.findAll());
        model.addAttribute("items", itemRepository.findAll());
        return "admin";
    }

    @GetMapping("/admin/item/add")
    public String addItemForm(Model model) {
        model.addAttribute("categories", itemRepository.findDistinctCategories());
        return "item_add";
    }

    @PostMapping("/admin/item/add")
    public String addItem(@ModelAttribute Item item, @RequestParam("imageFile") MultipartFile file) throws IOException {
        if (file != null && !file.isEmpty()) {
            String base64 = "data:" + file.getContentType() + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
            item.setImageUrl(base64);
        }
        itemRepository.save(item);
        return "redirect:/admin";
    }

    @GetMapping("/admin/item/edit/{id}")
    public String editItemForm(@PathVariable Long id, Model model) {
        Item item = itemRepository.findById(id).orElse(null);
        model.addAttribute("item", item);
        model.addAttribute("categories", itemRepository.findDistinctCategories());
        return "item_edit";
    }

    @PostMapping("/admin/item/edit")
    public String editItem(@ModelAttribute Item item, @RequestParam("imageFile") MultipartFile file) throws IOException {
        Item existingItem = itemRepository.findById(item.getId()).orElse(null);
        if (existingItem != null) {
            existingItem.setName(item.getName());
            existingItem.setPrice(item.getPrice());
            existingItem.setStock(item.getStock());
            existingItem.setCategory(item.getCategory());
            existingItem.setDescription(item.getDescription());
            if (file != null && !file.isEmpty()) {
                String base64 = "data:" + file.getContentType() + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
                existingItem.setImageUrl(base64);
            }
            itemRepository.save(existingItem);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/item/delete/{id}")
    public String deleteItem(@PathVariable Long id) {
        itemRepository.deleteById(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/user/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/admin";
    }
}
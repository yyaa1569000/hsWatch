package com.example.hs.controller;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.hs.entity.HsItem;
import com.example.hs.repository.HsRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// @CrossOrigin(origins = "http://localhost:5173")
@CrossOrigin(origins = "https://yyaa1569000.github.io")
@RequestMapping("/api")
@Slf4j
@Controller

public class HsController {

    @Autowired
    private HsRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dblc0p4pd",
            "api_key", "546934591939223",
            "api_secret", "3WInaPMT57wr1QUeh55rtx2S4g4"));

    // 左側列表
    @ResponseBody
    @GetMapping("/hs_leftList")
    public List<Map<String, Object>> getleftList() {
        List<HsItem> items = repository.findAll();
        Map<String, Map<String, Object>> groupedItems = new LinkedHashMap<>();

        for (HsItem item : items) {
            // 確保 title 不重複
            groupedItems.computeIfAbsent(item.getTitle(), k -> {
                Map<String, Object> map = new HashMap<>();
                map.put("title", k);
                map.put("names", new LinkedHashMap<>()); // 用 Map 去重複 name
                return map;
            });

            // 取得 name，確保它只存一次
            Map<String, Object> namesMap = (Map<String, Object>) groupedItems.get(item.getTitle()).get("names");
            namesMap.computeIfAbsent(item.getName(), k -> {
                Map<String, Object> nameData = new HashMap<>();
                nameData.put("name", k);
                nameData.put("item", new ArrayList<>()); // `item` 內存放 series & id
                return nameData;
            });

            // 新增 series 到對應的 name
            Map<String, Object> nameEntry = (Map<String, Object>) namesMap.get(item.getName());
            ((List<Map<String, Object>>) nameEntry.get("item"))
                    .add(Map.of("series", item.getSeries(), "id", item.getId()));
        }

        // 轉換格式，使前端更容易讀取
        List<Map<String, Object>> formattedData = new ArrayList<>();
        groupedItems.forEach((title, data) -> {
            Map<String, Object> formattedEntry = new HashMap<>();
            formattedEntry.put("title", title);
            formattedEntry.put("names", new ArrayList<>(((Map<String, Object>) data.get("names")).values()));
            formattedData.add(formattedEntry);
        });
        System.err.println(formattedData);
        return formattedData;
    }

    @ResponseBody
    @GetMapping("/hs_watchItem")
    public List<HsItem> getWatchItem(@RequestParam String name) {
        List<HsItem> items;

        if ("new".equalsIgnoreCase(name)) {
            items = repository.findByNewProductTrue(); // 撈出 is_new_product = true 的資料
        } else {
            items = repository.findByName(name); // 撈出符合 name 的資料
        }

        System.err.println(items);
        return items;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadItem(
            @RequestParam("image") MultipartFile file,
            @RequestParam Map<String, String> itemData) {
        try {
            // 上傳圖片到 Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String imageUrl = (String) uploadResult.get("secure_url");

            // 建立資料
            HsItem item = new HsItem();
            item.setTitle(itemData.getOrDefault("title", ""));
            item.setName(itemData.getOrDefault("name", ""));
            item.setSeries(itemData.getOrDefault("series", ""));
            item.setPrice(new BigDecimal(itemData.getOrDefault("price", "0")));
            item.setPhone(itemData.get("phone"));
            item.setContent(itemData.get("content"));
            item.setRemark(itemData.get("remark"));
            item.setImage(imageUrl); // ✅ 存 Cloudinary 的 URL

            // 資料存入 DB
            HsItem saved = repository.save(item);
            return ResponseEntity.ok("✅ 成功儲存：" + imageUrl + "，ID：" + saved.getId());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("❌ 上傳失敗：" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @ResponseBody
    @GetMapping("/hs_search")
    public ResponseEntity<?> searchItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long min,
            @RequestParam(required = false) Long max) {

        // 所有參數都是空的話就直接回傳 nodata
        if ((keyword == null || keyword.trim().isEmpty()) && min == null && max == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "nodata");
            return ResponseEntity.ok(response);
        }
        Long minPrice = (min != null) ? min : 0L;
        Long maxPrice = (max != null) ? max : 99999999L;

        List<HsItem> result;

        if (keyword != null && !keyword.trim().isEmpty()) {
            result = repository.searchWithKeyword(minPrice, maxPrice, keyword.trim());
        } else {
            result = repository.searchWithoutKeyword(minPrice, maxPrice);
        }
        if (result.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "nodata");
            return ResponseEntity.ok(response); // 保持 200 OK，回傳 JSON
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/getTitles")
    @ResponseBody
    public Map<String, List<String>> getTitlesAndNames() {
        String sqlTitles = "SELECT DISTINCT title FROM hs WHERE title IS NOT NULL";
        String sqlNames = "SELECT DISTINCT name FROM hs WHERE name IS NOT NULL";

        List<String> titles = jdbcTemplate.queryForList(sqlTitles, String.class);
        List<String> names = jdbcTemplate.queryForList(sqlNames, String.class);

        return Map.of("titles", titles, "names", names);
    }

    @GetMapping("/addWatch")
    public String showAdminPage(HttpSession session) {
        if (!"yes".equals(session.getAttribute("authorized"))) {
            return "auth"; // ✅ 用 redirect 而不是 return "auth"
        }
        return "addWatch"; // 對應 templates/addWatch.html
    }

    @PostMapping("/login")
    public String login(@RequestParam String password, HttpSession session) {
        if ("123".equals(password)) {
            session.setAttribute("authorized", "yes");
            return "addWatch"; // 這樣就會渲染 templates/addWatch.html
        }
        return "auth"; // 密碼錯誤回登入頁
    }

    @Configuration
    public class SessionConfig {

        @Bean
        public ServletListenerRegistrationBean<HttpSessionListener> sessionListener() {
            return new ServletListenerRegistrationBean<>(new HttpSessionListener() {
                @Override
                public void sessionCreated(HttpSessionEvent se) {
                    se.getSession().setMaxInactiveInterval(1);
                }
            });
        }
    }

}

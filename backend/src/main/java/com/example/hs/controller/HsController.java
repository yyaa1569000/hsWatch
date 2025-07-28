package com.example.hs.controller;

import com.example.hs.entity.HsItem;
import com.example.hs.repository.HsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
@Slf4j

public class HsController {

    @Autowired
    private HsRepository repository;

    // 左側列表
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

        System.err.println(file);
        try {
            // 定義儲存路徑
            String uploadDir = new File("HS/assets").getAbsolutePath() + File.separator;
            File uploadFolder = new File(uploadDir);
            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }

            // 儲存圖片
            String filePath = uploadDir + file.getOriginalFilename();
            file.transferTo(new File(filePath));

            // 建立 HsItem 物件
            HsItem item = new HsItem();
            item.setName(itemData.get("name"));
            item.setSeries(itemData.get("series"));
            item.setPrice(new BigDecimal(itemData.get("price")));
            item.setPhone(itemData.get("phone"));
            item.setContent(itemData.get("content"));
            item.setRemark(itemData.get("remark"));
            item.setImage(filePath); // 儲存圖片路徑

            HsItem saved = repository.save(item);
            return ResponseEntity.ok("成功儲存：" + filePath + "，ID：" + saved.getId());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("圖片上傳失敗：" + e.getMessage());
        }
    }

}

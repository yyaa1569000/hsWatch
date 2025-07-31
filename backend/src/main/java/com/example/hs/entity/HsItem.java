package com.example.hs.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "hs")
public class HsItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String name;
    private String series;
    private String content;
    private String image;
    private String phone;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    private String remark;

    @Column(name = "is_new_product", nullable = false)
    private Boolean newProduct = false;
}

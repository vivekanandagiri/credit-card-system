package com.example.util;

import org.springframework.stereotype.Component;

@Component
public class ProductCodeGenerator {

    public String generateBaseCode(String productName) {

        return productName
                .trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", "")
                .replaceAll("\\s+", "-");
    }
}
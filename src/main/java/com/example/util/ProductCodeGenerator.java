package com.example.util;

import org.springframework.stereotype.Component;

/**
 * Utility component for generating standardized product codes.
 *
 * <p>This class converts a given product name into a normalized,
 * uppercase, hyphen-separated code suitable for identifiers.
 *
 * <p>Transformation steps:
 * <ul>
 *     <li>Trim leading and trailing spaces</li>
 *     <li>Convert to uppercase</li>
 *     <li>Remove special characters (retain only A-Z, 0-9, and spaces)</li>
 *     <li>Replace spaces with hyphens (-)</li>
 * </ul>
 *
 * <p><b>Example:</b>
 * <pre>
 * Input  : "Platinum Card++"
 * Output : "PLATINUM-CARD"
 * </pre>
 *
 * <p>This code can be used for:
 * <ul>
 *     <li>Product identifiers</li>
 *     <li>URL slugs</li>
 *     <li>Reference codes</li>
 * </ul>
 */
@Component
public class ProductCodeGenerator {

    /**
     * Generates a base product code from a given product name.
     *
     * @param productName the name of the product
     * @return normalized product code
     * @throws IllegalArgumentException if productName is null or blank
     */
    public String generateBaseCode(String productName) {

        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }

        return productName
                .trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9 ]", "")   // remove special chars
                .replaceAll("\\s+", "-");       // replace spaces with hyphen
    }
}
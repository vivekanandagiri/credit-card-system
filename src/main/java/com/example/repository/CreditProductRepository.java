package com.example.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.entity.CreditProduct;
import com.example.enums.ProductStatus;

public interface CreditProductRepository extends JpaRepository<CreditProduct, Long>{

    boolean existsByProductCode(String productCode);

    List<CreditProduct> findAllByStatus(ProductStatus status);

    boolean existsByCreditProductId(Long creditProductId);   // ✅ FIXED
}
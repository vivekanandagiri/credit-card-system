package com.example.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.CreditCardProduct;
import com.example.enums.ProductStatus;

public interface CreditCardProductRepository extends JpaRepository<CreditCardProduct, UUID> {
//	List<CreditCardProduct> findAllByCreditProductCreditProductId(Long creditProductId);

    List<CreditCardProduct> findAllByStatus(ProductStatus status);
    
//    boolean existsByProductNameAndCreditProductCreditProductId(
//            String productName,
//            Long creditProductId
//    );
//    
//    List<CreditCardProduct> findAllByCreditProductCreditProductIdAndStatus(
//            Long creditProductId,
//            ProductStatus status
//    );

}

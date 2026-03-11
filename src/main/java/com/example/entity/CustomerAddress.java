package com.example.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "customer_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAddress extends BaseEntity {

    @Id
    @GeneratedValue
    @Column(name = "address_id")
    private UUID addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(name = "line1", nullable = false)
    private String line1;

    private String city;
    private String state;

    @Column(name = "postal_code")
    private String postalCode;

    private String country;
}
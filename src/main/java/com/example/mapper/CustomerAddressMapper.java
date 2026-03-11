package com.example.mapper;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.entity.Customer;
import com.example.entity.CustomerAddress;
import org.springframework.stereotype.Component;

@Component
public class CustomerAddressMapper {

    // Request -> Entity
    public CustomerAddress toEntity(AddressCreateRequest request, Customer customer) {

        CustomerAddress address = new CustomerAddress();

        address.setCustomer(customer);
        address.setLine1(request.getLine1());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPostalCode(request.getPostalCode());
        address.setCountry(request.getCountry());

        return address;
    }

    // Entity -> Response
    public AddressResponse toResponse(CustomerAddress address) {

        return new AddressResponse(
                address.getAddressId(),
                address.getLine1(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry()
        );
    }
}
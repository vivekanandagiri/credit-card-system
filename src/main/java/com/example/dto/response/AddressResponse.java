package com.example.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Schema(description = "Customer address details")
public class AddressResponse {

    @Schema(
            description = "Unique identifier of the address",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private UUID addressId;

    @Schema(
            description = "Primary address line",
            example = "Queens Road "
    )
    private String line1;

    @Schema(
            description = "City name",
            example = "Bengaluru"
    )
    private String city;

    @Schema(
            description = "State or province",
            example = " Karnataka"
    )
    private String state;

    @Schema(
            description = "Postal or ZIP code",
            example = "784584"
    )
    private String postalCode;

    @Schema(
            description = "Country name",
            example = "India"
    )
    private String country;
}
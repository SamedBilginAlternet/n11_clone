package com.example.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * ES index document. Denormalized copy of a Product — source of truth is
 * product-service. We keep the same id so the frontend can link straight
 * back to /product/{slug}.
 *
 * Text fields use the built-in "turkish" analyzer so stems ("telefonlar"
 * ~ "telefon") and diacritics work naturally.
 */
@Document(indexName = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "turkish")
    private String name;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Text, analyzer = "turkish")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Double)
    private double price;

    @Field(type = FieldType.Double)
    private double discountedPrice;

    @Field(type = FieldType.Integer)
    private int discountPercentage;

    @Field(type = FieldType.Integer)
    private int stockQuantity;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    @Field(type = FieldType.Double)
    private double rating;

    @Field(type = FieldType.Integer)
    private int reviewCount;
}

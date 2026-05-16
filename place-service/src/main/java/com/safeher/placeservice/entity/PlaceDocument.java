package com.safeher.placeservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Document(indexName = "places")
@Setting(settingPath = "elasticsearch/place-settings.json")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceDocument {

    @Id
    private String id;  // UUID as string

    @Field(type = FieldType.Keyword)
    private String externalId;

    @Field(type = FieldType.Keyword)
    private String source;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "standard"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword),
            @InnerField(suffix = "search", type = FieldType.Text, analyzer = "search_analyzer")
        }
    )
    private String name;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String subCategory;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "standard"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String address;

    @Field(type = FieldType.Keyword)
    private String city;

    @Field(type = FieldType.Keyword)
    private String country;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Double)
    private BigDecimal safetyScore;

    @Field(type = FieldType.Integer)
    private int totalRatings;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Boolean)
    private boolean verified;

    @Field(type = FieldType.Keyword)
    private List<String> amenities;

    @Field(type = FieldType.Keyword)
    private String createdBy;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private OffsetDateTime createdAt;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String aiSummary;
}

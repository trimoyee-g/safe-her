package com.safeher.ratingservice.entity.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.Instant;
import java.util.List;

@Document(indexName = "ratings")
@Setting(settingPath = "elasticsearch/rating-settings.json")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RatingDocument {

    @Id
    private String id;            // MongoDB ObjectId

    @Field(type = FieldType.Keyword)
    private String placeId;

    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Integer)
    private int score;

    @MultiField(
        mainField = @Field(type = FieldType.Text, analyzer = "review_analyzer"),
        otherFields = {
            @InnerField(suffix = "keyword", type = FieldType.Keyword)
        }
    )
    private String title;

    @Field(type = FieldType.Text, analyzer = "review_analyzer")
    private String body;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Boolean)
    private boolean anonymous;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Integer)
    private int helpfulCount;

    @Field(type = FieldType.Keyword)
    private String authorDisplayName;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    private Instant createdAt;
}

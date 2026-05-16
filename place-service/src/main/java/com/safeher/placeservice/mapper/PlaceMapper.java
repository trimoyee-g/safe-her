package com.safeher.placeservice.mapper;

import com.safeher.placeservice.dto.request.CreatePlaceRequest;
import com.safeher.placeservice.dto.request.UpdatePlaceRequest;
import com.safeher.placeservice.dto.response.PlaceResponse;
import com.safeher.placeservice.dto.response.PlaceSummaryResponse;
import com.safeher.placeservice.entity.Place;
import com.safeher.placeservice.entity.PlaceDocument;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mapstruct.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.util.List;

@Mapper(
    componentModel  = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    imports = { GeometryFactory.class, PrecisionModel.class, Coordinate.class }
)
public interface PlaceMapper {

    GeometryFactory GEO_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    // ── Entity from request ────────────────────────────────────────────────────

    @Mapping(target = "id",                   ignore = true)
    @Mapping(target = "externalId",           ignore = true)
    @Mapping(target = "source",               ignore = true)
    @Mapping(target = "location",             expression = "java(toPoint(request.getLongitude(), request.getLatitude()))")
    @Mapping(target = "safetyScore",          ignore = true)
    @Mapping(target = "totalRatings",         ignore = true)
    @Mapping(target = "aiSummary",            ignore = true)
    @Mapping(target = "aiSummaryGeneratedAt", ignore = true)
    @Mapping(target = "active",               ignore = true)
    @Mapping(target = "verified",             ignore = true)
    @Mapping(target = "reportedCount",        ignore = true)
    @Mapping(target = "createdBy",            ignore = true)
    @Mapping(target = "createdAt",            ignore = true)
    @Mapping(target = "updatedAt",            ignore = true)
    @Mapping(target = "googleMapsUrl",        ignore = true)
    Place toEntity(CreatePlaceRequest request);

    // ── Entity → response ──────────────────────────────────────────────────────

    @Mapping(target = "latitude",  expression = "java(place.getLatitude())")
    @Mapping(target = "longitude", expression = "java(place.getLongitude())")
    @Mapping(target = "distanceMeters", ignore = true)
    PlaceResponse toResponse(Place place);

    @Mapping(target = "latitude",    expression = "java(place.getLatitude())")
    @Mapping(target = "longitude",   expression = "java(place.getLongitude())")
    @Mapping(target = "avatarPhoto", expression = "java(place.getPhotos() != null && !place.getPhotos().isEmpty() ? place.getPhotos().get(0) : null)")
    @Mapping(target = "distanceMeters", ignore = true)
    PlaceSummaryResponse toSummary(Place place);

    List<PlaceSummaryResponse> toSummaryList(List<Place> places);

    // ── Entity → ES document ──────────────────────────────────────────────────

    @Mapping(target = "id",       expression = "java(place.getId().toString())")
    @Mapping(target = "location", expression = "java(toGeoPoint(place))")
    @Mapping(target = "source",   expression = "java(place.getSource() != null ? place.getSource().name() : null)")
    @Mapping(target = "category", expression = "java(place.getCategory() != null ? place.getCategory().name() : null)")
    PlaceDocument toDocument(Place place);

    // ── Partial update ─────────────────────────────────────────────────────────

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",                   ignore = true)
    @Mapping(target = "externalId",           ignore = true)
    @Mapping(target = "source",               ignore = true)
    @Mapping(target = "location",             expression = "java(request.getLatitude() != null && request.getLongitude() != null ? toPoint(request.getLongitude(), request.getLatitude()) : place.getLocation())")
    @Mapping(target = "safetyScore",          ignore = true)
    @Mapping(target = "totalRatings",         ignore = true)
    @Mapping(target = "aiSummary",            ignore = true)
    @Mapping(target = "aiSummaryGeneratedAt", ignore = true)
    @Mapping(target = "active",               ignore = true)
    @Mapping(target = "verified",             ignore = true)
    @Mapping(target = "reportedCount",        ignore = true)
    @Mapping(target = "createdBy",            ignore = true)
    @Mapping(target = "createdAt",            ignore = true)
    @Mapping(target = "updatedAt",            ignore = true)
    @Mapping(target = "googleMapsUrl",        ignore = true)
    void updateEntity(UpdatePlaceRequest request, @MappingTarget Place place);

    // ── Helper methods ─────────────────────────────────────────────────────────

    default Point toPoint(double longitude, double latitude) {
        return GEO_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    default GeoPoint toGeoPoint(Place place) {
        return new GeoPoint(place.getLatitude(), place.getLongitude());
    }
}

package com.example.search.service;

import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.search.document.ProductDocument;
import com.example.search.dto.SearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Full-text + faceted search against the products index.
 *
 *   q          → multi_match across name^3, brand^2, description (fuzziness AUTO)
 *   category   → term filter on category keyword
 *   brand      → term filter on brand keyword
 *   minPrice / maxPrice → range filter on discountedPrice
 *   minRating  → range filter on rating
 *   sort       → "relevance" (default) | "price_asc" | "price_desc" | "rating_desc"
 *
 * Returns paginated hits plus aggregations (brands, categories, price min/max)
 * so the UI can render a live facet sidebar.
 */
@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class SearchService {

    private static final SearchResponse EMPTY = new SearchResponse(
            List.of(), 0, 0, 0, 0,
            new SearchResponse.Facets(Map.of(), Map.of(), new SearchResponse.PriceStats(0, 0)));

    private final ElasticsearchOperations operations;

    public SearchResponse search(
            String q, String category, String brand,
            Double minPrice, Double maxPrice, Double minRating,
            String sort, int page, int size
    ) {
        try {
            return doSearch(q, category, brand, minPrice, maxPrice, minRating, sort, page, size);
        } catch (Exception ex) {
            log.warn("Search failed (index may not exist yet): {}", ex.getMessage());
            return EMPTY;
        }
    }

    private SearchResponse doSearch(
            String q, String category, String brand,
            Double minPrice, Double maxPrice, Double minRating,
            String sort, int page, int size
    ) {
        var qb = NativeQuery.builder()
                .withPageable(org.springframework.data.domain.PageRequest.of(page, size))
                .withTrackTotalHits(true);

        qb.withQuery(buildBoolQuery(q, category, brand, minPrice, maxPrice, minRating));
        applySort(qb, sort);
        addAggregations(qb);

        NativeQuery query = qb.build();

        SearchHits<ProductDocument> hits = operations.search(query, ProductDocument.class);

        long total = hits.getTotalHits();
        return new SearchResponse(
                hits.stream().map(SearchHit::getContent).toList(),
                total,
                (int) Math.ceil(total / (double) size),
                page,
                size,
                extractFacets(hits)
        );
    }

    // ---- query building ----

    private Query buildBoolQuery(String q, String category, String brand,
                                 Double minPrice, Double maxPrice, Double minRating) {
        return Query.of(b -> b.bool(bool -> {
            if (q != null && !q.isBlank()) {
                bool.must(m -> m.multiMatch(mm -> mm
                        .query(q)
                        .fields("name^3", "brand^2", "description")
                        .fuzziness("AUTO")));
            } else {
                bool.must(m -> m.matchAll(ma -> ma));
            }
            if (category != null && !category.isBlank()) {
                bool.filter(f -> f.term(t -> t.field("category").value(category)));
            }
            if (brand != null && !brand.isBlank()) {
                bool.filter(f -> f.term(t -> t.field("brand").value(brand)));
            }
            if (minPrice != null || maxPrice != null) {
                bool.filter(f -> f.range(r -> {
                    r.field("discountedPrice");
                    if (minPrice != null) r.gte(co.elastic.clients.json.JsonData.of(minPrice));
                    if (maxPrice != null) r.lte(co.elastic.clients.json.JsonData.of(maxPrice));
                    return r;
                }));
            }
            if (minRating != null) {
                bool.filter(f -> f.range(r -> r.field("rating")
                        .gte(co.elastic.clients.json.JsonData.of(minRating))));
            }
            return bool;
        }));
    }

    private void applySort(NativeQueryBuilder qb, String sort) {
        if (sort == null) return;
        SortOptions option = switch (sort) {
            case "price_asc" -> SortOptions.of(s -> s.field(FieldSort.of(f -> f
                    .field("discountedPrice").order(SortOrder.Asc))));
            case "price_desc" -> SortOptions.of(s -> s.field(FieldSort.of(f -> f
                    .field("discountedPrice").order(SortOrder.Desc))));
            case "rating_desc" -> SortOptions.of(s -> s.field(FieldSort.of(f -> f
                    .field("rating").order(SortOrder.Desc))));
            default -> null; // relevance (default ES score)
        };
        if (option != null) qb.withSort(option);
    }

    private void addAggregations(NativeQueryBuilder qb) {
        qb.withAggregation("brands", Aggregation.of(a -> a.terms(t -> t.field("brand").size(30))));
        qb.withAggregation("categories", Aggregation.of(a -> a.terms(t -> t.field("category").size(30))));
        qb.withAggregation("price_min", Aggregation.of(a -> a.min(m -> m.field("discountedPrice"))));
        qb.withAggregation("price_max", Aggregation.of(a -> a.max(m -> m.field("discountedPrice"))));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private SearchResponse.Facets extractFacets(SearchHits<ProductDocument> hits) {
        Map<String, Long> brands = new LinkedHashMap<>();
        Map<String, Long> categories = new LinkedHashMap<>();
        double priceMin = 0, priceMax = 0;

        var aggs = hits.getAggregations();
        if (aggs != null) {
            // Spring Data ES 5.x wraps ES aggregations — pull them out as raw maps via JSON
            var container = aggs.aggregations();
            if (container != null) {
                Map<String, Object> raw = (Map<String, Object>) (Map) container;
                brands = extractBuckets(raw.get("brands"));
                categories = extractBuckets(raw.get("categories"));
                priceMin = extractNumeric(raw.get("price_min"));
                priceMax = extractNumeric(raw.get("price_max"));
            }
        }
        return new SearchResponse.Facets(brands, categories,
                new SearchResponse.PriceStats(priceMin, priceMax));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> extractBuckets(Object agg) {
        // Best-effort; aggregations API differs across client versions. Fall back to empty.
        Map<String, Long> out = new LinkedHashMap<>();
        if (agg == null) return out;
        try {
            var aggregate = (co.elastic.clients.elasticsearch._types.aggregations.Aggregate) agg;
            if (aggregate.isSterms()) {
                aggregate.sterms().buckets().array()
                        .forEach(b -> out.put(b.key().stringValue(), b.docCount()));
            }
        } catch (Exception ignored) {
            // leave empty
        }
        return out;
    }

    private double extractNumeric(Object agg) {
        if (agg == null) return 0;
        try {
            var aggregate = (co.elastic.clients.elasticsearch._types.aggregations.Aggregate) agg;
            if (aggregate.isMin()) return aggregate.min().value();
            if (aggregate.isMax()) return aggregate.max().value();
        } catch (Exception ignored) {
            // ignore
        }
        return 0;
    }
}

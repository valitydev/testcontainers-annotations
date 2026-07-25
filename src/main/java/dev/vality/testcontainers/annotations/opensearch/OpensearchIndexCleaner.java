package dev.vality.testcontainers.annotations.opensearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.opensearch.client.Request;
import org.opensearch.client.ResponseException;
import org.opensearch.client.RestClient;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class OpensearchIndexCleaner {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<Map<String, String>>> INDEX_LIST_TYPE = new TypeReference<>() {
    };

    static void cleanup(
            GenericContainer<?> container,
            List<String> includedPrefixes,
            List<String> excludedIndexes) {
        var builder = RestClient.builder(new HttpHost(container.getHost(), container.getFirstMappedPort(), "http"));
        try (var client = builder.build()) {
            for (var index : listIndexes(client)) {
                if (shouldDelete(index, includedPrefixes, excludedIndexes)) {
                    deleteIndex(client, index);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to clean OpenSearch indexes", ex);
        }
    }

    private static List<String> listIndexes(RestClient client) throws IOException {
        var response = client.performRequest(new Request("GET", "/_cat/indices?format=json&h=index"));
        var entity = response.getEntity();
        if (entity == null) {
            return List.of();
        }
        try (var content = entity.getContent()) {
            return OBJECT_MAPPER.readValue(content, INDEX_LIST_TYPE).stream()
                    .map(index -> index.get("index"))
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        }
    }

    private static boolean shouldDelete(
            String index,
            List<String> includedPrefixes,
            List<String> excludedIndexes) {
        if (index.startsWith(".") || excludedIndexes.contains(index)) {
            return false;
        }
        return includedPrefixes.isEmpty() || includedPrefixes.stream().anyMatch(index::startsWith);
    }

    private static void deleteIndex(RestClient client, String index) throws IOException {
        var encodedIndex = URLEncoder.encode(index, StandardCharsets.UTF_8).replace("+", "%20");
        try {
            var response = client.performRequest(new Request("DELETE", "/" + encodedIndex));
            EntityUtils.consume(response.getEntity());
        } catch (ResponseException ex) {
            if (ex.getResponse().getStatusLine().getStatusCode() != 404) {
                throw ex;
            }
        }
    }
}

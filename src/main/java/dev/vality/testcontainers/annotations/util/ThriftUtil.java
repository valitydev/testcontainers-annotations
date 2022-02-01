package dev.vality.testcontainers.annotations.util;

import com.fasterxml.jackson.databind.JsonNode;
import dev.vality.geck.serializer.kit.json.JsonHandler;
import dev.vality.geck.serializer.kit.json.JsonProcessor;
import dev.vality.geck.serializer.kit.tbase.TBaseHandler;
import dev.vality.geck.serializer.kit.tbase.TBaseProcessor;
import lombok.SneakyThrows;
import org.apache.thrift.TBase;

public class ThriftUtil {

    @SneakyThrows
    public static <T extends TBase> JsonNode thriftToJson(T thrift) {
        return new TBaseProcessor().process(thrift, new JsonHandler());
    }

    @SneakyThrows
    public static <T extends TBase> T jsonToThrift(JsonNode jsonNode, Class<T> type) {
        return new JsonProcessor().process(jsonNode, new TBaseHandler<>(type));
    }
}

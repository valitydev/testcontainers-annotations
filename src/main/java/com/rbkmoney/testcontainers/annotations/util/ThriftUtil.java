package com.rbkmoney.testcontainers.annotations.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.rbkmoney.geck.serializer.kit.json.JsonHandler;
import com.rbkmoney.geck.serializer.kit.json.JsonProcessor;
import com.rbkmoney.geck.serializer.kit.mock.MockMode;
import com.rbkmoney.geck.serializer.kit.mock.MockTBaseProcessor;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseHandler;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseProcessor;
import com.rbkmoney.machinegun.msgpack.Value;
import lombok.SneakyThrows;
import lombok.var;
import org.apache.thrift.TBase;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.time.Instant;

public class ThriftUtil {

    @SneakyThrows
    public static <T extends TBase<?, ?>> T fillThriftObject(T data, Class<T> type) {
        var mockTBaseProcessor = new MockTBaseProcessor(MockMode.REQUIRED_ONLY, 25, 1);
        mockTBaseProcessor.addFieldHandler(
                structHandler -> structHandler.value(Instant.now().toString()),
                "created_at", "at", "due");
        return mockTBaseProcessor.process(data, new TBaseHandler<>(type));
    }

    @SneakyThrows
    public static <T extends TBase> JsonNode thriftBaseToJson(T thriftBase) {
        return new TBaseProcessor().process(thriftBase, new JsonHandler());
    }

    @SneakyThrows
    public static <T extends TBase> T jsonToThriftBase(JsonNode jsonNode, Class<T> type) {
        return new JsonProcessor().process(jsonNode, new TBaseHandler<>(type));
    }

    @SneakyThrows
    public static Value toByteArray(TBase<?, ?> data) {
        return Value.bin(
                new TSerializer(new TBinaryProtocol.Factory())
                        .serialize(data));
    }
}

package com.aibackend.AiBasedEndtoEndSystem.jackson;

import java.io.IOException;

import com.aibackend.AiBasedEndtoEndSystem.entity.SalaryRangeLpa;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Accepts {@code salaryRangeInLPA} as {@code [min, max]}, a single string element {@code ["4-5"]}, or a string {@code "4-5"}.
 */
public class SalaryRangeLpaDeserializer extends JsonDeserializer<SalaryRangeLpa> {

    @Override
    public SalaryRangeLpa deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        if (t == JsonToken.VALUE_NULL || t == null) {
            return null;
        }
        if (t == JsonToken.VALUE_STRING) {
            return parseFromDashString(p.getText());
        }
        if (t == JsonToken.START_ARRAY) {
            JsonNode node = p.getCodec().readTree(p);
            if (node == null || !node.isArray() || node.isEmpty()) {
                return null;
            }
            if (node.size() == 1) {
                JsonNode e0 = node.get(0);
                if (e0 != null && e0.isTextual()) {
                    SalaryRangeLpa fromText = parseFromDashString(e0.asText());
                    if (fromText != null) {
                        return fromText;
                    }
                }
                if (e0 != null && e0.isNumber()) {
                    int v = e0.asInt();
                    return SalaryRangeLpa.of(v, v);
                }
            }
            if (node.size() >= 2) {
                Integer a = readIntFlexible(node.get(0));
                Integer b = readIntFlexible(node.get(1));
                if (a != null && b != null) {
                    return SalaryRangeLpa.of(a, b);
                }
            }
            throw JsonMappingException.from(
                    p,
                    "salaryRangeInLPA: use [min, max] numbers, e.g. [4, 5], or one string \"4-5\"");
        }
        throw JsonMappingException.from(
                p,
                "salaryRangeInLPA must be null, a string like \"4-5\", or array [min, max]");
    }

    private static Integer readIntFlexible(JsonNode n) {
        if (n == null || n.isNull()) {
            return null;
        }
        if (n.isNumber()) {
            return n.asInt();
        }
        if (n.isTextual()) {
            try {
                return Integer.parseInt(n.asText().trim());
            } catch (NumberFormatException ignored) {
                SalaryRangeLpa dash = parseFromDashString(n.asText());
                if (dash != null && dash.getMin() != null) {
                    return dash.getMin();
                }
            }
        }
        return null;
    }

    static SalaryRangeLpa parseFromDashString(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim().replace('–', '-').replace('—', '-').replaceAll("\\s+", "");
        int dash = normalized.indexOf('-');
        if (dash <= 0 || dash >= normalized.length() - 1) {
            return null;
        }
        try {
            int a = Integer.parseInt(normalized.substring(0, dash));
            int b = Integer.parseInt(normalized.substring(dash + 1));
            return SalaryRangeLpa.of(a, b);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

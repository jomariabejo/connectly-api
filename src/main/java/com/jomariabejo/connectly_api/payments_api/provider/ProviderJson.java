package com.jomariabejo.connectly_api.payments_api.provider;

import com.fasterxml.jackson.databind.JsonNode;

public final class ProviderJson {
    private ProviderJson() {
    }

    public static String textAt(JsonNode node, String pointer) {
        JsonNode value = node.at(pointer);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}

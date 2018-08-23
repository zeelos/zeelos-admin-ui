package io.zeelos.ui.admin.model;

import java.util.List;
import java.util.Map;

public class Structure {

    private List<Map<String, String>> nodes;
    private List<Map<String, String>> links;

    public Structure(List<Map<String, String>> nodes, List<Map<String, String>> links) {
        this.nodes = nodes;
        this.links = links;
    }

    public List<Map<String, String>> getNodes() {
        return nodes;
    }

    public List<Map<String, String>> getLinks() {
        return links;
    }
}

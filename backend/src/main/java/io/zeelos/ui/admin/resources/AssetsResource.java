package io.zeelos.ui.admin.resources;

import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/assets")
@Singleton
public class AssetsResource {

    private static Logger log = LoggerFactory.getLogger(AssetsResource.class);

    @Inject
    @ConfigProperty(name = "tinkerpop3.url")
    String tinkerpop3Url;
    @Inject
    @ConfigProperty(name = "tinkerpop3.user")
    String tinkerpop3User;
    @Inject
    @ConfigProperty(name = "tinkerpop3.password")
    String tinkerpop3Password;

    private OrientGraphFactory gf;

    @PostConstruct
    void init() {
        // initialize GraphFactory
        this.gf = new OrientGraphFactory(tinkerpop3Url, tinkerpop3User, tinkerpop3Password);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject retrieveTopology() {
        log.info("retrieving topology ..");

        final JsonObjectBuilder structure = Json.createObjectBuilder();

        final JsonArrayBuilder n = Json.createArrayBuilder();
        final JsonArrayBuilder l = Json.createArrayBuilder();

        // store any "Servers" node type to create virtual links
        final List<Vertex> servers = new ArrayList<>();

        try (Graph graph = gf.getNoTx()) {
            GraphTraversalSource traversal = graph.traversal();

            // add virtual root "Gateways" node (with id:1) where "Servers"
            // nodes will attach to. Mainly used by zoomchart for it's "initialNodes" property
            final JsonObjectBuilder root = Json.createObjectBuilder();
            root.add("id", "1");
            root.add("name", "Gateways");
            root.add("type", "Root");
            root.add("loaded", true);
            n.add(root);

            // setup vertexes..
            traversal.V().toList().stream()
                    .map(vertex -> {
                        // convert Vertex to Json object..

                        JsonObjectBuilder obj = Json.createObjectBuilder();

                        // required by zoomchart
                        obj.add("id", vertex.id().toString());
                        obj.add("name", vertex.value("name").toString());
                        obj.add("type", vertex.label());
                        obj.add("loaded", true);
                        // any other properties from graph
                        vertex.keys().forEach(key -> {
                            if (!key.equals("id")) // ignore id to not conflict
                                obj.add(key, vertex.value(key).toString());
                        });

                        // if "Servers" node type, store it to create a virtual edge from "Gateways" node
                        if (vertex.label().equals("Servers")) {
                            servers.add(vertex);
                        }

                        return obj.build();
                    })
                    .forEach(n::add);

            structure.add("nodes", n.build());

            // setup edges..
            traversal.E().toList().stream()
                    .map(edge -> {
                        // convert Edge to Json object..

                        JsonObjectBuilder obj = Json.createObjectBuilder();

                        // required by zoomchart
                        obj.add("id", edge.id().toString());
                        obj.add("name", edge.label());
                        obj.add("from", edge.value("out").toString().replaceAll("[v\\[\\]]", ""));
                        obj.add("to", edge.value("in").toString().replaceAll("[v\\[\\]]", ""));

                        return obj.build();
                    })
                    .forEach(l::add);

            // add virtual link from "Gateways" node to (any) "Servers"
            servers.forEach(vertex -> {
                JsonObjectBuilder obj = Json.createObjectBuilder();

                obj.add("id", UUID.randomUUID().toString());
                obj.add("name", "hasGateway");
                obj.add("from", "1");
                obj.add("to", vertex.id().toString());

                l.add(obj);
            });

            structure.add("links", l.build());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // ready to return it
        return structure.build();
    }

    private List<String> urlDecode(String[] encodedIds) {
        List<String> decodedIds = new ArrayList<>(encodedIds.length);

        try {
            for (String encodedId : encodedIds) {
                decodedIds.add(URLDecoder.decode(encodedId, "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decodedIds;
    }
}


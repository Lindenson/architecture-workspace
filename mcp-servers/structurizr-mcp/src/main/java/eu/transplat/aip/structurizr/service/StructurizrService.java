package eu.transplat.aip.structurizr.service;

import com.structurizr.Workspace;
import com.structurizr.dsl.StructurizrDslParser;
import com.structurizr.dsl.StructurizrDslParserException;
import com.structurizr.model.Component;
import com.structurizr.model.Container;
import com.structurizr.model.Element;
import com.structurizr.model.Model;
import com.structurizr.model.Person;
import com.structurizr.model.Relationship;
import com.structurizr.model.SoftwareSystem;
import com.structurizr.view.View;
import com.structurizr.view.ViewSet;
import eu.transplat.aip.mcp.common.Confidence;
import eu.transplat.aip.mcp.common.McpResponse;
import eu.transplat.aip.structurizr.config.StructurizrProperties;
import eu.transplat.aip.structurizr.domain.ArchitectureState;
import eu.transplat.aip.structurizr.domain.DriftReport;
import eu.transplat.aip.structurizr.domain.ModelElement;
import eu.transplat.aip.structurizr.domain.RelationshipView;
import eu.transplat.aip.structurizr.domain.ValidationResult;
import eu.transplat.aip.structurizr.domain.ViewSummary;
import eu.transplat.aip.structurizr.domain.WorkspaceSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Structurizr-backed MCP tools. The C4 model is parsed from the DSL file
 * <em>lazily, per request</em> — never at startup — so the server always boots
 * even when the workspace file is missing or invalid. No method throws out of a
 * {@code @Tool}: on a missing file or parse error it returns an
 * {@code ERROR} / {@code DATA_STALE} {@link McpResponse}.
 *
 * <p>This is the {@code ARCHITECTURE_MODEL} source (MVP-2). Per the source-of-
 * truth hierarchy, <strong>code is authoritative</strong>; the DSL should be
 * regenerated from code (jQAssistant → Structurizr) and drift surfaced via
 * {@link #detectDrift(String)}.
 */
@Service
public class StructurizrService {

    /** Provenance label carried in every {@link McpResponse}. */
    public static final String SOURCE = "structurizr-mcp:workspace.dsl";

    private static final Logger log = LoggerFactory.getLogger(StructurizrService.class);

    private final StructurizrProperties properties;

    public StructurizrService(StructurizrProperties properties) {
        this.properties = properties;
    }

    // ------------------------------------------------------------------ tools

    @Tool(description = "Summary of the Structurizr C4 workspace: name, description, people, software systems with their containers/components, relationship and view counts.")
    public McpResponse readWorkspace() {
        try {
            Workspace ws = parse();
            return McpResponse.ok(buildSummary(ws), SOURCE);
        } catch (LoadException e) {
            return e.toResponse();
        }
    }

    @Tool(description = "Validate the Structurizr C4 model: parse the DSL and report {valid, errors, warnings}. Adds best-effort sanity warnings (e.g. container with no components, element with no relationships).")
    public McpResponse validateModel() {
        Workspace ws;
        try {
            ws = parse();
        } catch (LoadException e) {
            // A parse/load failure is a validation failure, not a tool error:
            // return the structured result so the agent can act on it.
            return McpResponse.ok(
                    new ValidationResult(false, List.of(e.getMessage()), List.of()),
                    SOURCE, Confidence.HIGH);
        }
        List<String> warnings = sanityWarnings(ws);
        return McpResponse.ok(new ValidationResult(true, List.of(), warnings), SOURCE);
    }

    @Tool(description = "List C4 model elements of a given type. type is one of: person, system, container, component. Each element carries its parent and technology.")
    public McpResponse listElements(String type) {
        if (type == null || type.isBlank()) {
            return McpResponse.error(SOURCE, "type is required: one of person, system, container, component.");
        }
        String t = type.trim().toLowerCase();
        try {
            Workspace ws = parse();
            List<ModelElement> elements = collectElements(ws.getModel(), t);
            if (elements == null) {
                return McpResponse.error(SOURCE,
                        "Unknown type '" + type + "'. Use one of: person, system, container, component.");
            }
            return McpResponse.ok(elements, SOURCE);
        } catch (LoadException e) {
            return e.toResponse();
        }
    }

    @Tool(description = "List all relationships in the Structurizr model: source, destination, description, technology.")
    public McpResponse listRelationships() {
        try {
            Workspace ws = parse();
            List<RelationshipView> rels = new ArrayList<>();
            for (Relationship r : ws.getModel().getRelationships()) {
                rels.add(new RelationshipView(
                        r.getSource().getName(),
                        r.getDestination().getName(),
                        nullToEmpty(r.getDescription()),
                        nullToEmpty(r.getTechnology())));
            }
            return McpResponse.ok(rels, SOURCE);
        } catch (LoadException e) {
            return e.toResponse();
        }
    }

    @Tool(description = "List all views defined in the Structurizr workspace: key, type, title.")
    public McpResponse getViews() {
        try {
            Workspace ws = parse();
            return McpResponse.ok(collectViews(ws.getViews()), SOURCE);
        } catch (LoadException e) {
            return e.toResponse();
        }
    }

    @Tool(description = "Drift hook: given a comma-separated list of ACTUAL component/package names from the code (e.g. from jqassistant-mcp), compare against the model's container/component names and return {inModelNotInCode, inCodeNotInModel, matched}. Heuristic, case-insensitive contains matching.")
    public McpResponse detectDrift(String actualComponentsCsv) {
        if (actualComponentsCsv == null || actualComponentsCsv.isBlank()) {
            return McpResponse.stale(null, SOURCE,
                    "provide actual components, e.g. from jqassistant-mcp, to compute drift");
        }
        try {
            Workspace ws = parse();
            DriftReport report = computeDrift(ws.getModel(), actualComponentsCsv);
            // Heuristic name matching → MEDIUM confidence, never HIGH.
            return McpResponse.ok(report, SOURCE, Confidence.MEDIUM);
        } catch (LoadException e) {
            return e.toResponse();
        }
    }

    @Tool(description = "ARCHITECTURE_MODEL state slice for the digital twin: {workspaceName, systems, containers, components, relationships, views, parsedOk, workspacePath}. Missing file or parse error returns DATA_STALE with LOW confidence.")
    public McpResponse getState() {
        String path = properties.getWorkspacePath();
        try {
            Workspace ws = parse();
            Model model = ws.getModel();
            int containers = 0;
            int components = 0;
            for (SoftwareSystem system : model.getSoftwareSystems()) {
                containers += system.getContainers().size();
                for (Container container : system.getContainers()) {
                    components += container.getComponents().size();
                }
            }
            ArchitectureState state = new ArchitectureState(
                    ws.getName(),
                    model.getSoftwareSystems().size(),
                    containers,
                    components,
                    model.getRelationships().size(),
                    ws.getViews().getViews().size(),
                    true,
                    path);
            return McpResponse.ok(state, SOURCE);
        } catch (LoadException e) {
            ArchitectureState empty = new ArchitectureState(
                    null, 0, 0, 0, 0, 0, false, path);
            return McpResponse.stale(empty, SOURCE, e.getMessage());
        }
    }

    // -------------------------------------------------------------- internals

    /**
     * Parse the workspace DSL lazily. Wraps every failure mode (missing file,
     * parse error, unexpected exception) in a {@link LoadException} carrying a
     * ready-made {@link McpResponse}.
     */
    private Workspace parse() throws LoadException {
        String path = properties.getWorkspacePath();
        if (path == null || path.isBlank()) {
            throw LoadException.stale("workspace path is not configured (structurizr.workspace-path).");
        }
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw LoadException.stale("workspace DSL file not found at '" + file.getPath() + "'.");
        }
        try {
            StructurizrDslParser parser = new StructurizrDslParser();
            parser.parse(file);
            Workspace ws = parser.getWorkspace();
            if (ws == null) {
                throw LoadException.error("parser returned no workspace for '" + file.getPath() + "'.");
            }
            return ws;
        } catch (StructurizrDslParserException e) {
            log.warn("Structurizr DSL parse failed for {}: {}", file.getPath(), e.toString());
            throw LoadException.error("DSL parse error: " + e.getMessage());
        } catch (LoadException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Unexpected error reading workspace {}: {}", file.getPath(), e.toString());
            throw LoadException.error("failed to read workspace '" + file.getPath() + "': " + e.getMessage());
        }
    }

    private WorkspaceSummary buildSummary(Workspace ws) {
        Model model = ws.getModel();

        List<String> people = new ArrayList<>();
        for (Person p : model.getPeople()) {
            people.add(p.getName());
        }

        List<WorkspaceSummary.SoftwareSystemSummary> systems = new ArrayList<>();
        for (SoftwareSystem system : model.getSoftwareSystems()) {
            List<WorkspaceSummary.ContainerSummary> containers = new ArrayList<>();
            for (Container container : system.getContainers()) {
                containers.add(new WorkspaceSummary.ContainerSummary(
                        container.getName(),
                        nullToEmpty(container.getTechnology()),
                        container.getComponents().size()));
            }
            systems.add(new WorkspaceSummary.SoftwareSystemSummary(
                    system.getName(), containers.size(), containers));
        }

        return new WorkspaceSummary(
                ws.getName(),
                nullToEmpty(ws.getDescription()),
                people,
                systems,
                model.getRelationships().size(),
                ws.getViews().getViews().size());
    }

    /**
     * Best-effort model sanity checks. Never fatal — purely advisory warnings.
     */
    private List<String> sanityWarnings(Workspace ws) {
        List<String> warnings = new ArrayList<>();
        Model model = ws.getModel();

        if (model.getSoftwareSystems().isEmpty()) {
            warnings.add("Model has no software systems.");
        }
        if (model.getRelationships().isEmpty()) {
            warnings.add("Model has no relationships.");
        }
        if (ws.getViews().getViews().isEmpty()) {
            warnings.add("Workspace defines no views.");
        }

        for (SoftwareSystem system : model.getSoftwareSystems()) {
            if (hasNoRelationships(system)) {
                warnings.add("Software system '" + system.getName() + "' has no relationships.");
            }
            for (Container container : system.getContainers()) {
                if (container.getComponents().isEmpty()) {
                    warnings.add("Container '" + system.getName() + " / " + container.getName()
                            + "' has no components.");
                }
                if (hasNoRelationships(container)) {
                    warnings.add("Container '" + system.getName() + " / " + container.getName()
                            + "' has no relationships.");
                }
            }
        }
        return warnings;
    }

    private static boolean hasNoRelationships(Element element) {
        return element.getRelationships().isEmpty() && !hasInboundRelationship(element);
    }

    private static boolean hasInboundRelationship(Element element) {
        Model model = element.getModel();
        for (Relationship r : model.getRelationships()) {
            if (r.getDestination().equals(element)) {
                return true;
            }
        }
        return false;
    }

    /** Returns matching elements, or {@code null} for an unknown type. */
    private List<ModelElement> collectElements(Model model, String type) {
        List<ModelElement> out = new ArrayList<>();
        switch (type) {
            case "person" -> {
                for (Person p : model.getPeople()) {
                    out.add(new ModelElement("person", p.getName(), null, null, nullToEmpty(p.getDescription())));
                }
            }
            case "system" -> {
                for (SoftwareSystem s : model.getSoftwareSystems()) {
                    out.add(new ModelElement("system", s.getName(), null, null, nullToEmpty(s.getDescription())));
                }
            }
            case "container" -> {
                for (SoftwareSystem s : model.getSoftwareSystems()) {
                    for (Container c : s.getContainers()) {
                        out.add(new ModelElement("container", c.getName(), s.getName(),
                                nullToEmpty(c.getTechnology()), nullToEmpty(c.getDescription())));
                    }
                }
            }
            case "component" -> {
                for (SoftwareSystem s : model.getSoftwareSystems()) {
                    for (Container c : s.getContainers()) {
                        for (Component comp : c.getComponents()) {
                            out.add(new ModelElement("component", comp.getName(), c.getName(),
                                    nullToEmpty(comp.getTechnology()), nullToEmpty(comp.getDescription())));
                        }
                    }
                }
            }
            default -> {
                return null;
            }
        }
        return out;
    }

    private List<ViewSummary> collectViews(ViewSet views) {
        List<ViewSummary> out = new ArrayList<>();
        for (View v : views.getViews()) {
            out.add(new ViewSummary(
                    v.getKey(),
                    v.getClass().getSimpleName(),
                    nullToEmpty(viewTitle(v))));
        }
        return out;
    }

    private static String viewTitle(View v) {
        String title = v.getTitle();
        return (title == null || title.isBlank()) ? v.getDescription() : title;
    }

    /**
     * Heuristic drift comparison: case-insensitive {@code contains} matching of
     * actual code names against the model's container + component names.
     */
    private DriftReport computeDrift(Model model, String actualComponentsCsv) {
        List<String> modelNames = new ArrayList<>();
        for (SoftwareSystem s : model.getSoftwareSystems()) {
            for (Container c : s.getContainers()) {
                modelNames.add(c.getName());
                for (Component comp : c.getComponents()) {
                    modelNames.add(comp.getName());
                }
            }
        }

        List<String> codeNames = Arrays.stream(actualComponentsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        List<String> matched = new ArrayList<>();
        List<String> inModelNotInCode = new ArrayList<>();
        List<String> inCodeNotInModel = new ArrayList<>();

        for (String m : modelNames) {
            if (codeNames.stream().anyMatch(c -> fuzzyMatch(m, c))) {
                matched.add(m);
            } else {
                inModelNotInCode.add(m);
            }
        }
        for (String c : codeNames) {
            if (modelNames.stream().noneMatch(m -> fuzzyMatch(m, c))) {
                inCodeNotInModel.add(c);
            }
        }

        return new DriftReport(inModelNotInCode, inCodeNotInModel, matched, true);
    }

    /** Case-insensitive, symmetric {@code contains} match. */
    private static boolean fuzzyMatch(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String la = a.toLowerCase().trim();
        String lb = b.toLowerCase().trim();
        if (la.isEmpty() || lb.isEmpty()) {
            return false;
        }
        return la.equals(lb) || la.contains(lb) || lb.contains(la);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /**
     * Internal carrier that pairs a failure message with the right
     * {@link McpResponse} factory (ERROR for unexpected/parse failures,
     * DATA_STALE for the expected "no model yet" cases).
     */
    private static final class LoadException extends Exception {

        private final boolean stale;

        private LoadException(String message, boolean stale) {
            super(message);
            this.stale = stale;
        }

        static LoadException stale(String message) {
            return new LoadException(message, true);
        }

        static LoadException error(String message) {
            return new LoadException(message, false);
        }

        McpResponse toResponse() {
            return stale
                    ? McpResponse.stale(null, SOURCE, getMessage())
                    : McpResponse.error(SOURCE, getMessage());
        }
    }
}

package eu.transplat.aip.structurizr.domain;

/**
 * A single relationship in the model, returned by {@code listRelationships}.
 *
 * @param source      source element name
 * @param destination destination element name
 * @param description relationship description
 * @param technology  relationship technology / protocol
 */
public record RelationshipView(
        String source,
        String destination,
        String description,
        String technology) {
}

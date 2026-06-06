package eu.transplat.aip.structurizr.domain;

/**
 * A single C4 model element, flattened for {@code listElements}.
 *
 * @param type       one of person / system / container / component
 * @param name       element name
 * @param parent     parent element name (system for a container, container for a
 *                   component); {@code null} for people and systems
 * @param technology element technology, where applicable
 * @param description element description
 */
public record ModelElement(
        String type,
        String name,
        String parent,
        String technology,
        String description) {
}

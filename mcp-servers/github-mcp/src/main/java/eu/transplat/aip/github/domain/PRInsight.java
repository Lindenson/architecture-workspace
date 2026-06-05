package eu.transplat.aip.github.domain;

import java.util.List;

/**
 * Analytical view of a single pull request used by the digital twin to reason
 * about the blast radius of a change.
 *
 * @param number         PR/MR number
 * @param title          title
 * @param author         author username
 * @param state          {@code open} / {@code closed} / {@code merged}
 * @param changedFiles   number of files touched
 * @param additions      added lines
 * @param deletions      deleted lines
 * @param touchedPackages distinct top-level directories of changed files (proxy for affected modules)
 */
public record PRInsight(
        int number,
        String title,
        String author,
        String state,
        int changedFiles,
        int additions,
        int deletions,
        List<String> touchedPackages) {
}

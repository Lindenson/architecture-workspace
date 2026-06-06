package eu.transplat.aip.rag.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple character-window chunker with overlap. Splits on a sliding window of
 * {@code maxChars} characters, preferring to break at the last whitespace inside
 * the window so chunks don't cut words mid-token. Adjacent chunks share
 * {@code overlap} characters to preserve context across boundaries.
 */
final class Chunker {

    private Chunker() {
    }

    static List<String> chunk(String text, int maxChars, int overlap) {
        List<String> out = new ArrayList<>();
        if (text == null) {
            return out;
        }
        String normalized = text.strip();
        if (normalized.isEmpty()) {
            return out;
        }
        int max = Math.max(1, maxChars);
        int ov = Math.max(0, Math.min(overlap, max - 1));
        int len = normalized.length();
        int start = 0;
        while (start < len) {
            int end = Math.min(start + max, len);
            // Prefer a whitespace boundary inside the window (not when we hit the end).
            if (end < len) {
                int lastWs = normalized.lastIndexOf(' ', end);
                if (lastWs > start + max / 2) {
                    end = lastWs;
                }
            }
            String piece = normalized.substring(start, end).strip();
            if (!piece.isEmpty()) {
                out.add(piece);
            }
            if (end >= len) {
                break;
            }
            start = Math.max(end - ov, start + 1);
        }
        return out;
    }
}

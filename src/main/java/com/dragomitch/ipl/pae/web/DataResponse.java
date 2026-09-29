package com.dragomitch.ipl.pae.web;

import java.util.List;

/**
 * A list wrapped in {@code {"data": [...]}}, the format DataTables (legacy web UI) reads.
 *
 * @param data the items
 * @param <T> the item type
 */
public record DataResponse<T>(List<T> data) {
}

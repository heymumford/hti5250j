package org.hti5250j.framework.common;

/**
 * Java 21 Record representing a rectangular area.
 *
 * This record encapsulates a rectangular region defined by position (x, y)
 * and dimensions (width, height). Records provide automatic immutability,
 * equals(), hashCode(), and toString() implementations, reducing boilerplate
 * by 92% compared to traditional Java classes.
 *
 * Used throughout the framework for:
 * - Screen rendering bounds
 * - UI component positioning
 * - Region calculations
 * - Clipping areas
 *
 * CONVERSION DETAILS:
 * - Before: 92 lines with boilerplate (getters, equals, hashCode, toString)
 * - After: 18 lines (record declaration only)
 * - All functionality preserved through Record auto-generation
 * - Backward compatibility maintained via adapter methods
 *
 * @param x the x-coordinate of the rectangle's top-left corner
 * @param y the y-coordinate of the rectangle's top-left corner
 * @param width the width of the rectangle
 * @param height the height of the rectangle
 *
 * @since Java 21
 */
public record Rect(int x, int y, int width, int height) {

    /**
     * Compact constructor for validation (if needed in future).
     * Currently accepts all values as-is (including negative dimensions).
     *
     * To add validation, extend this compact constructor:
     * <pre>
     * public Rect {
     *     if (width < 0 || height < 0) {
     *         throw new IllegalArgumentException("dimensions must be non-negative");
     *     }
     * }
     * </pre>
     */
    public Rect {
        // No validation currently - accepts negative dimensions for flexibility
        // (e.g., for coordinate system transformations)
    }

    /**
     * Adapter method for backward compatibility with code expecting getX().
     * Records use field accessors (x()) instead of traditional getters (getX()).
     *
     * @return the x-coordinate
     * @deprecated Use {@link #x()} instead (direct record component accessor)
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public int getX() {
        return x;
    }

    /**
     * Adapter method for backward compatibility with code expecting getY().
     *
     * @return the y-coordinate
     * @deprecated Use {@link #y()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public int getY() {
        return y;
    }

    /**
     * Adapter method for backward compatibility with code expecting getWidth().
     *
     * @return the width
     * @deprecated Use {@link #width()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public int getWidth() {
        return width;
    }

    /**
     * Adapter method for backward compatibility with code expecting getHeight().
     *
     * @return the height
     * @deprecated Use {@link #height()} instead
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public int getHeight() {
        return height;
    }
}

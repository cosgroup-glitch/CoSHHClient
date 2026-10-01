package haven;

/** Estimates meals from food-meter increases; server refreshes are not meals. */
final class FeastFoodCounter {
    private double previous = Double.NaN;

    boolean update(double total) {
        if(!Double.isFinite(total) || (total < 0)) {
            previous = Double.NaN;
            return false;
        }
        boolean increased = Double.isFinite(previous) && (total > previous);
        previous = total;
        return increased;
    }
}

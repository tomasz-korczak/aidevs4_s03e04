package pl.tomaszko.s03e04.service;

public record SearchOutcome(String output, boolean infrastructureError) {

    public static SearchOutcome ok(String output) {
        return new SearchOutcome(output, false);
    }

    public static SearchOutcome infrastructure(String output) {
        return new SearchOutcome(output, true);
    }
}

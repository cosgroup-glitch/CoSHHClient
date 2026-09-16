package haven;

public enum CombatReducerMode {
    OFF("Off"),
    SEMI("Semi"),
    ON("On");

    public final String label;

    CombatReducerMode(String label) {
	this.label = label;
    }

    public CombatReducerMode next() {
	switch(this) {
	    case ON: return SEMI;
	    case SEMI: return OFF;
	    default: return ON;
	}
    }
}

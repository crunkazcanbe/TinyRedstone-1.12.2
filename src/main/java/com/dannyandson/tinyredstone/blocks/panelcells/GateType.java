package com.dannyandson.tinyredstone.blocks.panelcells;

/**
 * Every gate type the one configurable {@link LogicGate} cell can morph into — the full
 * Project Red catalog (task #235). Grouped by {@link Category}; the morphing config menu uses
 * the category + per-type metadata to show the right options for the selected type.
 *
 * <p>Each type carries:
 * <ul>
 *   <li>{@link #title} — display name (also the en_us.lang key suffix)</li>
 *   <li>{@link #category} — drives menu layout, accent color, and which options appear</li>
 *   <li>input mask ({@link #usesBack}/{@link #usesLeft}/{@link #usesRight}) — which sides feed in</li>
 *   <li>{@link #logic} — pure combinational function, or {@code null} for stateful/array types
 *       that compute in {@link LogicGate#tick} instead</li>
 * </ul>
 *
 * <p>Convention: output is always the FRONT side. The 9 combinational gates are fully wired here.
 * Stateful gates (latches/timers/counters/sensors) are selectable and persisted now and get their
 * tick logic filled in incrementally. ARRAY/bus cells need bundled cable (task #232) before they
 * function, but are present in the enum from the start so the cell and its menu cover all of them.
 */
public enum GateType {

    // ---- SIMPLE (pure combinational — fully implemented) ----
    AND        ("AND",         Category.SIMPLE, false, true,  true,  (b,l,r) -> l && r),
    OR         ("OR",          Category.SIMPLE, false, true,  true,  (b,l,r) -> l || r),
    NOT        ("NOT",         Category.SIMPLE, true,  false, false, (b,l,r) -> !b),
    NOR        ("NOR",         Category.SIMPLE, false, true,  true,  (b,l,r) -> !(l || r)),
    NAND       ("NAND",        Category.SIMPLE, false, true,  true,  (b,l,r) -> !(l && r)),
    XOR        ("XOR",         Category.SIMPLE, false, true,  true,  (b,l,r) -> l ^ r),
    XNOR       ("XNOR",        Category.SIMPLE, false, true,  true,  (b,l,r) -> !(l ^ r)),
    BUFFER     ("Buffer",      Category.SIMPLE, true,  false, false, (b,l,r) -> b),
    MULTIPLEXER("Multiplexer", Category.SIMPLE, true,  true,  true,  (b,l,r) -> b ? r : l), // back = select

    // ---- LATCH (stateful — tick logic pending) ----
    RS_LATCH         ("RS Latch",          Category.LATCH, false, true, true, null),
    TOGGLE_LATCH     ("Toggle Latch",      Category.LATCH, false, true, true, null),
    TRANSPARENT_LATCH("Transparent Latch", Category.LATCH, false, true, true, null),

    // ---- TIMING / SEQUENTIAL (stateful — tick logic pending) ----
    REPEATER    ("Repeater",     Category.TIMING, true,  false, false, null),
    TIMER       ("Timer",        Category.TIMING, true,  false, false, null),
    SEQUENCER   ("Sequencer",    Category.TIMING, true,  false, false, null),
    COUNTER     ("Counter",      Category.TIMING, false, true,  true,  null),
    STATE_CELL  ("State Cell",   Category.TIMING, false, true,  true,  null),
    SYNCHRONIZER("Synchronizer", Category.TIMING, false, true,  true,  null),
    PULSE_FORMER("Pulse Former", Category.TIMING, true,  false, false, null),
    RANDOMIZER  ("Randomizer",   Category.TIMING, true,  false, false, null),

    // ---- SENSOR (stateful — reads world, tick logic pending) ----
    LIGHT_SENSOR("Light Sensor", Category.SENSOR, false, false, false, null),
    RAIN_SENSOR ("Rain Sensor",  Category.SENSOR, false, false, false, null),

    // ---- SPECIAL ----
    COMPARATOR("Comparator", Category.SPECIAL, true,  false, true,  null), // analog, tick logic pending

    // ---- ARRAY / BUS CELLS (need bundled cable, task #232) ----
    NULL_CELL      ("Null Cell",       Category.ARRAY, false, true, true, null),
    INVERT_CELL    ("Invert Cell",     Category.ARRAY, false, true, true, null),
    BUFFER_CELL    ("Buffer Cell",     Category.ARRAY, false, true, true, null),
    AND_CELL       ("AND Cell",        Category.ARRAY, false, true, true, null),
    BUS_TRANSCEIVER("Bus Transceiver", Category.ARRAY, true,  true, true, null),
    BUS_RANDOMIZER ("Bus Randomizer",  Category.ARRAY, false, true, true, null),
    BUS_CONVERTER  ("Bus Converter",   Category.ARRAY, false, true, true, null),
    BUS_INPUT_PANEL("Bus Input Panel", Category.ARRAY, false, false,false,null),
    STRETCHING_CELL("Stretching Cell", Category.ARRAY, false, true, true, null),
    SEGMENT_DISPLAY("Segment Display", Category.ARRAY, false, true, true, null),
    DEC_COUNTER    ("Dec Counter",     Category.ARRAY, false, true, true, null);

    /** What kind of gate — drives menu layout + accent color. */
    public enum Category { SIMPLE, LATCH, TIMING, SENSOR, SPECIAL, ARRAY }

    /** Pure combinational function: (backIn, leftIn, rightIn) -> frontOut. */
    @FunctionalInterface
    public interface GateLogic { boolean apply(boolean back, boolean left, boolean right); }

    public final String title;
    public final Category category;
    public final boolean usesBack, usesLeft, usesRight;
    /** non-null only for the combinational gates that need no stored state */
    public final GateLogic logic;

    GateType(String title, Category category, boolean usesBack, boolean usesLeft, boolean usesRight, GateLogic logic) {
        this.title = title;
        this.category = category;
        this.usesBack = usesBack;
        this.usesLeft = usesLeft;
        this.usesRight = usesRight;
        this.logic = logic;
    }

    /** True if this gate's output is a pure function of its current inputs (no tick state needed). */
    public boolean isCombinational() { return logic != null; }

    /** True if this gate operates on bundled cable and is inert until that lands (task #232). */
    public boolean isArray() { return category == Category.ARRAY; }

    // ---- which menu options actually apply to this gate (so the menu only shows the relevant ones) ----
    public boolean usesInterval() { return this == TIMER || this == SEQUENCER || this == STATE_CELL; }
    public boolean usesMaxCount() { return this == COUNTER; }
    public boolean usesPulse()    { return this == PULSE_FORMER || this == TIMER || this == SYNCHRONIZER; }
    public boolean usesSubtract() { return this == COMPARATOR; }

    /** ARGB accent color for the cell render + menu title bar, by category. */
    public int accentColor() {
        switch (category) {
            case SIMPLE:  return 0xFFC82020; // redstone red
            case LATCH:   return 0xFF2E8BC0; // blue
            case TIMING:  return 0xFFE0A030; // amber
            case SENSOR:  return 0xFF50BE58; // green
            case SPECIAL: return 0xFFA050D0; // purple
            case ARRAY:   return 0xFF40C0C0; // teal
            default:      return 0xFFFFFFFF;
        }
    }

    /**
     * The Project Red "surface" texture name for this gate (assets/projectred/textures/blocks/
     * integration/surface/&lt;name&gt;-0.png), or null if PR has no matching face (those fall back to
     * the bundled hand-drawn art). Lets the cell reuse PR's REAL gate textures shrunk onto the board.
     */
    public String prSurface() {
        switch (this) {
            case AND: return "and";            case OR: return "or";          case NOT: return "not";
            case NOR: return "nor";            case NAND: return "nand";      case XOR: return "xor";
            case XNOR: return "xnor";          case BUFFER: return "buffer";  case MULTIPLEXER: return "multiplexer";
            case RS_LATCH: return "rslatch";   case TOGGLE_LATCH: return "toglatch"; case TRANSPARENT_LATCH: return "translatch";
            case REPEATER: return "repeater";  case TIMER: return "time";     case COUNTER: return "count";
            case STATE_CELL: return "statecell"; case SYNCHRONIZER: return "sync"; case PULSE_FORMER: return "pulse";
            case RANDOMIZER: return "rand";    case LIGHT_SENSOR: return "lightsensor"; case RAIN_SENSOR: return "rainsensor";
            case COMPARATOR: return "comparator";
            case INVERT_CELL: return "invcell"; case BUFFER_CELL: return "buffcell"; case AND_CELL: return "andcell";
            case BUS_TRANSCEIVER: return "busxcvr"; case BUS_RANDOMIZER: return "busrand1";
            case BUS_CONVERTER: return "busconv"; case BUS_INPUT_PANEL: return "businput";
            default: return null; // SEQUENCER, NULL_CELL, STRETCHING_CELL, SEGMENT_DISPLAY, DEC_COUNTER
        }
    }

    /** Safe lookup by name for NBT (falls back to AND if an unknown/old value is read). */
    public static GateType byName(String name) {
        if (name != null) {
            try { return GateType.valueOf(name); } catch (IllegalArgumentException ignored) {}
        }
        return AND;
    }

    public GateType next() { GateType[] v = values(); return v[(ordinal() + 1) % v.length]; }
    public GateType prev() { GateType[] v = values(); return v[(ordinal() - 1 + v.length) % v.length]; }
}

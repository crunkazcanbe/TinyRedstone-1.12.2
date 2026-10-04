package com.dannyandson.tinyredstone.blocks.panelcells;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * ONE configurable Logic Gate cell that morphs into any {@link GateType} — the full Project Red
 * catalog, built custom (task #235). Output is the FRONT side (rotated by {@link #rotation}); inputs
 * are read from the sides each {@link GateType} declares it uses.
 *
 * <p>Rich per-gate options (configured in the menu, mirrored across PR/RedLogic/Integrated Dynamics
 * conventions): analog output strength, output invert, per-input invert, timing interval, counter
 * max, pulse length, comparator subtract-mode, rotation, reset.
 */
public class LogicGate implements IPanelCell {

    private GateType type = GateType.AND;

    // 1-tick output register
    private boolean output = false;
    private boolean pendingTarget = false;

    // stateful memory + edge tracking
    private boolean memState = false;
    private boolean prevBack = false, prevLeft = false, prevRight = false;

    // timing / counting state
    private int tickCounter = 0;
    private int pulseRemaining = 0;
    private int counterValue = 0;

    // ---- configurable options ----
    private int interval = 40;            // ticks — timer/sequencer/state-cell period
    private int maxCount = 10;            // counter target
    private int outputStrength = 15;      // analog output level 1..15
    private boolean invertOutput = false; // NOT-ify the result
    private boolean invBack = false, invLeft = false, invRight = false; // per-input invert
    private int pulseLength = 2;          // ticks — pulse former / timer / synchronizer pulse width
    private boolean subtractMode = false; // comparator: subtract vs compare
    /** facing: 0..3 quarter-turns CW (rotate wrench / menu) — rotates I/O + the face texture */
    private int rotation = 0;

    private static final float H = 0.25f; // slab height (comparator-style 3D)

    private Side rot(Side s) {
        Side r = s;
        for (int i = 0; i < rotation; i++) r = r.rotateYCW();
        return r;
    }

    private ResourceLocation topTexture() {
        return new ResourceLocation(TinyRedstone.MODID, "block/gate_" + type.name().toLowerCase());
    }

    @Override
    public void render(float partialTicks, int combinedLight, float alpha) {
        TextureAtlasSprite side = RenderHelper.getSprite(PanelTileRenderer.TEXTURE);
        int topLight = output ? 0xF000F0 : combinedLight; // glow when on

        GlStateManager.translate(0, 0, H);

        // top face, spun about the cell centre so the art faces the gate's current rotation
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.5f, 0.5f, 0f);
        GlStateManager.rotate(rotation * 90f, 0f, 0f, 1f);
        GlStateManager.translate(-0.5f, -0.5f, 0f);
        String pr = type.prSurface();
        if (pr != null && net.minecraftforge.fml.common.Loader.isModLoaded("projectred-integration")) {
            // reuse Project Red's REAL gate face: circuit-board base + the printed gate surface
            TextureAtlasSprite base = RenderHelper.getSprite(new ResourceLocation("projectred", "blocks/integration/base"));
            TextureAtlasSprite face = RenderHelper.getSprite(new ResourceLocation("projectred", "blocks/integration/surface/" + pr + "-0"));
            RenderHelper.drawRectangle(0, 1, 0, 1, base.getMinU(), base.getMaxU(), base.getMaxV(), base.getMinV(), topLight, 0xFFFFFF, alpha);
            GlStateManager.translate(0, 0, 0.002f);
            RenderHelper.drawRectangle(0, 1, 0, 1, face.getMinU(), face.getMaxU(), face.getMaxV(), face.getMinV(), topLight, 0xFFFFFF, alpha);
        } else {
            TextureAtlasSprite top = RenderHelper.getSprite(topTexture());
            RenderHelper.drawRectangle(0, 1, 0, 1, top.getMinU(), top.getMaxU(), top.getMaxV(), top.getMinV(), topLight, 0xFFFFFF, alpha);
        }
        GlStateManager.popMatrix();

        // four side walls
        GlStateManager.rotate(90, 1, 0, 0);
        GlStateManager.translate(0, -H, 0);
        RenderHelper.drawRectangle(0, 1, 0, H, side.getMinU(), side.getMaxU(), side.getMaxV(), side.getMinV(), combinedLight, 0xFFFFFF, alpha);
        for (int i = 0; i < 3; i++) {
            GlStateManager.rotate(90, 0, 1, 0);
            GlStateManager.translate(0, 0, 1);
            RenderHelper.drawRectangle(0, 1, 0, H, side.getMinU(), side.getMaxU(), side.getMaxV(), side.getMinV(), combinedLight, 0xFFFFFF, alpha);
        }
    }

    @Override
    public boolean neighborChanged(PanelCellPos cellPos) {
        return false; // evaluated every tick
    }

    private boolean readInput(PanelCellPos pos, Side side) {
        PanelCellNeighbor n = pos.getNeighbor(side);
        return n != null && n.getWeakRsOutput() > 0;
    }

    private int readSignal(PanelCellPos pos, Side side) {
        PanelCellNeighbor n = pos.getNeighbor(side);
        return n != null ? n.getWeakRsOutput() : 0;
    }

    private int pulseW() { return Math.max(1, pulseLength); }

    private boolean computeTarget(PanelCellPos pos) {
        boolean back  = type.usesBack  && (readInput(pos, rot(Side.BACK))  ^ invBack);
        boolean left  = type.usesLeft  && (readInput(pos, rot(Side.LEFT))  ^ invLeft);
        boolean right = type.usesRight && (readInput(pos, rot(Side.RIGHT)) ^ invRight);

        boolean result;
        if (type.isCombinational()) {
            result = type.logic.apply(back, left, right);
        } else {
            switch (type) {
                case RS_LATCH:
                    if (right) memState = false; else if (left) memState = true;
                    result = memState; break;
                case TRANSPARENT_LATCH:
                    if (right) memState = left;
                    result = memState; break;
                case TOGGLE_LATCH:
                    if (right) memState = false;
                    else if (left && !prevLeft) memState = !memState;
                    result = memState; break;
                case REPEATER:
                    result = back; break;
                case PULSE_FORMER:
                    if (back && !prevBack) pulseRemaining = pulseW();
                    result = pulseRemaining > 0; if (pulseRemaining > 0) pulseRemaining--; break;
                case TIMER:
                    if (back) { tickCounter = 0; pulseRemaining = 0; result = false; }
                    else {
                        if (++tickCounter >= interval) { tickCounter = 0; pulseRemaining = pulseW(); }
                        result = pulseRemaining > 0; if (pulseRemaining > 0) pulseRemaining--;
                    }
                    break;
                case SEQUENCER:
                    if (++tickCounter >= interval) { tickCounter = 0; memState = !memState; }
                    result = memState; break;
                case STATE_CELL:
                    if (back && !prevBack) pulseRemaining = interval;
                    result = pulseRemaining > 0; if (pulseRemaining > 0) pulseRemaining--; break;
                case COUNTER:
                    if (right) counterValue = 0;
                    else if (left && !prevLeft) { if (++counterValue > maxCount) counterValue = maxCount; }
                    result = counterValue >= maxCount; break;
                case SYNCHRONIZER:
                    if ((left && right) && (!prevLeft || !prevRight)) pulseRemaining = pulseW();
                    result = pulseRemaining > 0; if (pulseRemaining > 0) pulseRemaining--; break;
                case RANDOMIZER: {
                    if (back && !prevBack) {
                        World w = pos.getPanelTile().getWorld();
                        memState = (w != null) ? w.rand.nextBoolean() : !memState;
                    }
                    result = memState; break;
                }
                case LIGHT_SENSOR: {
                    World w = pos.getPanelTile().getWorld();
                    BlockPos p = pos.getPanelTile().getPos();
                    result = w != null && w.isDaytime() && w.canBlockSeeSky(p);
                    break;
                }
                case RAIN_SENSOR: {
                    World w = pos.getPanelTile().getWorld();
                    BlockPos p = pos.getPanelTile().getPos();
                    result = w != null && w.isRainingAt(p);
                    break;
                }
                case COMPARATOR: {
                    int b = readSignal(pos, rot(Side.BACK)), r = readSignal(pos, rot(Side.RIGHT));
                    result = subtractMode ? (b - r) > 0 : (b > 0 && b >= r);
                    break;
                }
                default: // ARRAY / bus cells — need bundled cable (#232)
                    result = false;
            }
        }

        if (invertOutput) result = !result;

        prevBack = back; prevLeft = left; prevRight = right;
        return result;
    }

    @Override
    public boolean tick(PanelCellPos cellPos) {
        boolean changed = (pendingTarget != output);
        output = pendingTarget;
        pendingTarget = computeTarget(cellPos);
        return changed;
    }

    private int strength() { return Math.max(1, Math.min(15, outputStrength)); }

    @Override
    public int getWeakRsOutput(Side outputDirection) {
        return (outputDirection == rot(Side.FRONT) && output) ? strength() : 0;
    }

    @Override
    public int getStrongRsOutput(Side outputDirection) {
        return (outputDirection == rot(Side.FRONT) && output) ? strength() : 0;
    }

    @Override
    public boolean hasActivation(EntityPlayer player) { return true; }

    @Override
    public boolean onBlockActivated(PanelCellPos cellPos, PanelCellSegment segmentClicked, EntityPlayer player) {
        net.minecraft.item.Item held = player.getHeldItemMainhand().getItem();

        // COLORED wrench -> open the full options menu
        if (held == com.dannyandson.tinyredstone.setup.Registration.TINY_COLOR_SELECTOR) {
            if (cellPos.getPanelTile().getLevel().isRemote) {
                com.dannyandson.tinyredstone.gui.LogicGateGUI.open(cellPos.getPanelTile(), cellPos.getIndex(), this);
            }
            return false;
        }
        // ROTATE wrench -> turn the gate 90°
        if (held == com.dannyandson.tinyredstone.setup.Registration.REDSTONE_WRENCH) {
            this.rotation = (this.rotation + 1) % 4;
            return true;
        }
        // bare hand -> quick-swap the gate type (sneak = previous)
        this.type = player.isSneaking() ? this.type.prev() : this.type.next();
        resetRuntime();
        return true;
    }

    private void resetRuntime() {
        output = pendingTarget = false;
        memState = prevBack = prevLeft = prevRight = false;
        tickCounter = pulseRemaining = counterValue = 0;
    }

    // ---- config accessors (menu + sync) ----
    public GateType getGateType() { return type; }
    public int getInterval() { return interval; }
    public int getMaxCount() { return maxCount; }
    public int getOutputStrength() { return outputStrength; }
    public boolean isInvertOutput() { return invertOutput; }
    public boolean isInvBack() { return invBack; }
    public boolean isInvLeft() { return invLeft; }
    public boolean isInvRight() { return invRight; }
    public int getPulseLength() { return pulseLength; }
    public boolean isSubtractMode() { return subtractMode; }
    public int getRotation() { return rotation; }

    /** Apply the full option set from the menu (and reset runtime so it behaves predictably). */
    public void applyConfig(GateType t, int interval, int maxCount, int outputStrength, boolean invertOutput,
                            boolean invBack, boolean invLeft, boolean invRight, int pulseLength,
                            boolean subtractMode, int rotation) {
        this.type = (t != null) ? t : this.type;
        this.interval = Math.max(1, interval);
        this.maxCount = Math.max(1, maxCount);
        this.outputStrength = Math.max(1, Math.min(15, outputStrength));
        this.invertOutput = invertOutput;
        this.invBack = invBack; this.invLeft = invLeft; this.invRight = invRight;
        this.pulseLength = Math.max(1, pulseLength);
        this.subtractMode = subtractMode;
        this.rotation = ((rotation % 4) + 4) % 4;
        resetRuntime();
    }

    @Override
    public NBTTagCompound writeNBT() {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("type", type.name());
        nbt.setBoolean("output", output);
        nbt.setBoolean("pending", pendingTarget);
        nbt.setBoolean("memState", memState);
        nbt.setBoolean("prevBack", prevBack);
        nbt.setBoolean("prevLeft", prevLeft);
        nbt.setBoolean("prevRight", prevRight);
        nbt.setInteger("tickCounter", tickCounter);
        nbt.setInteger("pulseRemaining", pulseRemaining);
        nbt.setInteger("counterValue", counterValue);
        nbt.setInteger("interval", interval);
        nbt.setInteger("maxCount", maxCount);
        nbt.setInteger("outputStrength", outputStrength);
        nbt.setBoolean("invertOutput", invertOutput);
        nbt.setBoolean("invBack", invBack);
        nbt.setBoolean("invLeft", invLeft);
        nbt.setBoolean("invRight", invRight);
        nbt.setInteger("pulseLength", pulseLength);
        nbt.setBoolean("subtractMode", subtractMode);
        nbt.setInteger("rotation", rotation);
        return nbt;
    }

    @Override
    public void readNBT(NBTTagCompound nbt) {
        this.type = GateType.byName(nbt.getString("type"));
        this.output = nbt.getBoolean("output");
        this.pendingTarget = nbt.getBoolean("pending");
        this.memState = nbt.getBoolean("memState");
        this.prevBack = nbt.getBoolean("prevBack");
        this.prevLeft = nbt.getBoolean("prevLeft");
        this.prevRight = nbt.getBoolean("prevRight");
        this.tickCounter = nbt.getInteger("tickCounter");
        this.pulseRemaining = nbt.getInteger("pulseRemaining");
        this.counterValue = nbt.getInteger("counterValue");
        if (nbt.hasKey("interval")) this.interval = nbt.getInteger("interval");
        if (nbt.hasKey("maxCount")) this.maxCount = nbt.getInteger("maxCount");
        if (nbt.hasKey("outputStrength")) this.outputStrength = nbt.getInteger("outputStrength");
        this.invertOutput = nbt.getBoolean("invertOutput");
        this.invBack = nbt.getBoolean("invBack");
        this.invLeft = nbt.getBoolean("invLeft");
        this.invRight = nbt.getBoolean("invRight");
        if (nbt.hasKey("pulseLength")) this.pulseLength = nbt.getInteger("pulseLength");
        this.subtractMode = nbt.getBoolean("subtractMode");
        this.rotation = nbt.getInteger("rotation");
    }

    @Override
    public PanelCellVoxelShape getShape() {
        return new PanelCellVoxelShape(new Vec3d(0d, 0d, 0d), new Vec3d(1d, 0.2d, 1d));
    }
}

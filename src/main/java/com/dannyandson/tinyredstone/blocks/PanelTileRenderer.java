package com.dannyandson.tinyredstone.blocks;

import com.dannyandson.tinyredstone.TinyRedstone;
import com.dannyandson.tinyredstone.api.IPanelCell;
import com.dannyandson.tinyredstone.blocks.panelcells.GhostRenderer;
import com.dannyandson.tinyredstone.blocks.panelcells.RedstoneDust;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import javax.annotation.CheckForNull;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * 1.12.2 port of Tiny Redstone's panel renderer: a TileEntitySpecialRenderer (1.16 used
 * TileEntityRenderer + MatrixStack/IRenderTypeBuffer). All matrix ops are now immediate-mode
 * GlStateManager and drawing goes through the converted {@link RenderHelper} (Tessellator).
 */
public class PanelTileRenderer extends TileEntitySpecialRenderer<PanelTile> {

    public static ResourceLocation TEXTURE = new ResourceLocation(TinyRedstone.MODID, "block/redstone_panel");
    public static ResourceLocation TEXTURE_CRASHED = new ResourceLocation(TinyRedstone.MODID, "block/redstone_panel_crashed");

    private static final float scale = 0.125f;
    private static final float t2X = 0.0f;
    private static final float t2Y = -1.0f;
    private static final float t2Z = 0.0f;
    private static final float rotation1 = 270f;

    private final double cellSize = 1d / 8d;

    @Override
    public void render(PanelTile tileEntity, double x, double y, double z, float partialTicks, int destroyStage, float alphaParam) {
        boolean hasBase = tileEntity.hasBase();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();

        int combinedLight = (tileEntity.getWorld() != null)
                ? tileEntity.getWorld().getCombinedLight(tileEntity.getPos(), 0) : 0xF000F0;

        switch (tileEntity.getBlockState().getValue(PanelBlock.FACING)) {
            case UP:
                GlStateManager.rotate(180F, 1F, 0F, 0F); GlStateManager.translate(0, -1, -1); break;
            case NORTH:
                GlStateManager.rotate(90F, 1F, 0F, 0F); GlStateManager.translate(0, 0, -1); break;
            case EAST:
                GlStateManager.rotate(90F, 0F, 0F, 1F); GlStateManager.translate(0, -1, 0); break;
            case SOUTH:
                GlStateManager.rotate(-90F, 1F, 0F, 0F); GlStateManager.translate(0, -1, 0); break;
            case WEST:
                GlStateManager.rotate(-90F, 0F, 0F, 1F); GlStateManager.translate(-1, 0, 0); break;
            default: break;
        }

        TextureAtlasSprite sprite = RenderHelper.getSprite(PanelTileRenderer.TEXTURE);

        if (tileEntity.isCovered()) {
            GlStateManager.pushMatrix();
            tileEntity.panelCover.render(partialTicks, combinedLight, tileEntity.getColor());
            GlStateManager.popMatrix();
        } else {
            if (hasBase) {
                int color = tileEntity.getColor();
                GlStateManager.pushMatrix();
                GlStateManager.rotate(270F, 1F, 0F, 0F); GlStateManager.translate(0, -1, 0.125);
                RenderHelper.drawRectangle(0, 1, 0, 1, sprite, combinedLight, color, 1.0f);

                GlStateManager.rotate(90F, 1F, 0F, 0F); GlStateManager.translate(0, -0.125, 0);
                RenderHelper.drawRectangle(0, 1, 0, .125f, sprite, combinedLight, color, 1.0f);

                GlStateManager.rotate(90F, 0F, 1F, 0F); GlStateManager.translate(0, 0, 1);
                RenderHelper.drawRectangle(0, 1, 0, .125f, sprite, combinedLight, color, 1.0f);

                GlStateManager.rotate(90F, 0F, 1F, 0F); GlStateManager.translate(0, 0, 1);
                RenderHelper.drawRectangle(0, 1, 0, .125f, sprite, combinedLight, color, 1.0f);

                GlStateManager.rotate(90F, 0F, 1F, 0F); GlStateManager.translate(0, 0, 1);
                RenderHelper.drawRectangle(0, 1, 0, .125f, sprite, combinedLight, color, 1.0f);

                GlStateManager.rotate(90F, 1F, 0F, 0F); GlStateManager.translate(0, -1, 0);
                RenderHelper.drawRectangle(0, 1, 0, 1, sprite, combinedLight, color, 1.0f);
                GlStateManager.popMatrix();
            }

            List<PanelCellPos> positions = tileEntity.getCellPositions();
            for (PanelCellPos pos : positions) {
                IPanelCell panelCell = pos.getIPanelCell();
                if (panelCell != null) {
                    renderCell(pos, (tileEntity.isCrashed()) ? 0 : combinedLight,
                            (tileEntity.isCrashed()) ? 0.5f : 1.0f, hasBase);
                }
            }

            if (tileEntity.panelCellGhostPos != null) {
                renderCell(tileEntity.panelCellGhostPos, combinedLight, 0.5f, hasBase);
            }
        }

        if (tileEntity.isCrashed() || tileEntity.isOverflown()) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0, 0.126, 1);
            GlStateManager.rotate(rotation1, 1F, 0F, 0F);
            sprite = RenderHelper.getSprite(TEXTURE_CRASHED);
            RenderHelper.drawRectangle(0, 1, 0, 1, sprite, combinedLight, 0.9f);
            GlStateManager.popMatrix();
        }

        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void renderCell(PanelCellPos pos, int combinedLight, float alpha, boolean hasBase) {
        GlStateManager.pushMatrix();

        GlStateManager.translate(cellSize * (double) pos.getRow(),
                ((hasBase) ? 0.125 : 0) + (pos.getLevel() * 0.125), cellSize * (pos.getColumn()));
        GlStateManager.rotate(rotation1, 1F, 0F, 0F);

        Side facing = pos.getCellFacing();
        if (facing == Side.LEFT) {
            GlStateManager.translate(0, -cellSize, 0); GlStateManager.rotate(90F, 0F, 0F, 1F);
        } else if (facing == Side.BACK) {
            GlStateManager.translate(cellSize, -cellSize, 0); GlStateManager.rotate(180F, 0F, 0F, 1F);
        } else if (facing == Side.RIGHT) {
            GlStateManager.translate(cellSize, 0, 0); GlStateManager.rotate(270F, 0F, 0F, 1F);
        } else if (facing == Side.BOTTOM) {
            GlStateManager.translate(0, -cellSize, 0); GlStateManager.rotate(-90F, 1F, 0F, 0F);
        } else if (facing == Side.TOP) {
            GlStateManager.translate(0, 0, cellSize); GlStateManager.rotate(90F, 1F, 0F, 0F);
        }

        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate(t2X, t2Y, t2Z);

        pos.getIPanelCell().render(0f, combinedLight, alpha);

        GlStateManager.popMatrix();
    }

    public static void register() {
        ClientRegistry.bindTileEntitySpecialRenderer(PanelTile.class, new PanelTileRenderer());
    }

    @CheckForNull
    public static PanelCellGhostPos getPlayerLookingAtCell(PanelTile panelTile) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        BlockPos blockPos = panelTile.getPos();
        if (player != null) {
            if (blockPos.distanceSq(player.posX, player.posY, player.posZ) < 16.0d) {
                RayTraceResult blockHitResult = panelTile.getPlayerCollisionHitResult(player);
                if (blockHitResult == null) return null;
                PanelCellPos cellPos1 = PosInPanelCell.fromHitVec(panelTile, blockPos, blockHitResult);
                panelTile.panelCellHovering = cellPos1;

                if (blockPos.distanceSq(player.posX, player.posY, player.posZ) < 9.0d) {

                    if (PanelBlock.isPanelCellItem(player.getHeldItemMainhand().getItem())) {
                        if (cellPos1 != null) {
                            PanelCellPos cellPos = cellPos1;
                            if (cellPos.getIPanelCell() != null && (!cellPos.getIPanelCell().hasActivation(player) || player.isSneaking())) {
                                cellPos = cellPos.offset(panelTile.getSideFromDirection(blockHitResult.sideHit));
                            }
                            if (cellPos != null && cellPos.getIPanelCell() == null) {
                                try {
                                    IPanelCell panelCell = (IPanelCell) PanelBlock.getPanelCellClassFromItem(player.getHeldItemMainhand().getItem()).getConstructors()[0].newInstance();
                                    Side rotationLock = RotationLock.getRotationLock();
                                    Side cellFacing = rotationLock == null ?
                                            panelTile.getSideFromDirection(panelTile.getPlayerDirectionFacing(player, panelCell.canPlaceVertical()))
                                            : rotationLock;

                                    if (panelCell.needsSolidBase()) {
                                        Side attachingSideDir = panelTile.getSideFromDirection(blockHitResult.sideHit).getOpposite();
                                        Side attachingSideRel = (attachingSideDir == Side.TOP || attachingSideDir == Side.BOTTOM) ? attachingSideDir : Side.FRONT;

                                        if (!panelCell.canAttachToBaseOnSide(attachingSideRel) || (
                                                !cellPos1.equals(cellPos) && (
                                                        cellPos1.getIPanelCell() == null
                                                                || !cellPos1.getIPanelCell().isPushable()
                                                )
                                        )) {
                                            return null;
                                        } else {
                                            panelCell.setBaseSide(attachingSideRel);
                                            if (attachingSideRel == Side.FRONT)
                                                cellFacing = attachingSideDir;
                                        }
                                    }

                                    panelCell.onPlace(cellPos, player);
                                    return PanelCellGhostPos.fromPosInPanelCell(cellPos, panelCell, cellFacing);
                                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                    TinyRedstone.LOGGER.error("Exception thrown when attempting to draw ghost cell: " + e.getMessage());
                                }
                            }
                        }
                    }

                    PosInPanelCell posInPanelCell = PosInPanelCell.fromHitVec(panelTile, blockPos, blockHitResult);
                    if (posInPanelCell != null && posInPanelCell.getIPanelCell() instanceof RedstoneDust) {
                        PanelCellSegment segmentHovering = posInPanelCell.getSegment();
                        return PanelCellGhostPos.fromPosInPanelCell(
                                posInPanelCell, new GhostRenderer(segmentHovering), posInPanelCell.getCellFacing());
                    }
                }
            }
        }
        return null;
    }
}

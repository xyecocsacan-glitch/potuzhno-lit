package ru.potuzhnolit.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import ru.potuzhnolit.FlyingMachineEntity;
import ru.potuzhnolit.PotuzhnoLitMod;

/** Client models replace the old server-side block-display placeholders. */
public final class MachineRenderer extends EntityRenderer<FlyingMachineEntity, MachineRenderer.State> {
    private static final Identifier DRONE = PotuzhnoLitMod.id("textures/entity/drone.png");
    private static final Identifier PLANE = PotuzhnoLitMod.id("textures/entity/potuzhno_lit.png");
    private final MachineModel drone = new MachineModel(MachineModel.createDroneLayer().bakeRoot(), true);
    private final MachineModel plane = new MachineModel(MachineModel.createPlaneLayer().bakeRoot(), false);

    public MachineRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = .65f;
    }
    public static final class State extends EntityRenderState {
        public int variant;
        public float yaw, pitch;
        public boolean moving;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(FlyingMachineEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.variant = entity.getVariant();
        state.yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        double dx = entity.getX() - entity.xo, dy = entity.getY() - entity.yo, dz = entity.getZ() - entity.zo;
        state.moving = dx * dx + dy * dy + dz * dz > .0001 || entity.isVehicle();
    }
    @Override public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        if (state.variant == 0 && state.moving)
            poses.mulPose(Axis.XP.rotationDegrees(Mth.clamp(state.pitch, -10, 10)));
        // Model coordinates have negative Y upward; local +Z is the nose.
        poses.scale(-1, -1, 1);
        MachineModel model = state.variant == 0 ? drone : plane;
        Identifier texture = state.variant == 0 ? DRONE : PLANE;
        collector.submitModel(model, state, poses, model.renderType(texture),
            state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
        poses.popPose();
        super.submit(state, poses, collector, camera);
    }
}

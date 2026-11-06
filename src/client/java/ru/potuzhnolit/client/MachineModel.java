package ru.potuzhnolit.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Original stylized game models. 16 model pixels equal one Minecraft block. */
public final class MachineModel extends EntityModel<MachineRenderer.State> {
    private final ModelPart[] rotors;
    private final boolean drone;

    public MachineModel(ModelPart root, boolean drone) {
        super(root);
        this.drone = drone;
        rotors = new ModelPart[drone ? 4 : 1];
        for (int i = 0; i < rotors.length; i++) rotors[i] = root.getChild("rotor_" + i);
    }

    @Override public void setupAnim(MachineRenderer.State state) {
        super.setupAnim(state);
        float phase = state.ageInTicks * (state.moving ? 2.4f : .18f);
        for (int i = 0; i < rotors.length; i++) {
            if (drone) rotors[i].yRot = phase * (i % 2 == 0 ? 1 : -1) + i * .7f;
            else rotors[i].zRot = phase;
        }
    }

    private static void box(PartDefinition parent, String name, int material,
                            float x, float y, float z, float w, float h, float d) {
        // Each material has a 64x32 tile within a 128x128 atlas.
        if (material < 0 || material > 7 || w <= 0 || h <= 0 || d <= 0
                || 2 * (w + d) > 64 || h + d > 32)
            throw new IllegalArgumentException("Invalid vehicle cube/UV: " + name);
        parent.addOrReplaceChild(name, CubeListBuilder.create()
            .texOffs((material % 2) * 64, (material / 2) * 32)
            .addBox(x, y, z, w, h, d), PartPose.ZERO);
    }
    private static PartDefinition pivot(PartDefinition parent, String name,
                                       float x, float y, float z, float rx, float ry, float rz) {
        return parent.addOrReplaceChild(name, CubeListBuilder.create(),
            PartPose.offsetAndRotation(x, y, z, rx, ry, rz));
    }

    public static LayerDefinition createDroneLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Carbon frame, blue battery, dark pod, camera lens and four small rotors.
        box(root, "lower_frame", 0, -4, -7, -6, 8, 1, 12);
        box(root, "upper_frame", 0, -4, -10, -6, 8, 1, 12);
        for (int x : new int[]{-3, 3}) for (int z : new int[]{-4, 4})
            box(root, "spacer_" + x + "_" + z, 3, x - .4f, -9, z - .4f, .8f, 2, .8f);
        box(root, "battery", 1, -2.7f, -14, -4.5f, 5.4f, 4, 9);
        for (int z : new int[]{-3, 2}) {
            box(root, "strap_top_" + z, 2, -2.85f, -14.2f, z, 5.7f, .35f, 1);
            box(root, "strap_l_" + z, 2, -2.95f, -14, z, .4f, 4, 1);
            box(root, "strap_r_" + z, 2, 2.55f, -14, z, .4f, 4, 1);
        }
        box(root, "battery_label", 3, -1.5f, -14.25f, -.8f, 3, .15f, 1.6f);
        box(root, "pod", 2, -2.5f, -5.5f, -5, 5, 4, 10);
        box(root, "pod_center", 0, -3, -5, -4, 6, 3, 8);
        box(root, "pod_cap", 3, -2, -5, 5, 4, 3, 1.5f);
        box(root, "pod_strap", 2, -3.1f, -6, -1, 6.2f, 5, 1);
        box(root, "camera_shell", 0, -2, -9.5f, 6, 4, 3.5f, 2);
        box(root, "lens_rim", 3, -1.5f, -9.1f, 8, 3, 2.7f, .5f);
        box(root, "lens", 4, -1.1f, -8.8f, 8.5f, 2.2f, 2.1f, .5f);
        box(root, "status_led", 5, 3.6f, -8.5f, 3, .5f, .8f, 1);
        PartDefinition aerial = pivot(root, "antenna", 2.8f, -9, -5.5f, -.3f, 0, -.15f);
        box(aerial, "stem", 2, -.22f, -7, -.22f, .44f, 7, .44f);
        box(aerial, "tip", 3, -.6f, -7.8f, -.6f, 1.2f, .8f, 1.2f);
        PartDefinition wire = pivot(root, "cable", -2.8f, -9, -4.5f, -.6f, 0, .1f);
        box(wire, "rise", 2, 0, -4, 0, .3f, 4, .3f);
        box(wire, "return", 2, 0, -4, 0, 2, .3f, .3f);
        int index = 0;
        for (int sx : new int[]{-1, 1}) for (int sz : new int[]{-1, 1}) {
            float mx = sx * 9.5f, mz = sz * 9;
            PartDefinition arm = pivot(root, "arm_" + index, 0, -8.2f, 0,
                0, (float) Math.atan2(mx, mz), 0);
            box(arm, "beam", 0, -.8f, 0, 1, 1.6f, 1.1f, 12.4f);
            box(root, "motor_base_" + index, 3, mx - 1.5f, -9, mz - 1.5f, 3, 1, 3);
            box(root, "motor_" + index, 2, mx - 1.25f, -11, mz - 1.25f, 2.5f, 2, 2.5f);
            PartDefinition rotor = pivot(root, "rotor_" + index, mx, -11.3f, mz, 0, 0, 0);
            for (int blade = 0; blade < 3; blade++) {
                PartDefinition b = pivot(rotor, "blade_" + blade, 0, 0, 0,
                    0, (float) (blade * Math.PI * 2 / 3), 0);
                box(b, "blade", 0, -.55f, -.15f, .4f, 1.1f, .3f, 4.2f);
                box(b, "tip", 3, -.5f, -.16f, 3.8f, 1, .32f, .8f);
            }
            box(rotor, "hub", 3, -.5f, -.4f, -.5f, 1, .7f, 1);
            index++;
        }
        return LayerDefinition.create(mesh, 128, 128);
    }

    public static LayerDefinition createPlaneLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Blue/yellow barrel from the reference, open cockpit, wooden seat, black skids.
        box(root, "floor", 6, -5, -6, -10, 10, 2, 20);
        for (int sx : new int[]{-1, 1}) {
            box(root, "lower_side_" + sx, 6, sx < 0 ? -7 : 5, -10, -10, 2, 4, 20);
            box(root, "upper_side_" + sx, 1, sx < 0 ? -7 : 5, -15, -10, 2, 5, 20);
            box(root, "rim_" + sx, 3, sx < 0 ? -7.2f : 4.8f, -15.3f, -10, 2.4f, .5f, 20);
            box(root, "band_" + sx, 2, sx < 0 ? -7.3f : 7, -14.7f, -6, .3f, 10, 1);
            box(root, "skid_" + sx, 2, sx * 8 - .55f, -1.1f, -13, 1.1f, 1.1f, 28);
            for (int z : new int[]{-7, 7})
                box(root, "strut_" + sx + "_" + z, 3, sx * 6 - .45f, -5, z, .9f, 4, .9f);
            box(root, "gun_mount_" + sx, 3, sx < 0 ? -10.8f : 7, -11.8f, 3, 3.8f, 1, 3);
            box(root, "gun_body_" + sx, 0, sx * 10.4f - 1.2f, -12.4f, 3, 2.4f, 2, 6);
            box(root, "barrel_" + sx, 2, sx * 10.4f - .45f, -11.65f, 9, .9f, .9f, 13.5f);
            box(root, "muzzle_" + sx, 3, sx * 10.4f - .65f, -11.85f, 21, 1.3f, 1.3f, 1.5f);
            for (int z = 5; z <= 8; z++)
                box(root, "ammo_" + sx + "_" + z, 6, sx * 10.4f - 1.5f, -11, z, 3, .6f, .45f);
        }
        for (int z : new int[]{-7, 7})
            box(root, "crossbar_" + z, 2, -8.55f, -1.8f, z, 17.1f, .8f, 1);
        box(root, "front_lower", 6, -5, -10, 10, 10, 6, 3);
        box(root, "front_upper", 1, -5, -15, 10, 10, 5, 3);
        box(root, "front_mid_blue", 1, -7, -12, 10, 14, 2, 3);
        box(root, "front_mid_yellow", 6, -7, -10, 10, 14, 3, 3);
        box(root, "rear_lower", 6, -5, -10, -12, 10, 6, 2);
        box(root, "rear_upper", 1, -5, -15, -12, 10, 5, 2);
        for (int z : new int[]{-4, -2, 0, 2})
            box(root, "seat_plank_" + z, 7, -5, -6.3f, z, 10, .7f, 1.8f);
        box(root, "seat_back", 7, -4.5f, -14, -5.5f, 9, 8, 1);
        box(root, "seat_frame", 2, -5, -14.5f, -6, 10, .6f, 1.6f);
        box(root, "rear_board", 7, -8, -15.8f, -10, 16, .8f, 4);
        box(root, "engine", 0, -2, -11, 13, 4, 4, 3);
        box(root, "shaft", 3, -.6f, -9.6f, 16, 1.2f, 1.2f, 2);
        PartDefinition rotor = pivot(root, "rotor_0", 0, -9, 17.8f, 0, 0, 0);
        box(rotor, "propeller_a", 2, -.75f, -9, -.25f, 1.5f, 18, .5f);
        box(rotor, "propeller_b", 0, -9, -.75f, -.2f, 18, 1.5f, .4f);
        box(rotor, "hub", 3, -1.3f, -1.3f, -.7f, 2.6f, 2.6f, 1.7f);
        box(root, "stick", 2, -.35f, -11, 6, .7f, 5, .7f);
        box(root, "handle", 3, -2, -11.5f, 5.8f, 4, .7f, 1);
        return LayerDefinition.create(mesh, 128, 128);
    }
}

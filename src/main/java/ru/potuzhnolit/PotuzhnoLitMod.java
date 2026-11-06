package ru.potuzhnolit;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class PotuzhnoLitMod implements ModInitializer {
    public static final String MOD_ID = "potuzhnolit";
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(MOD_ID, path); }
    public static final EntityType<FlyingMachineEntity> MACHINE = Registry.register(
        BuiltInRegistries.ENTITY_TYPE, id("machine"),
        EntityType.Builder.<FlyingMachineEntity>of(FlyingMachineEntity::new, MobCategory.MISC)
            .sized(1.8f, 0.9f).clientTrackingRange(10).updateInterval(1)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id("machine")))
    );
    private static Item machineItem(String key, int variant) {
        Identifier id = id(key);
        return Registry.register(BuiltInRegistries.ITEM, id,
            new MachineItem(variant, new Item.Properties().stacksTo(1)
                .setId(ResourceKey.create(Registries.ITEM, id))));
    }
    public static final Item DRONE = machineItem("drone", 0);
    public static final Item PLANE = machineItem("potuzhno_lit", 1);

    @Override public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(ControlPayload.TYPE, ControlPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DroneViewPayload.TYPE, DroneViewPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ControlPayload.TYPE, (payload, context) ->
            context.server().execute(() -> RemoteControl.handle(context.player(), payload)));
        ServerTickEvents.END_SERVER_TICK.register(RemoteControl::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RemoteControl.stop(handler.player, false));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RemoteControl.clear());
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.accept(DRONE);
            entries.accept(PLANE);
        });
    }
}

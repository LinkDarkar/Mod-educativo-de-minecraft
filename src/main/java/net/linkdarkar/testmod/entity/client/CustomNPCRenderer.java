package net.linkdarkar.testmod.entity.client;

import net.linkdarkar.testmod.TestMod;
import net.linkdarkar.testmod.entity.custom.CustomNPCEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class CustomNPCRenderer extends MobEntityRenderer <CustomNPCEntity, PlayerEntityModel<CustomNPCEntity>> {
    public CustomNPCRenderer (EntityRendererFactory.Context ctx) {
        super (ctx, new PlayerEntityModel<>(ctx.getPart(net.minecraft.client.render.entity.model.EntityModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public Identifier getTexture(CustomNPCEntity entity) {
        return Identifier.of(TestMod.MOD_ID, "textures/skins/reimu-hakurei.png");
    }
}

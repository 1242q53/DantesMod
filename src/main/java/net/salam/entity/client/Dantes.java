package net.salam.entity.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.model.SinglePartEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import net.salam.entity.animations.ModAnimations;
import net.salam.entity.custom.DantesEntity;

// ✅ Добавлен ModelWithArms — обязателен для HeldItemFeatureRenderer
public class Dantes<T extends DantesEntity> extends SinglePartEntityModel<T> implements ModelWithArms {

	private final ModelPart dantes;
	private final ModelPart head;
	private final ModelPart bone;
	private final ModelPart body;
	private final ModelPart left_arm;
	private final ModelPart right_arm;
	private final ModelPart left_leg;
	private final ModelPart right_leg;

	public Dantes(ModelPart root) {
		this.dantes    = root.getChild("dantes");
		this.head      = this.dantes.getChild("head");
		this.bone      = this.head.getChild("bone");
		this.body      = this.dantes.getChild("body");
		this.left_arm  = this.dantes.getChild("left_arm");
		this.right_arm = this.dantes.getChild("right_arm");
		this.left_leg  = this.dantes.getChild("left_leg");
		this.right_leg = this.dantes.getChild("right_leg");
	}

	public static TexturedModelData getTexturedModelData() {
		ModelData modelData = new ModelData();
		ModelPartData modelPartData = modelData.getRoot();
		ModelPartData dantes = modelPartData.addChild("dantes", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData head = dantes.addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
		ModelPartData bone = head.addChild("bone", ModelPartBuilder.create(), ModelTransform.pivot(0.0F, 0.0F, 0.0F));
		ModelPartData body = dantes.addChild("body", ModelPartBuilder.create().uv(16, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(0.0F, 0.0F, 0.0F));

		ModelPartData left_arm = dantes.addChild("left_arm", ModelPartBuilder.create()
						.uv(40, 16).mirrored().cuboid(-1.0F, -1.0F, -2.0F, 4.0F, 11.0F, 4.0F, new Dilation(0.0F)).mirrored(false)
						.uv(40, 16).mirrored().cuboid(-1.0F, -2.0F, -2.0F, 4.0F,  1.0F, 4.0F, new Dilation(0.0F)).mirrored(false),
				ModelTransform.pivot(5.0F, 2.0F, 0.0F));

		ModelPartData right_arm = dantes.addChild("right_arm", ModelPartBuilder.create()
						.uv(40, 16).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F)),
				ModelTransform.pivot(-5.0F, 2.0F, 0.0F));

		ModelPartData left_leg  = dantes.addChild("left_leg",  ModelPartBuilder.create().uv(0, 16).mirrored().cuboid(-1.9F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F)).mirrored(false), ModelTransform.pivot( 1.9F, 12.0F, 0.0F));
		ModelPartData right_leg = dantes.addChild("right_leg", ModelPartBuilder.create().uv(0, 16).cuboid(-2.1F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, new Dilation(0.0F)), ModelTransform.pivot(-1.9F, 12.0F, 0.0F));

		return TexturedModelData.of(modelData, 64, 64);
	}

	@Override
	public void setAngles(DantesEntity entity, float limbSwing, float limbSwingAmount,
						  float ageInTicks, float netHeadYaw, float headPitch) {
		this.getPart().traverse().forEach(ModelPart::resetTransform);
		this.setHeadAngles(netHeadYaw, headPitch);

		this.animateMovement(ModAnimations.walk, limbSwing, limbSwingAmount, 2f, 2f);
		this.updateAnimation(entity.idleAnimationState,   ModAnimations.idle,         ageInTicks, 1f);
		this.updateAnimation(entity.attackAnimationState, ModAnimations.DANTES_PUNCH,  ageInTicks, 1f);

		if (entity.isShooting()) {
			entity.shootAnimationState.startIfNotRunning(entity.age);
		} else {
			entity.shootAnimationState.stop();
		}
		this.updateAnimation(entity.shootAnimationState, ModAnimations.DANTES_SHOOT, ageInTicks, 1f);
	}

	private void setHeadAngles(float headYaw, float headPitch) {
		headYaw   = MathHelper.clamp(headYaw,   -30.0F, 30.0F);
		headPitch = MathHelper.clamp(headPitch, -25.0F, 45.0F);
		this.head.yaw   = headYaw   * 0.017453292F;
		this.head.pitch = headPitch * 0.017453292F;
	}

	// ✅ Обязательные методы ModelWithArms
	// Указывают рендереру предмета куда "прикрепить" пистолет
	@Override
	public void setArmAngle(Arm arm, MatrixStack matrices) {
		ModelPart armPart = arm == Arm.RIGHT ? this.right_arm : this.left_arm;
		armPart.rotate(matrices);
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumer vertexConsumer,
					   int light, int overlay, float red, float green, float blue, float alpha) {
		dantes.render(matrices, vertexConsumer, light, overlay, red, green, blue, alpha);
	}

	@Override
	public ModelPart getPart() {
		return dantes;
	}
}
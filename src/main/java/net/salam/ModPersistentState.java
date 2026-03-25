package net.salam; // ← было com.salam — вот причина красного!

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class ModPersistentState extends PersistentState {

    public boolean peterburgSpawned = false;

    private static final String SAVE_KEY = "salam_peterburg_state";

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("peterburgSpawned", peterburgSpawned);
        return nbt;
    }

    public static ModPersistentState readNbt(NbtCompound nbt) {
        ModPersistentState state = new ModPersistentState();
        state.peterburgSpawned = nbt.getBoolean("peterburgSpawned");
        return state;
    }

    public static ModPersistentState getServerState(MinecraftServer server) {
        PersistentStateManager manager =
                server.getWorld(World.OVERWORLD).getPersistentStateManager();

        return manager.getOrCreate(
                new PersistentState.Type<>(
                        ModPersistentState::new,
                        ModPersistentState::readNbt,
                        null
                ),
                SAVE_KEY
        );
    }
}
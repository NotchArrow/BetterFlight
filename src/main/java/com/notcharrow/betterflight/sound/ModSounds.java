package com.notcharrow.betterflight.sound;

import com.notcharrow.betterflight.BetterFlight;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final Identifier FLAP_ID = new Identifier(BetterFlight.MODID, "betterflight.flap");
    public static final Identifier BOOST_ID = new Identifier(BetterFlight.MODID, "betterflight.boost");
    
    public static final SoundEvent FLAP = SoundEvent.of(FLAP_ID);
    public static final SoundEvent BOOST = SoundEvent.of(BOOST_ID);

    public static void register() {
        Registry.register(Registries.SOUND_EVENT, FLAP_ID, FLAP);
        Registry.register(Registries.SOUND_EVENT, BOOST_ID, BOOST);
    }
}
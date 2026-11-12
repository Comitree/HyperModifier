package com.aritxonly.myhypermodifier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Read-only access to a legacy static setting or a live Settings.Int/Bool value. */
final class HyperMusicCoverSetting {
    private final Field entry, value;

    HyperMusicCoverSetting(Field entry, Field value, Class<?> type) throws NoSuchFieldException {
        int flags = value.getModifiers();
        if (value.getType() != type || Modifier.isFinal(flags)
                || (entry == null ? !Modifier.isStatic(flags)
                : Modifier.isStatic(flags) || !Modifier.isVolatile(flags)
                    || !Modifier.isStatic(entry.getModifiers())
                    || entry.getType() != value.getDeclaringClass()))
            throw new NoSuchFieldException("HMC setting layout changed: " + value);
        if (entry != null) entry.setAccessible(true);
        value.setAccessible(true);
        this.entry = entry;
        this.value = value;
    }

    private Object receiver() throws IllegalAccessException {
        if (entry == null) return null;
        Object current = entry.get(null);
        if (current == null) throw new IllegalAccessException("HMC setting object not initialized");
        return current;
    }

    int getInt() throws IllegalAccessException { return value.getInt(receiver()); }
    boolean getBoolean() throws IllegalAccessException { return value.getBoolean(receiver()); }
}

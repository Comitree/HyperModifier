package com.aritxonly.myhypermodifier;

import android.graphics.RectF;
import android.view.View;
import org.junit.Test;
import static org.junit.Assert.*;

public class HyperMusicCoverAdapterTest {
    // Helpers are deliberately named like R8 output; the adapter only touches retained Main.
    static class q { static boolean a = true; }
    static class RetainedMain {
        static View sContainer;
        static Float sHoldY = 700f;
        static int sFpAvoid = 2;
        static boolean sHideFp = true;
        static float glyphUnit(RectF ignored) { return 300f; }
        static View[] clockRoots() { return null; }
        static View clockTarget(View root) { return root; }
        static boolean clockHeld() { return q.a; }
        static boolean keyguardLocked() { return true; }
        static boolean onKeyguardNow() { return true; }
        static boolean controlCenterShown() { return false; }
        static float cardProgress() { return 1f; }
        static boolean screenOn() { return true; }
        static void applyHideFp() {}
        static boolean fodForKeyguard(Object view) { return true; }
        static Object fodViewOf(Object animation) { return animation; }
        static boolean isFodRing(int resource) { return true; }
    }
    static class ChangedMain { static int glyphUnit(RectF ignored) { return 300; } }
    @Test public void renamedInternalClassesDoNotAffectAdapter() throws Exception {
        HyperMusicCoverAdapter adapter = new HyperMusicCoverAdapter(RetainedMain.class);
        assertTrue(adapter.bool(adapter.clockHeld));
        q.a = false;
        assertFalse(adapter.bool(adapter.clockHeld));
        q.a = true;
        assertEquals(2, adapter.fpAvoid.getInt());
        assertTrue(adapter.hideFp.getBoolean());
        assertEquals(Float.valueOf(700f), adapter.holdY.get(null));
    }
    @Test public void missingOrChangedRetainedInterfaceIsRejected() {
        assertThrows(NoSuchMethodException.class, () -> new HyperMusicCoverAdapter(String.class));
        assertThrows(NoSuchMethodException.class, () -> new HyperMusicCoverAdapter(ChangedMain.class));
    }
    static class RenamedHelper {
        static float p = .75f;
        static int a = 2;
        static boolean b = true;
        static float z(RectF box) { return 123f; }
        static void r() { }
        static boolean q(Object view) { return true; }
    }
    private static String descriptor(Class<?> owner) { return "L" + owner.getName().replace('.', '/') + ";"; }
    static HyperMusicCoverDexSymbols splitSymbols() {
        HyperMusicCoverDexSymbols s = new HyperMusicCoverDexSymbols();
        String owner = descriptor(RenamedHelper.class);
        s.members.put("glyphUnit", new HyperMusicCoverDexSymbols.Member(owner, "z", "(Landroid/graphics/RectF;)F"));
        s.members.put("progress", new HyperMusicCoverDexSymbols.Member(owner, "p", "F"));
        s.members.put("fpAvoid", new HyperMusicCoverDexSymbols.Member(owner, "a", "I"));
        s.members.put("hideFp", new HyperMusicCoverDexSymbols.Member(owner, "b", "Z"));
        s.members.put("applyHide", new HyperMusicCoverDexSymbols.Member(owner, "r", "()V"));
        s.members.put("fodForKeyguard", new HyperMusicCoverDexSymbols.Member(owner, "q", "(Ljava/lang/Object;)Z"));
        return s;
    }
    @Test public void splitAndRenamedHelpersUseResolvedMembersIncludingInlinedProgress() throws Exception {
        HyperMusicCoverAdapter adapter = new HyperMusicCoverAdapter(RetainedMain.class, splitSymbols());
        assertEquals(RenamedHelper.class, adapter.glyphUnit.getDeclaringClass());
        assertEquals(.75f, adapter.progress(), 0f);
        RenamedHelper.p = .25f;
        assertEquals(.25f, adapter.progress(), 0f);
        RenamedHelper.p = .75f;
        assertEquals(2, adapter.fpAvoid.getInt());
        assertTrue(adapter.hideFp.getBoolean());
        assertEquals(RenamedHelper.class, adapter.applyHide.getDeclaringClass());
    }
    @Test public void resolvedFieldWithWrongTypeIsRejected() {
        HyperMusicCoverDexSymbols symbols = splitSymbols();
        symbols.members.put("progress", symbols.members.get("fpAvoid"));
        assertThrows(NoSuchFieldException.class, () -> new HyperMusicCoverAdapter(RetainedMain.class, symbols));
    }
    static class IntSetting { private volatile int v; private int fallback = 99; }
    static class BoolSetting { private volatile boolean v; }
    static class Settings {
        static IntSetting a = new IntSetting();
        static final BoolSetting b = new BoolSetting();
    }
    private static HyperMusicCoverDexSymbols settingSymbols() {
        HyperMusicCoverDexSymbols s = splitSymbols();
        s.members.put("fpAvoidEntry", new HyperMusicCoverDexSymbols.Member(descriptor(Settings.class), "a", descriptor(IntSetting.class)));
        s.members.put("fpAvoid", new HyperMusicCoverDexSymbols.Member(descriptor(IntSetting.class), "v", "I"));
        s.members.put("hideFpEntry", new HyperMusicCoverDexSymbols.Member(descriptor(Settings.class), "b", descriptor(BoolSetting.class)));
        s.members.put("hideFp", new HyperMusicCoverDexSymbols.Member(descriptor(BoolSetting.class), "v", "Z"));
        return s;
    }
    @Test public void settingsObjectsAreReadLiveWithoutChangingUserPreferences() throws Exception {
        Settings.a = new IntSetting();
        Settings.b.v = false;
        HyperMusicCoverAdapter adapter = new HyperMusicCoverAdapter(RetainedMain.class, settingSymbols());
        for (int mode = 0; mode <= 2; mode++) {
            Settings.a.v = mode;
            Settings.b.v = mode != 0;
            assertEquals(mode, adapter.fpAvoid.getInt());
            assertEquals(mode != 0, adapter.hideFp.getBoolean());
            assertEquals(mode, Settings.a.v);
            assertEquals(99, Settings.a.fallback);
        }
        // Cache reflection metadata, not a setting's current value or receiver.
        Settings.a = new IntSetting();
        Settings.b.v = false;
        assertEquals(0, adapter.fpAvoid.getInt());
        assertFalse(adapter.hideFp.getBoolean());
    }
    @Test public void settingDefaultCannotBeMistakenForLiveValue() {
        HyperMusicCoverDexSymbols s = settingSymbols();
        s.members.put("fpAvoid", new HyperMusicCoverDexSymbols.Member(descriptor(IntSetting.class), "fallback", "I"));
        assertThrows(NoSuchFieldException.class, () -> new HyperMusicCoverAdapter(RetainedMain.class, s));
    }
    @Test public void mismatchedSettingObjectIsRejected() {
        HyperMusicCoverDexSymbols s = settingSymbols();
        s.members.put("fpAvoidEntry", s.members.get("hideFpEntry"));
        assertThrows(NoSuchFieldException.class, () -> new HyperMusicCoverAdapter(RetainedMain.class, s));
    }
    @Test public void nullSettingObjectFailsWithoutGuessingAUserPreference() throws Exception {
        HyperMusicCoverAdapter adapter = new HyperMusicCoverAdapter(RetainedMain.class, settingSymbols());
        Settings.a = null;
        try { assertThrows(IllegalAccessException.class, () -> adapter.fpAvoid.getInt()); }
        finally { Settings.a = new IntSetting(); }
    }
    static class FodParent { private final Object mMiuiGxzwAnimationView = "native-view"; }
    static class FodChild extends FodParent { }
    @Test public void inlinedFodViewHelperUsesCachedNativeFieldOnParent() throws Exception {
        HyperMusicCoverAdapter adapter = new HyperMusicCoverAdapter(RetainedMain.class);
        FodChild animation = new FodChild();
        assertEquals("native-view", adapter.fodView(animation));
        assertEquals("native-view", adapter.fodView(animation));
        assertNull(adapter.fodView(new Object()));
        assertNull(adapter.fodView(new Object()));
    }
}

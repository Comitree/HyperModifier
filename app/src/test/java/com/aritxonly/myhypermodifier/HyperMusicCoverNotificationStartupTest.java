package com.aritxonly.myhypermodifier;

import com.android.keyguard.panel.KeyguardPanelViewController$fixture$nsslLockYPosition$inlined$combine$1;
import io.github.libxposed.api.XposedInterface;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlinx.coroutines.flow.Flow;
import kotlinx.coroutines.flow.StateFlow;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

/** Exercises the real startup entry and original notification hooks with a recorded Xposed API. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE, application = android.app.Application.class)
public class HyperMusicCoverNotificationStartupTest {
    private final Map<Method, XposedInterface.Hooker> hooks = new HashMap<>();
    private final MyHyperModifier module = new MyHyperModifier();
    private final HostLoader loader = new HostLoader();

    public static class Dex {
        public Enumeration<String> entries() {
            return Collections.enumeration(Collections.singleton(
                    KeyguardPanelViewController$fixture$nsslLockYPosition$inlined$combine$1.class.getName() + "$3"));
        }
    }
    public static class Element { public final Dex dexFile = new Dex(); }
    public static class Paths { public final Object[] dexElements = {new Element()}; }
    public static class HostLoader extends ClassLoader {
        public final Paths pathList = new Paths();
        HostLoader() { super(HyperMusicCoverNotificationStartupTest.class.getClassLoader()); }
    }

    @Before public void prepare() throws Exception {
        ModuleSettings.moduleHooksEnabled = true;
        ModuleSettings.systemUiCompatibilityMode = false;
        ModuleSettings.hmcExpandedFingerprint = true;
        ModuleSettings.sinkLockscreenNotificationsForFingerprint = false;
        flag(ModuleSettings.class, "SETTINGS_LOADED").set(true);
        flag(MyHyperModifier.class, "EARLY_SYSTEM_UI_HOOKS_INSTALLED").set(false);
        // Keep the unrelated delayed worker out of this startup-boundary test.
        flag(MyHyperModifier.class, "DEFERRED_SYSTEM_UI_HOOKS_SCHEDULED").set(true);
        flag(HyperMusicCoverExtensions.class, "NOTIFICATIONS_INSTALLED").set(false);
        set("notifications", null);
        set("adapter", null);
        set("expanded", false);
        module.attachFramework((XposedInterface) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{XposedInterface.class}, (proxy, method, args) -> {
                    if (method.getName().equals("deoptimize")) return true;
                    if (!method.getName().equals("hook")) return null;
                    Method target = (Method) args[0];
                    return Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[]{XposedInterface.HookBuilder.class}, (builder, operation, values) -> {
                                if (operation.getName().equals("intercept")) {
                                    assertNull("duplicate hook: " + target, hooks.put(target, (XposedInterface.Hooker) values[0]));
                                    return null;
                                }
                                return builder;
                            });
                }), () -> {});
    }

    @Test public void firstCollectionIncludesPolicyBeforeDeferredInstallAndRestoresMode() throws Throwable {
        startEarly();
        KeyguardPanelViewController$fixture$nsslLockYPosition$inlined$combine$1 source = source(7);
        Flow<?>[] collecting = collect(source);
        assertEquals(8, collecting.length);
        StateFlow<?> policy = (StateFlow<?>) collecting[7];
        assertEquals(0, policy.getValue()); // HMC not bound yet.
        HyperMusicCoverAdapterTest.RetainedMain.sFpAvoid = 2;
        set("adapter", new HyperMusicCoverAdapter(HyperMusicCoverAdapterTest.RetainedMain.class));
        HyperMusicCoverNotificationFlow notifications = (HyperMusicCoverNotificationFlow) field("notifications").get(null);
        set("expanded", true);
        notifications.refresh();
        assertEquals(1, policy.getValue());
        set("expanded", false);
        notifications.refresh();
        assertEquals(2, policy.getValue()); // Original HMC avoidance is restored.
        assertSame(collecting, collect(source)); // No second policy on recollection.
        int hookCount = hooks.size();
        HyperMusicCoverExtensions.installNotificationFlow(module, loader);
        assertEquals(hookCount, hooks.size()); // Delayed phase cannot install duplicates.
        assertSame(notifications, field("notifications").get(null));
    }

    @Test public void existingHmcPolicyIsPreserved() throws Throwable {
        startEarly();
        KeyguardPanelViewController$fixture$nsslLockYPosition$inlined$combine$1 source = source(8);
        Flow<?>[] originals = source.sources;
        Flow<?>[] collecting = collect(source);
        assertEquals(9, collecting.length);
        for (int i = 0; i < originals.length; i++) assertSame(originals[i], collecting[i]);
    }

    @Test public void disabledFeatureLeavesSourcesUntouched() throws Throwable {
        ModuleSettings.hmcExpandedFingerprint = false;
        startEarly();
        KeyguardPanelViewController$fixture$nsslLockYPosition$inlined$combine$1 source = source(7);
        Flow<?>[] originals = source.sources;
        assertSame(originals, collect(source));
        assertEquals(7, originals.length);
    }

    @Test public void compatibilityModeSkipsEarlyNotificationHooks() throws Exception {
        ModuleSettings.systemUiCompatibilityMode = true;
        startEarly();
        assertTrue(hooks.isEmpty());
        assertNull(field("notifications").get(null));
    }

    private void startEarly() throws Exception {
        Method start = MyHyperModifier.class.getDeclaredMethod("startEarlySystemUiHooks", ClassLoader.class);
        start.setAccessible(true);
        start.invoke(module, loader);
    }
    private KeyguardPanelViewController$fixture$nsslLockYPosition$inlined$combine$1 source(int count) {
        KeyguardPanelViewController$fixture$nsslLockYPosition$inlined$combine$1 source =
                new KeyguardPanelViewController$fixture$nsslLockYPosition$inlined$combine$1(count);
        for (int i = 0; i < count; i++) source.sources[i] = kotlinx.coroutines.flow.StateFlowKt.MutableStateFlow(i);
        return source;
    }
    private Flow<?>[] collect(KeyguardPanelViewController$fixture$nsslLockYPosition$inlined$combine$1 source) throws Throwable {
        Method collect = source.getClass().getDeclaredMethod("collect");
        XposedInterface.Hooker hook = hooks.get(collect);
        assertNotNull("notification collect hook must exist before first collection", hook);
        XposedInterface.Chain chain = (XposedInterface.Chain) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{XposedInterface.Chain.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getThisObject")) return source;
                    if (method.getName().equals("proceed")) return source.collect();
                    throw new AssertionError(method);
                });
        return (Flow<?>[]) hook.intercept(chain);
    }
    private static AtomicBoolean flag(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name); field.setAccessible(true);
        return (AtomicBoolean) field.get(null);
    }
    private static Field field(String name) throws Exception {
        Field field = HyperMusicCoverExtensions.class.getDeclaredField(name); field.setAccessible(true);
        return field;
    }
    private static void set(String name, Object value) throws Exception { field(name).set(null, value); }
}

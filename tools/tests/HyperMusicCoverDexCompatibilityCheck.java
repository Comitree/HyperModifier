package com.aritxonly.myhypermodifier;

/** Runs the production resolver against an official APK without Android or apktool. */
public final class HyperMusicCoverDexCompatibilityCheck {
    public static void main(String[] args) throws Exception {
        for (String apk : args) {
            long start = System.nanoTime();
            try (java.net.URLClassLoader loader = new java.net.URLClassLoader(
                    new java.net.URL[]{new java.io.File(apk).toURI().toURL()}, null)) {
                String source = HyperMusicCoverApkSource.loadedApk(loader, "com.os4.musiccover.Main");
                if (source == null || !new java.io.File(source).getCanonicalFile().equals(new java.io.File(apk).getCanonicalFile()))
                    throw new IllegalStateException("HMC APK resource could not be located without a View: " + apk);
                System.out.println("  loader-owned APK source PASS (no clock View/Context)");
            }
            HyperMusicCoverDexSymbols symbols = HyperMusicCoverDexSymbols.read(apk);
            String expectedFile = System.getProperty("hmc.expectedSymbols");
            if (expectedFile != null) {
                if (args.length != 1) throw new IllegalArgumentException("Expectations require exactly one APK");
                java.util.Properties expected = new java.util.Properties();
                try (java.io.InputStream in = new java.io.FileInputStream(expectedFile)) { expected.load(in); }
                byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(apk)));
                String hash = java.util.HexFormat.of().formatHex(digest);
                if (!hash.equals(expected.remove("sha256"))) throw new AssertionError("Wrong APK for fixture");
                if (symbols.members.size() != expected.size()) throw new AssertionError("HMC role count changed");
                for (String role : expected.stringPropertyNames()) {
                    String actual = symbols.get(role).toString();
                    if (!expected.getProperty(role).equals(actual))
                        throw new AssertionError(role + ": expected " + expected.getProperty(role) + ", got " + actual);
                }
                System.out.println("  SHA-256 and all expected semantic roles PASS");
            }
            System.out.println(apk + " PASS " + symbols.members.size() + " roles " + (System.nanoTime() - start) / 1000000 + "ms");
            symbols.members.forEach((role, member) -> System.out.println("  " + role + "=" + member));
        }
    }
}

# TuffClient (Fabric, Minecraft 26.2)
Requires JDK 25 and Gradle 9.x. Drop src/ + gradle files into the official Fabric example mod
(26.2 branch) so you get the Gradle wrapper, then `./gradlew build`.

Controls: Right Shift = menu, F6 = freecam (rebind in Controls). Gear button next to Storage ESP = colors.
Config: .minecraft/config/tuffclient.json

If something fails to compile, extract the deobfuscated client jar from
~/.gradle/caches/fabric-loom/ and run `javap -p <class>` against it (see TROUBLESHOOTING in chat).

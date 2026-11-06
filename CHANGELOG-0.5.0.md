# 0.5.0 - smooth movement, raised aircraft camera, no random events

- Add Minecraft 1.21.11 InterpolationHandler to the machine entity and tick it on the client. Keep server authority and collision/explosion checks.
- Ease velocity changes and hull yaw. Release keys to brake smoothly; timeout/dismount/disconnection still stops immediately. Targeting uses fresh aim angles instead of lagging behind the hull.
- Sample camera position and mouse angles on every rendered frame through a client-only Camera.setup mixin. This removes the additional tick-delayed FPV proxy view.
- Aircraft camera automatically moves 2.6 blocks above and 3.2 blocks behind the aircraft. Clip camera against terrain and restore the previous camera type after leaving.
- Aim both barrels at the crosshair target from the same camera geometry used by the client. Check camera sightline, barrel-to-muzzle and muzzle-to-target paths against blocks. Range remains 36 blocks.
- Remove random messages, animal sounds, musical particles and the B key binding. Keep the drone viewfinder and optional subtle CRT noise on V.
- Preserve 0.4.0 models, textures, recipes, remote-control drone, G detonation and descent bindings.

Verification: Gradle build, existing tests, pure smoothing/camera math tests and source/resource wiring regression checks. Manual in-world camera, collision and aiming checks are still required; automated tests do not prove visual smoothness on every client or network connection.

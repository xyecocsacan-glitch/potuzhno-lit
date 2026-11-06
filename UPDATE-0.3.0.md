# Potuzhno Lit 0.3.0 (Minecraft Java 1.21.11 / Fabric)

## Install
Close the game, remove the previous potuzhno-lit JAR from the active instance's mods directory and put potuzhno-lit-1.21.11-0.3.0.jar there instead. Keep the matching Fabric API. Both client and server must use 0.3.0 because the control channel was versioned.

## Controls (default physical keys)
- E near a drone: remote console. The player stays at the starting position; only the local camera follows the drone. Right-click also works.
- E again or opening a menu: leave the drone camera. Destroying the drone, hurting its operator, disconnecting or losing the control heartbeat closes the console.
- WASD: movement; mouse: aim; Space or Up Arrow: ascend.
- X (the Russian keyboard key Ч) or Down Arrow: descend. Shift is an extra descent key for the remote drone only; in the plane it remains the normal dismount key.
- G: detonate only the currently controlled drone. The drone also detonates on collision after its short arming delay.
- Plane: E / right-click to board, left mouse or R to fire the alternating guns. Shots have muzzle flashes, tracers and server-side hits; no ammunition is required in this prototype.
- V toggles subtle CRT scanlines/grain. The camera brackets and aiming reticle remain visible.
- B toggles harmless local funny events: messages, quiet animal sounds and musical-note particles. The first is after about 15 seconds of flight, then at randomized 30-60 second intervals. No random damage or lost controls.

Remote range is limited to 64 blocks and already loaded chunks. The operator is not invulnerable. The server never switches the player's spectator camera or mounts them onto the drone. A client-only camera proxy is used. The original recipes and creative-tab items are unchanged.

## Checks
CI compiles both the server/common and client source sets and runs nine control-math tests, including 8192 input/heading/vehicle combinations. These tests cover descent, simultaneous up/down input, movement bounds and the detonation authorization predicate; they are not an in-game FPV/rendering/multiplayer test.

## Manual regression checklist
Use a disposable creative world first. Check E without inventory opening; keep a second player watching the operator's stationary body; fly up, down with X/Down Arrow, turn the camera, hit G and confirm camera restoration. Test ordinary collision detonation, the 64-block boundary, damage to the operator, disconnect/rejoin, menu exit, changed key bindings and both plane guns. Verify V and B toggles and that ordinary movement/attacks resume after exit. Back up existing worlds before updating.

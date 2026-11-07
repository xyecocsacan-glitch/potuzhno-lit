# 0.6.0

- Hide the first-person hand and held item only during remote drone camera sessions.
- Read sensitivity-adjusted mouse movement into independent aim angles. Use those same angles in the camera, control packets and aircraft targeting, independently of passenger/hull interpolation.
- Correct the gun ray endpoint regression: cross the selected surface before testing the muzzle ray. Resolve real mob bounding boxes, preserve wall obstruction, credit the pilot and show impact feedback only for accepted damage.
- Increase maximum remote range from 64 to 128 blocks. Existing loaded-chunk and session safety limits still apply; this update does not force-load the entire world or override a server's view distance.
- Keep the 0.5.0 raised aircraft view, smoothing, models, recipes, G detonation and removal of random events.

Validation: compilation, independent aim tests, actual Minecraft AABB intersection regression tests and resource/wiring checks. A successful build is not a claim of manual in-game testing.

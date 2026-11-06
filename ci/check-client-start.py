from pathlib import Path
import re

root = Path('build/client-smoke')
log = '\n'.join(p.read_text(errors='replace') for p in root.glob('*.log'))
required = ['FlightCameraMixin', 'FlightMouseMixin', 'FlightHandMixin']
errors = []
for name in required:
    if not re.search(r'Mixing\s+' + name + r'\b', log):
        errors.append('No runtime application marker for ' + name)
if not re.search(r'Created:.*(?:textures/atlas/blocks|blocks\.png)', log):
    errors.append('Minecraft did not finish creating the block texture atlas')
if re.search(r'Mixin apply.*failed|Mixin transformation.*failed|InjectionError|InvalidMixinException|Crash report saved|ReportedException', log, re.I):
    errors.append('A mixin or client startup crash was reported')
summary = 'CLIENT_STARTUP_OK\n' if not errors else '\n'.join(errors) + '\n'
(root / 'result.txt').write_text(summary)
print(summary)
if errors:
    print(log[-10000:])
    raise SystemExit(1)
print('Validated client startup and mixin application only; no manual gameplay claims.')

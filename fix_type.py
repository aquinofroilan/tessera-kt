import re

file_path = 'src/test/kotlin/com/aquinofroilan/tessera/service/WarehouseServiceTest.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    # If the line has .isEqualTo("...") or .isEqualTo(orgId) and needs a UUID
    if '.isEqualTo(' in line and 'java.util.UUID' not in line:
        lines[i] = re.sub(r'\.isEqualTo\("([^"]+)"\)', r'.isEqualTo(java.util.UUID.fromString("\1"))', line)

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(lines)

import re

path = r"d:\Project\backend\tessera-kt\src\test\kotlin\com\aquinofroilan\tessera\service\ProjectTaskServiceTest.kt"
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. First, make sure orgId, projectId are UUIDs
content = re.sub(r'val orgId = "([^"]+)"', r'val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")', content)
content = re.sub(r'val projectId = "([^"]+)"', r'val projectId = java.util.UUID.fromString("297fd989-49e7-378b-9562-907dbf28876d")', content)

# 2. Fix the `task` helper function signature
content = content.replace("id: String,", "id: java.util.UUID,")
content = content.replace("parent: String? = null", "parent: java.util.UUID? = null")
content = content.replace("org: String = orgId", "org: java.util.UUID = orgId")
content = content.replace("project: String = projectId", "project: java.util.UUID = projectId")
content = content.replace("name = id,", "name = id.toString(),")

# 3. Define standard UUID strings for t1, t2, t3, missing
uuids = {
    "t1": 'java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")',
    "t2": 'java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")',
    "t3": 'java.util.UUID.fromString("00000000-0000-0000-0000-000000000003")',
    "missing": 'java.util.UUID.fromString("00000000-0000-0000-0000-000000000999")'
}

for short, uuid_str in uuids.items():
    content = content.replace(f'"{short}"', uuid_str)
    
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

import re

def fix():
    path = r"d:\Project\backend\tessera-kt\src\test\kotlin\com\aquinofroilan\tessera\service\ProjectTaskServiceTest.kt"
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    content = content.replace("parent: String? = null", "parent: UUID? = null")
    content = content.replace("org: String = orgId", "org: UUID = orgId")
    content = content.replace("project: String = projectId", "project: UUID = projectId")
    
    # name = id won't compile if id is UUID and name is String
    content = content.replace("name = id", 'name = id.toString()')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

fix()

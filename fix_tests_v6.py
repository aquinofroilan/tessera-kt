import re
import os
import uuid

def string_to_uuid_str(s):
    if re.match(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$', s):
        return s
    return str(uuid.uuid3(uuid.NAMESPACE_DNS, s))

def fix_all():
    test_dir = r"d:\Project\backend\tessera-kt\src\test\kotlin\com\aquinofroilan\tessera"
    for root, _, files in os.walk(test_dir):
        for file in files:
            if not file.endswith('.kt'):
                continue
            
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
                
            orig = content
            
            # 1. Type annotations: customerId: String -> customerId: UUID
            content = re.sub(r'([a-zA-Z0-9_]+[iI]d\w*)\s*:\s*String(\?)?', r'\1: UUID\2', content)
            content = re.sub(r'(createdBy|updatedBy)\s*:\s*String(\?)?', r'\1: UUID\2', content)
            
            # 2. variable assignments: val createdBy = "..." -> val createdBy = UUID...
            def repl_by(m):
                kw = m.group(1)
                name = m.group(2)
                type_decl = m.group(3) or ""
                s = m.group(4)
                if len(s) == 36 and s.count('-') == 4:
                    return m.group(0)
                return f'{kw} {name}{type_decl} = java.util.UUID.fromString("{string_to_uuid_str(s)}")'
            
            content = re.sub(r'(val|var)\s+(createdBy|updatedBy)\s*(:\s*String\??)?\s*=\s*"([^"]+)"', repl_by, content)
            
            # 3. Add UUID import if we added UUID type
            if "UUID" in content and "java.util.UUID" not in content and "import java.util.UUID" not in content:
                # Let's just always use java.util.UUID for the type if it was missing?
                # Actually, replacing `UUID` with `java.util.UUID` in type definitions is safer.
                pass
                
            if content != orig:
                if "UUID" in content and "import java.util.UUID" not in content:
                    content = re.sub(r'^package\s+.*$', lambda m: m.group(0) + '\n\nimport java.util.UUID', content, flags=re.MULTILINE)
                    
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)

if __name__ == "__main__":
    fix_all()

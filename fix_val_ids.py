import re
import os
import sys
import uuid

def string_to_uuid_str(s):
    if re.match(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$', s):
        return s
    return str(uuid.uuid3(uuid.NAMESPACE_DNS, s))

def fix_all_files():
    test_dir = r"d:\Project\backend\tessera-kt\src\test\kotlin\com\aquinofroilan\tessera"
    
    files_fixed = 0
    for root, _, files in os.walk(test_dir):
        for file in files:
            if not file.endswith('.kt'):
                continue
            
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
                
            orig_content = content
            
            # 1. Fix `val somethingId = "..."`
            def repl_val_id(m):
                kw = m.group(1) # val or var
                name = m.group(2)
                type_decl = m.group(3) or ""
                s = m.group(4)
                
                # replace type_decl if it's String
                if "String" in type_decl:
                    type_decl = type_decl.replace("String", "UUID")
                    
                if len(s) == 36 and s.count('-') == 4:
                    return m.group(0)
                    
                u = string_to_uuid_str(s)
                return f'{kw} {name}{type_decl} = java.util.UUID.fromString("{u}")'
                
            content = re.sub(r'(val|var)\s+(\w+Id\w*)\s*(:\s*String\??)?\s*=\s*"([^"]+)"', repl_val_id, content)
            
            # 2. Fix `id = "..."` (default parameters or named arguments)
            def repl_arg_id(m):
                name = m.group(1)
                s = m.group(2)
                if len(s) == 36 and s.count('-') == 4:
                    return m.group(0)
                u = string_to_uuid_str(s)
                return f'{name} = java.util.UUID.fromString("{u}")'
                
            content = re.sub(r'(\b\w*id\w*)\s*=\s*"([^"]+)"', repl_arg_id, content, flags=re.IGNORECASE)
            
            if content != orig_content:
                files_fixed += 1
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.write(content)
                    
    print(f"Fixed {files_fixed} files.")

if __name__ == "__main__":
    fix_all_files()

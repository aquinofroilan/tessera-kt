import re
import os
import sys
import uuid

def string_to_uuid_str(s):
    # Generates a consistent UUID string for a given short string
    # If the string is already a UUID, return it
    if re.match(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$', s):
        return s
    return str(uuid.uuid3(uuid.NAMESPACE_DNS, s))

def fix_errors(log_path):
    with open(log_path, 'r', encoding='utf-8') as f:
        log_content = f.read()

    error_pattern = re.compile(r'e: file:///(.+?):(\d+):(\d+) (.*)')
    
    files_to_fix = {}
    
    for match in error_pattern.finditer(log_content):
        file_path = match.group(1).replace('/', '\\')
        line_num = int(match.group(2))
        msg = match.group(4)
        
        if file_path not in files_to_fix:
            files_to_fix[file_path] = []
        files_to_fix[file_path].append((line_num, msg))

    for file_path, errors in files_to_fix.items():
        if not os.path.exists(file_path):
            continue
            
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
        needs_uuid_import = False
        
        for line_num, msg in errors:
            idx = line_num - 1
            if idx >= len(lines):
                continue
                
            line = lines[idx]
            
            if "Unresolved reference 'UUID'" in msg or "Unresolved reference: UUID" in msg:
                needs_uuid_import = True
                # Also we can fix the exact UUID usages if they are wrong, but unresolved reference just needs import.
                
            elif "String" in msg and "UUID" in msg:
                # Need to replace String with UUID
                
                # 1. Fix listOf("a", "b") -> listOf(java.util.UUID.fromString(...), ...)
                if "List" in msg or "Iterable" in msg:
                    def replace_list_args(m):
                        args = m.group(1)
                        # Replace all "string" inside args
                        def replace_str(sm):
                            s = sm.group(1)
                            u = string_to_uuid_str(s)
                            return f'java.util.UUID.fromString("{u}")'
                        new_args = re.sub(r'"([^"]+)"', replace_str, args)
                        return f'listOf({new_args})'
                    line = re.sub(r'listOf\((.*?)\)', replace_list_args, line)
                
                # 2. Fix == "..."
                if "==" in msg:
                    def replace_eq_str(m):
                        s = m.group(1)
                        u = string_to_uuid_str(s)
                        return f'== java.util.UUID.fromString("{u}")'
                    line = re.sub(r'==\s*"([^"]+)"', replace_eq_str, line)
                    
                # 3. Fix mapOf("id" to "p1")
                if "mapOf" in line:
                    def replace_map_str(m):
                        s = m.group(1)
                        u = string_to_uuid_str(s)
                        return f'java.util.UUID.fromString("{u}")'
                    # we only replace strings that are short and have no spaces, likely IDs
                    # actually, let's just use regex for string literals that are values in the map and short
                    line = re.sub(r'(?:to\s*)"([a-zA-Z0-9_-]{1,20})"', lambda m: f'to java.util.UUID.fromString("{string_to_uuid_str(m.group(1))}")', line)
                
                # 4. Fix val id = "..." or id = "..." or somethingId = "..."
                def replace_id_assign(m):
                    prefix = m.group(1)
                    s = m.group(2)
                    u = string_to_uuid_str(s)
                    return f'{prefix}java.util.UUID.fromString("{u}")'
                line = re.sub(r'(\b\w*id\w*\s*=\s*)"([^"]+)"', replace_id_assign, line, flags=re.IGNORECASE)
                
                # 5. Fix function parameters. e.g. createTaxRate("tr-a", ...)
                # If there's a short string literal with no spaces, replace it.
                # Only if the string is < 20 chars and no spaces.
                def replace_short_str(m):
                    s = m.group(1)
                    if " " not in s and len(s) < 20 and not s.isupper() and s != "":
                        # likely an ID
                        u = string_to_uuid_str(s)
                        return f'java.util.UUID.fromString("{u}")'
                    return f'"{s}"'
                # apply to strings not preceded by fromString(
                line = re.sub(r'(?<!fromString\()"([^"]+)"', replace_short_str, line)
                
                # 6. Fix `String` types in variable declarations
                line = re.sub(r':\s*String(\?)?', lambda m: f': UUID{m.group(1) or ""}', line)
                line = re.sub(r'List<String>', r'List<UUID>', line)
                line = re.sub(r'Map<String,', r'Map<UUID,', line)
                
            lines[idx] = line
            
        if needs_uuid_import:
            # Add import after package
            for i, line in enumerate(lines):
                if line.startswith('package '):
                    lines.insert(i + 1, '\nimport java.util.UUID\n')
                    break
                    
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(lines)
            
    print(f"Applied semantic fixes to {len(files_to_fix)} files.")

if __name__ == "__main__":
    fix_errors(sys.argv[1])

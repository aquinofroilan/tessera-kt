import re
import os
import sys
import uuid

def string_to_uuid_str(s):
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
                
            elif "String" in msg and "UUID" in msg:
                # 1. Replace string literals
                def repl(match):
                    s = match.group(1)
                    if len(s) == 36 and s.count('-') == 4:
                        return match.group(0)
                    if " " in s or "." in s or s.isdigit():
                        return match.group(0)
                    start = match.start()
                    prefix = line[max(0, start-20):start]
                    if "fromString(" in prefix or "BigDecimal(" in prefix:
                        return match.group(0)
                    u = string_to_uuid_str(s)
                    return f'java.util.UUID.fromString("{u}")'
                
                line = re.sub(r'"([^"]+)"', repl, line)
                
                # 2. Fix type declarations, e.g. `id: String` -> `id: UUID`
                line = re.sub(r'([iI]d)\s*:\s*String(\?)?', r'\1: UUID\2', line)
                
                # 3. Fix collection types
                line = re.sub(r'List<String>', r'List<UUID>', line)
                line = re.sub(r'Map<String,', r'Map<UUID,', line)
                line = re.sub(r'Iterable<String>', r'Iterable<UUID>', line)
                line = re.sub(r'Set<String>', r'Set<UUID>', line)
                
            lines[idx] = line
            
        if needs_uuid_import:
            for i, line in enumerate(lines):
                if line.startswith('package '):
                    lines.insert(i + 1, '\nimport java.util.UUID\n')
                    break
                    
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(lines)
            
    print(f"Applied fixes to {len(files_to_fix)} files.")

if __name__ == "__main__":
    fix_errors(sys.argv[1])

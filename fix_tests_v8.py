import re
import os
import sys
import uuid

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
            
        for line_num, msg in errors:
            idx = line_num - 1
            if idx >= len(lines):
                continue
                
            line = lines[idx]
            
            if "actual type is 'String', but 'UUID" in msg or "expected 'UUID', actual 'String'" in msg or "actual type is 'String', but 'UUID?' was expected" in msg or "expected 'UUID?', actual 'String'" in msg:
                # Need to wrap a string with UUID
                def repl_str(m):
                    s = m.group(1)
                    if len(s) == 36 and s.count('-') == 4:
                        return f'java.util.UUID.fromString("{s}")'
                    u = str(uuid.uuid3(uuid.NAMESPACE_DNS, s))
                    return f'java.util.UUID.fromString("{u}")'
                
                # if there is a variable passing like `orgId` we can't fix it this way,
                # but if there's a string literal like "foo", this will fix it.
                line = re.sub(r'(?<!fromString\()"([^"]+)"', repl_str, line)
                
            lines[idx] = line
            
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(lines)
            
    print(f"Applied fixes to {len(files_to_fix)} files.")

if __name__ == "__main__":
    fix_errors(sys.argv[1])

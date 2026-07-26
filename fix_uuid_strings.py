import re
import os
import sys

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
            
            if "String" in msg and "UUID" in msg:
                # Wrap any UUID-formatted string with java.util.UUID.fromString
                def repl_uuid_str(match):
                    s = match.group(1)
                    start = match.start()
                    prefix = line[max(0, start-15):start]
                    if "fromString(" in prefix:
                        return match.group(0)
                    return f'java.util.UUID.fromString("{s}")'
                    
                line = re.sub(r'"([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})"', repl_uuid_str, line)
                
            lines[idx] = line
            
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(lines)
            
    print(f"Applied fixes to {len(files_to_fix)} files.")

if __name__ == "__main__":
    fix_errors(sys.argv[1])

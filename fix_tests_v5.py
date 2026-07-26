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
            
        needs_uuid_import = False
        
        for line_num, msg in errors:
            idx = line_num - 1
            if idx >= len(lines):
                continue
                
            line = lines[idx]
            
            if "Unresolved reference 'UUID'" in msg or "Unresolved reference: UUID" in msg:
                line = re.sub(r'(?<!java\.util\.)UUID', 'java.util.UUID', line)
                
            elif "actual type is 'UUID!', but 'String" in msg or "expected 'String', actual 'UUID!'" in msg or "actual type is 'UUID!', but 'String?' was expected." in msg:
                # We placed a UUID where a String was expected. Revert it to a string.
                line = re.sub(r'java\.util\.UUID\.fromString\("([^"]+)"\)', r'"\1"', line)
                
            elif "String" in msg and "UUID" in msg:
                if "Map<String" in msg:
                    line = re.sub(r'Map<String,', r'Map<java.util.UUID,', line)
                    
                else:
                    # String where UUID was expected
                    # Replace short string literals
                    def repl_str(match):
                        s = match.group(1)
                        if len(s) == 36 and s.count('-') == 4:
                            return match.group(0)
                        if " " in s or "." in s:
                            return match.group(0)
                        import uuid
                        u = str(uuid.uuid3(uuid.NAMESPACE_DNS, s))
                        return f'java.util.UUID.fromString("{u}")'
                        
                    line = re.sub(r'(?<!fromString\()"([^"]+)"', repl_str, line)

            lines[idx] = line
            
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(lines)
            
    print(f"Applied fixes to {len(files_to_fix)} files.")

if __name__ == "__main__":
    fix_errors(sys.argv[1])

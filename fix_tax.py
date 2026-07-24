import re

def fix_tax(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Parameter Types
    content = content.replace("id: String", "id: java.util.UUID")
    content = content.replace("orgId: String", "orgId: java.util.UUID")
    content = content.replace("taxRateIds: List<String>", "taxRateIds: List<java.util.UUID>")
    content = content.replace("List<String>", "List<java.util.UUID>")
    content = content.replace("Map<String,", "Map<java.util.UUID,")
    
    # 2. String literal assignments in default arguments
    # id: String = "tr-1" is now id: UUID = ...
    # We will just replace specific short strings with UUIDs globally
    
    def uuid_for(s):
        if s == "tr-1": return "00000000-0000-0000-0000-000000000001"
        if s == "tr-2": return "00000000-0000-0000-0000-000000000002"
        if s == "tg-1": return "00000000-0000-0000-0000-000000000011"
        if s == "tg-2": return "00000000-0000-0000-0000-000000000012"
        if s == "acc-2300": return "00000000-0000-0000-0000-000000000021"
        if s == "acc-2310": return "00000000-0000-0000-0000-000000000022"
        if s == "other-org": return "00000000-0000-0000-0000-000000000099"
        return None
        
    def repl(m):
        u = uuid_for(m.group(1))
        if u: return f'java.util.UUID.fromString("{u}")'
        return m.group(0)

    # 3. Replace all "tr-1" etc strings
    content = re.sub(r'"((?:tr|tg|acc|other-org)[^"]*)"', repl, content)
    
    # 4. Handle "Tax Rate $id" and "TR-$id" since id is now UUID
    content = content.replace('name = "Tax Rate $id"', 'name = "Tax Rate ${id}"')
    content = content.replace('code = "TR-$id"', 'code = "TR-${id}"')
    content = content.replace('name = "Tax Group $id"', 'name = "Tax Group ${id}"')
    content = content.replace('code = "TG-$id"', 'code = "TG-${id}"')
    content = content.replace('name = "Account $code"', 'name = "Account ${code}"')
    
    # 5. orgId = "other-org" needs to be fixed to orgId = java.util... which is done above
    
    # 6. assertThat(exception.message).contains(rate.id) but rate.id is UUID
    content = content.replace('contains(rate.id)', 'contains(rate.id.toString())')
    content = content.replace('contains(rate1.id)', 'contains(rate1.id.toString())')
    
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)

fix_tax(r"d:\Project\backend\tessera-kt\src\test\kotlin\com\aquinofroilan\tessera\service\TaxGroupServiceTest.kt")
fix_tax(r"d:\Project\backend\tessera-kt\src\test\kotlin\com\aquinofroilan\tessera\service\TaxRateServiceTest.kt")


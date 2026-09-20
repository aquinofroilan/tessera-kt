import sys
content = open(sys.argv[1]).read()

content = content.replace("AND quantity >= ?", "AND quantity - held_quantity >= ?")

open(sys.argv[1], 'w').write(content)

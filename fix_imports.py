with open('app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if line.startswith('import androidx.compose.foundation.rememberScrollState') or line.startswith('import androidx.compose.foundation.verticalScroll'):
        continue
    new_lines.append(line)
    if line.startswith('package '):
        new_lines.append('import androidx.compose.foundation.rememberScrollState\n')
        new_lines.append('import androidx.compose.foundation.verticalScroll\n')

with open('app/src/main/java/com/xyecoc/mail/ui/screens/compose/ComposeScreen.kt', 'w') as f:
    f.writelines(new_lines)
EOF
python3 fix_imports.py

const fs = require('fs');

const files = [
    'app/src/main/java/com/example/ui/screens/HomeScreen.kt',
    'app/src/main/java/com/example/ui/screens/ReportScreen.kt',
    'app/src/main/java/com/example/ui/screens/HeatmapScreen.kt',
    'app/src/main/java/com/example/ui/screens/UsageScreen.kt',
    'app/src/main/java/com/example/ui/screens/PreferencesScreen.kt'
];

files.forEach(file => {
    let content = fs.readFileSync(file, 'utf8');

    // 1) Inject appFont if completely missing
    if (!content.includes('val appFont = ')) {
        content = content.replace(
            /(val selectedTheme by viewModel\.selectedTheme\.collectAsState\(\).*?\n)/,
            "$1    val selectedFont by viewModel.selectedFont.collectAsState()\n    val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)\n"
        );
    }
    
    // Preferences screen special handling
    if (file.includes('PreferencesScreen.kt')) {
        content = content.replace(/val selectedFontName by viewModel\.selectedFont\.collectAsState\(\)/g, "val selectedFont by viewModel.selectedFont.collectAsState()");
        content = content.replace(/val fontFamily = ThemeStyles\.getSelectedFontFamily\(selectedFontName\)/g, "val appFont = ThemeStyles.getSelectedFontFamily(selectedFont)");
        content = content.replace(/fontFamily = fontFamily/g, "fontFamily = appFont");
    }

    // Replace FontFamily.Cursive -> appFont globally
    content = content.replace(/fontFamily = FontFamily\.Cursive/g, "fontFamily = appFont");

    let replaced = content.split('Text(');
    for (let i = 1; i < replaced.length; i++) {
        let block = replaced[i];
        let parensCount = 1;
        let j = 0;
        let inString = false;
        let escape = false;

        while(j < block.length && parensCount > 0) {
            let ch = block[j];
            if (inString) {
                if (escape) escape = false;
                else if (ch === '\\') escape = true;
                else if (ch === '"') inString = false;
            } else {
                if (ch === '"') inString = true;
                else if (ch === '(') parensCount++;
                else if (ch === ')') parensCount--;
            }
            j++;
        }
        
        let inner = block.substring(0, j - 1);
        
        if (!inner.includes('fontFamily')) {
            let isPureEmoji = false;
            if (inner.includes('text = habitIcon')) isPureEmoji = true;
            if (inner.includes('text = icon')) isPureEmoji = true;
            if (inner.includes('text = yr.toString() + " 🍕"')) isPureEmoji = false;
            let textMatch = inner.match(/text\s*=\s*"([^"]*)"/);
            if (textMatch) {
                let textVal = textMatch[1].replace(/\\./g, '');
                if (!/[a-zA-Z0-9]/.test(textVal)) {
                     if (textVal.trim() !== "") {
                         isPureEmoji = true;
                     }
                }
            } else {
                let maybeEmojiVar = inner.match(/text\s*=\s*([a-zA-Z0-9_]+)/);
                if (maybeEmojiVar && (maybeEmojiVar[1] === 'habitIcon' || maybeEmojiVar[1] === 'icon')) {
                     isPureEmoji = true;
                }
            }
            
            if (!isPureEmoji) {
                replaced[i] = 'fontFamily = appFont, ' + block;
            }
        }
    }
    content = replaced.join('Text(');

    fs.writeFileSync(file, content);
});
console.log('done');

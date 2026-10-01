const fs = require('fs');
const path = require('path');

const fonts = {
    'jetbrains_mono.ttf': 'https://fonts.gstatic.com/s/jetbrainsmono/v24/tDbY2o-flEEny0FZhsfKu5WU4zr3E_BX0PnT8RD8yKxjPQ.ttf',
    'space_grotesk.ttf': 'https://fonts.gstatic.com/s/spacegrotesk/v22/V8mQoQDjQSkFtoMM3T6r8E7mF71Q-gOoraIAEj7oUUsj.ttf',
    'comfortaa.ttf': 'https://fonts.gstatic.com/s/comfortaa/v47/1Pt_g8LJRfWJmhDAuUsSQamb1W0lwk4S4WjMPrQ.ttf',
    'nunito.ttf': 'https://fonts.gstatic.com/s/nunito/v32/XRXI3I6Li01BKofiOc5wtlZ2di8HDLshRTM.ttf'
};

const dir = path.join(process.cwd(), 'app/src/main/res/font');
if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
}

async function download() {
    for (const [name, url] of Object.entries(fonts)) {
        console.log(`Downloading ${name} from ${url}...`);
        const res = await fetch(url);
        if (!res.ok) throw new Error(`Failed to fetch ${name}: ${res.statusText}`);
        const ab = await res.arrayBuffer();
        fs.writeFileSync(path.join(dir, name), Buffer.from(ab));
        console.log(`Saved ${name}`);
    }
}

download().catch(console.error);

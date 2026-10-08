Add-Type -AssemblyName System.Drawing

$textureDirectory = Join-Path $PSScriptRoot '../common/src/main/resources/assets/rsadvanced/textures/item'
New-Item -ItemType Directory -Force $textureDirectory | Out-Null

# The shared disk silhouette and infinity glyph keep future disk variants visually consistent.
$diskPixels = @(
    '................',
    '..DDDDDDDDDD....',
    '.DEEEEEEEEEED...',
    '.DEMMMMMMMDED...',
    '.DEMMMMMMMDED...',
    '.DEMMMMMMMDED...',
    '.DEMMMMMMMDED...',
    '.DEEEEEEEEEED...',
    '.DEDDDDDDDDED...',
    '.DEDWWDWWDDED...',
    '.DEDWDWDWDDED...',
    '.DEDWWDWWDDED...',
    '.DEDDDDDDDDED...',
    '.DEEEEEEEEEED...',
    '..DDDDDDDDDD....',
    '................'
)

$diskVariants = @(
    @{ Name = 'infinite_item_disk'; Accent = '#8C9298' },
    @{ Name = 'infinite_fluid_disk'; Accent = '#2389E8' }
)

foreach ($diskVariant in $diskVariants) {
    $colors = @{
        D = [System.Drawing.ColorTranslator]::FromHtml('#172331')
        E = [System.Drawing.ColorTranslator]::FromHtml('#BCD0DF')
        M = [System.Drawing.ColorTranslator]::FromHtml($diskVariant.Accent)
        W = [System.Drawing.ColorTranslator]::FromHtml('#63FFD4')
    }
    $bitmap = [System.Drawing.Bitmap]::new(16, 16)
    try {
        for ($pixelY = 0; $pixelY -lt 16; $pixelY++) {
            for ($pixelX = 0; $pixelX -lt 16; $pixelX++) {
                $pixelCode = [string]$diskPixels[$pixelY][$pixelX]
                if ($colors.ContainsKey($pixelCode)) {
                    $bitmap.SetPixel($pixelX, $pixelY, $colors[$pixelCode])
                }
            }
        }
        $texturePath = Join-Path $textureDirectory ($diskVariant.Name + '.png')
        $bitmap.Save($texturePath, [System.Drawing.Imaging.ImageFormat]::Png)
    }
    finally {
        $bitmap.Dispose()
    }
}

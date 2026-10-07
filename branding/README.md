# WWave visual assets

Approved visual direction: interlocked diagonal WW monogram, a smooth lavender → sky blue → mint gradient, light background.

Reference studied before generation: https://ru.pinterest.com/pin/2814818513026732/
Transferred features: thick typographic strokes, deep V-shaped counters, fused organic bridge and flat-cut terminals. The logo depicts WW, not the reference wordmark.

`ww-master.png` is the accepted pastel version on a light background. `ww-transparent.png` removes that background for placement directly in the app header. The header contains a single logo without a second WW text label.

The logo masters were AI-generated and edited with the imagegen tool. Packaging code crops unused transparent canvas and resizes assets without changing their aspect ratio.

Background-removal prompt:

> Edit this approved WW logo solely to remove its off-white square background. Produce the identical pastel WW monogram as a transparent PNG cutout on a fully transparent background. Preserve exactly the existing diagonal interlocked W geometry, stroke widths, sharp V counters, connected S-curved bridge, smooth continuous periwinkle/lavender to sky blue to mint/aqua gradient, edges and positioning. All background pixels must be transparent including the counters and outer area. Do not add any square, rounded tile, text, shadow, glow, border, object, or extra symbol. This is a clean transparent master logo for a light app header. Preserve the symbol, remove only the background.

param(
 [string]$SdkRoot=$env:ANDROID_SDK_ROOT,
 [string]$JdkRoot=$env:JAVA_HOME,
 [string]$KeyStore=$env:WW_KEYSTORE,
 [switch]$Unsigned
)
$ErrorActionPreference='Stop'
if(-not $SdkRoot -or -not $JdkRoot){throw 'Set ANDROID_SDK_ROOT and JAVA_HOME (JDK 17).'}
$root=$PSScriptRoot
$app=Join-Path $root 'app'
$build=Join-Path $root 'build'
$classes=Join-Path $build 'classes'
$dex=Join-Path $build 'dex'
$tools=Join-Path $SdkRoot 'build-tools/34.0.0'
$androidJar=Join-Path $SdkRoot 'platforms/android-32/android.jar'
$bin=Join-Path $JdkRoot 'bin'
foreach($required in @($androidJar,(Join-Path $tools 'aapt.exe'),(Join-Path $bin 'javac.exe'))){if(-not(Test-Path -LiteralPath $required)){throw "Missing build dependency: $required"}}
# Delete only this repository's generated build directory.
$fullRoot=[IO.Path]::GetFullPath($root)+[IO.Path]::DirectorySeparatorChar
$fullBuild=[IO.Path]::GetFullPath($build)
if(-not $fullBuild.StartsWith($fullRoot,[StringComparison]::OrdinalIgnoreCase)){throw 'Build path outside repository'}
if(Test-Path -LiteralPath $fullBuild){Remove-Item -LiteralPath $fullBuild -Recurse -Force}
New-Item -ItemType Directory -Path $classes,$dex -Force | Out-Null
& (Join-Path $bin 'javac.exe') -encoding UTF-8 -source 8 -target 8 -classpath $androidJar -d $classes (Get-ChildItem -LiteralPath $app -Filter '*.java').FullName
if($LASTEXITCODE -ne 0){throw 'javac failed'}
& (Join-Path $bin 'java.exe') -cp (Join-Path $tools 'lib/d8.jar') com.android.tools.r8.D8 --min-api 26 --lib $androidJar --output $dex (Get-ChildItem -LiteralPath $classes -Recurse -Filter '*.class').FullName
if($LASTEXITCODE -ne 0){throw 'D8 failed'}
$raw=Join-Path $build 'ww-unsigned.apk'
& (Join-Path $tools 'aapt.exe') package -f -M (Join-Path $app 'AndroidManifest.xml') -S (Join-Path $app 'res') -I $androidJar -F $raw
if($LASTEXITCODE -ne 0){throw 'aapt failed'}
Push-Location $dex
try{& (Join-Path $tools 'aapt.exe') add $raw classes.dex;if($LASTEXITCODE -ne 0){throw 'aapt add failed'}}finally{Pop-Location}
$aligned=Join-Path $build 'ww-aligned.apk'
& (Join-Path $tools 'zipalign.exe') -f 4 $raw $aligned
if($LASTEXITCODE -ne 0){throw 'zipalign failed'}
if($Unsigned){Write-Output $aligned;return}
if(-not $KeyStore -or -not $env:WW_KEYSTORE_PASSWORD){throw 'Set WW_KEYSTORE and WW_KEYSTORE_PASSWORD; the signing key is never stored in this repository.'}
$signed=Join-Path $build 'WWave-1.6.apk'
& (Join-Path $bin 'java.exe') -jar (Join-Path $tools 'lib/apksigner.jar') sign --ks $KeyStore --ks-pass env:WW_KEYSTORE_PASSWORD --out $signed $aligned
if($LASTEXITCODE -ne 0){throw 'APK signing failed'}
& (Join-Path $bin 'java.exe') -jar (Join-Path $tools 'lib/apksigner.jar') verify $signed
if($LASTEXITCODE -ne 0){throw 'APK verification failed'}
Write-Output $signed

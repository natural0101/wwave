param([string]$JdkRoot=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(-not $JdkRoot){throw 'Set JAVA_HOME or pass -JdkRoot'}
$root=Split-Path -Parent $PSScriptRoot
$out=Join-Path $root 'build/tests'
New-Item -ItemType Directory -Path $out -Force | Out-Null
& (Join-Path $JdkRoot 'bin/javac.exe') -encoding UTF-8 -d $out (Join-Path $root 'app/NativeBacklight.java') (Join-Path $root 'app/ScheduleWindow.java') (Join-Path $PSScriptRoot 'CompatibilityTests.java')
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& (Join-Path $JdkRoot 'bin/java.exe') -cp $out local.tclbrightness.CompatibilityTests
if($LASTEXITCODE -ne 0){throw 'Compatibility tests failed'}

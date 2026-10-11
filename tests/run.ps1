param([string]$JdkRoot=$env:JAVA_HOME)
$ErrorActionPreference='Stop'
if(-not $JdkRoot){throw 'Set JAVA_HOME or pass -JdkRoot'}
$root=Split-Path -Parent $PSScriptRoot
$out=Join-Path $root 'build/tests'
New-Item -ItemType Directory -Path $out -Force | Out-Null
$deps=Join-Path $root 'build/test-deps'
New-Item -ItemType Directory -Path $deps -Force | Out-Null
$json=Join-Path $deps 'json.jar'
if(-not(Test-Path -LiteralPath $json)){Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar' -OutFile $json}
if((Get-FileHash -LiteralPath $json -Algorithm SHA256).Hash -ne '3CF6CD6892E32E2B4C1C39E0F52F5248A2F5B37646FDFBB79A66B46B618414ED'){throw 'Unexpected test JSON dependency hash'}
& (Join-Path $JdkRoot 'bin/javac.exe') -encoding UTF-8 -cp $json -d $out (Join-Path $root 'app/BacklightScale.java') (Join-Path $root 'app/NativeBacklight.java') (Join-Path $root 'app/RestBacklight.java') (Join-Path $root 'app/LocalTvHttp.java') (Join-Path $root 'app/ScheduleWindow.java') (Join-Path $PSScriptRoot 'CompatibilityTests.java') (Join-Path $PSScriptRoot 'RestTests.java')
if($LASTEXITCODE -ne 0){throw 'Test compilation failed'}
& (Join-Path $JdkRoot 'bin/java.exe') -cp $out local.tclbrightness.CompatibilityTests
if($LASTEXITCODE -ne 0){throw 'Compatibility tests failed'}
& (Join-Path $JdkRoot 'bin/java.exe') -cp "$out;$json" local.tclbrightness.RestTests
if($LASTEXITCODE -ne 0){throw 'REST adapter tests failed'}

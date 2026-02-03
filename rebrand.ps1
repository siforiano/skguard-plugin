$baseDir = "e:\proyectos propios\plugin seguridad"
$oldName = "SoulGuard"
$newName = "SKGuard"
$oldPkg = "soulguard"
$newPkg = "skguard"

function Rebrand-Files {
    param($targetDir)
    Get-ChildItem -Path $targetDir -Recurse -File | ForEach-Object {
        $content = Get-Content $_.FullName -Raw
        
        # String replacements
        $content = $content -replace "com.$oldPkg", "com.$newPkg"
        $content = $content -replace $oldName, $newName
        $content = $content -replace $oldPkg, $newPkg
        
        # Filename replacement
        $newNameFile = $_.Name -replace $oldName, $newName
        $newPath = Join-Path $_.DirectoryName $newNameFile
        
        Set-Content -Path $_.FullName -Value $content
        if ($_.Name -ne $newNameFile) {
            Move-Item -Path $_.FullName -Destination $newPath -Force
        }
    }
}

# 1. First, move the package directory
$oldPkgDir = Join-Path $baseDir "src\main\java\com\$oldPkg"
$newPkgDir = Join-Path $baseDir "src\main\java\com\$newPkg"

if (Test-Path $oldPkgDir) {
    if (Test-Path $newPkgDir) {
        Remove-Item -Path $newPkgDir -Recurse -Force
    }
    Move-Item -Path $oldPkgDir -Destination $newPkgDir
}

# 2. Rebrand everything in the new package dir
Rebrand-Files $newPkgDir

# 3. Rebrand resources
$resDir = Join-Path $baseDir "src\main\resources"
Rebrand-Files $resDir

# 4. Handle root files (pom.xml, build.sh, etc)
$rootFiles = @("pom.xml", "build.sh", "README.md")
foreach ($f in $rootFiles) {
    $path = Join-Path $baseDir $f
    if (Test-Path $path) {
        $content = Get-Content $path -Raw
        $content = $content -replace "com.$oldPkg", "com.$newPkg"
        $content = $content -replace $oldName, $newName
        $content = $content -replace $oldPkg, $newPkg
        Set-Content -Path $path -Value $content
    }
}

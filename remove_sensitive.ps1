$ErrorActionPreference = "Continue"
cd d:\JAVA-basic-code\questions-magic-tool

Write-Host "Backing up files..."
mkdir d:\temp_backup_config -ErrorAction SilentlyContinue
Copy-Item src\main\resources\application*.yml d:\temp_backup_config\ -ErrorAction SilentlyContinue

Write-Host "Committing current changes..."
git add .
git commit -m "Save local changes before history rewrite"

Write-Host "Running git filter-branch..."
# Using bash-compatible quotes for the index-filter since filter-branch runs in sh
git filter-branch --force --index-filter 'git rm --cached --ignore-unmatch src/main/resources/application.yml src/main/resources/application-test.yml src/main/resources/application-prod.yml' --prune-empty --tag-name-filter cat -- --all

Write-Host "Restoring files..."
Copy-Item d:\temp_backup_config\application*.yml src\main\resources\ -ErrorAction SilentlyContinue

Write-Host "Cleaning up git refs..."
git for-each-ref --format="%(refname)" refs/original/ | ForEach-Object { git update-ref -d $_ }
git reflog expire --expire=now --all
git gc --prune=now

Write-Host "Adding files to .gitignore if not present..."
$gitignore = ".gitignore"
$files = @("application.yml", "application-test.yml", "application-prod.yml")
foreach ($file in $files) {
    if (-not (Select-String -Path $gitignore -Pattern "^$file$" -Quiet)) {
        Add-Content -Path $gitignore -Value $file
    }
}

Write-Host "Done!"

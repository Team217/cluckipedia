# Remove Local & Global Branches on VSC
## (That have been deleted globally)
1. Open a new `Git Bash` terminal
2. Run this command: ```git fetch --prune```
3. Run this command: ```git branch -vv | grep ': gone]' | sed 's/^[* ]*//' | awk '{print $1}' | xargs -r git branch -D```

⚠️ If you do not have `grep`, run the commands below in a **POWERSHELL** terminal with **administrator**
```
winget install --id Git.Git -e --source winget

$PATH = [Environment]::GetEnvironmentVariable("PATH", "Machine"); [Environment]::SetEnvironmentVariable("PATH", "$PATH;C:\Program Files\Git\usr\bin", "Machine")
```
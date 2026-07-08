Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$DefaultOwner = "martin-gnahn"
$DefaultProjectNumber = 3
$DefaultTicketDirs = @(
    "Plans/partygames_tickets/refactoring-tickets",
    "Plans/partygames_tickets/tickets",
    "Plans/partygames_tickets/side-tickets"
)

function Show-Usage {
    Write-Host "Usage:"
    Write-Host "  .\scripts\sync-project.ps1 --dry-run [--ticket-dir <path>] [--owner martin-gnahn] [--project-number 3]"
    Write-Host "  .\scripts\sync-project.ps1 --push    [--ticket-dir <path>] [--owner martin-gnahn] [--project-number 3]"
    Write-Host ""
    Write-Host "Default ticket dirs:"
    foreach ($ticketDir in $DefaultTicketDirs) {
        Write-Host "  - $ticketDir"
    }
    Write-Host ""
    Write-Host "Pass --ticket-dir multiple times to override the defaults."
}

function Read-Options {
    $options = @{
        Mode = $null
        Owner = $DefaultOwner
        ProjectNumber = $DefaultProjectNumber
        TicketDirs = @($DefaultTicketDirs)
        HasCustomTicketDirs = $false
    }

    for ($i = 0; $i -lt $args.Count; $i++) {
        switch ($args[$i]) {
            "--dry-run" {
                if ($options.Mode) { throw "Choose exactly one mode: --dry-run or --push." }
                $options.Mode = "DryRun"
            }
            "--push" {
                if ($options.Mode) { throw "Choose exactly one mode: --dry-run or --push." }
                $options.Mode = "Push"
            }
            "--owner" {
                $i++
                if ($i -ge $args.Count) { throw "--owner requires a value." }
                $options.Owner = $args[$i]
            }
            "--project-number" {
                $i++
                if ($i -ge $args.Count) { throw "--project-number requires a value." }
                $options.ProjectNumber = [int]$args[$i]
            }
            "--ticket-dir" {
                $i++
                if ($i -ge $args.Count) { throw "--ticket-dir requires a value." }
                if (-not $options.HasCustomTicketDirs) {
                    $options.TicketDirs = @()
                    $options.HasCustomTicketDirs = $true
                }
                $options.TicketDirs += $args[$i]
            }
            "--help" {
                Show-Usage
                exit 0
            }
            default {
                throw "Unknown argument '$($args[$i])'."
            }
        }
    }

    if (-not $options.Mode) {
        $options.Mode = "DryRun"
        Write-Host "No mode specified. Running in --dry-run mode."
    }

    return [pscustomobject]$options
}

function Invoke-GhJson {
    param([string[]]$Arguments)

    $output = & gh @Arguments 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "gh $($Arguments -join ' ') failed:`n$($output -join [Environment]::NewLine)"
    }

    $text = $output -join [Environment]::NewLine
    if ([string]::IsNullOrWhiteSpace($text)) {
        return $null
    }

    return $text | ConvertFrom-Json
}

function Invoke-GhGraphQl {
    param(
        [string]$Query,
        [hashtable]$Variables = @{},
        [hashtable]$FileVariables = @{}
    )

    $tempFiles = New-Object System.Collections.Generic.List[string]
    try {
        $queryFile = [System.IO.Path]::GetTempFileName()
        $tempFiles.Add($queryFile)
        [System.IO.File]::WriteAllText($queryFile, $Query, [System.Text.UTF8Encoding]::new($false))

        $arguments = @("api", "graphql", "-F", "query=@$queryFile")
        foreach ($key in $Variables.Keys) {
            $arguments += "-F"
            $arguments += "${key}=$($Variables[$key])"
        }
        foreach ($key in $FileVariables.Keys) {
            $valueFile = [System.IO.Path]::GetTempFileName()
            $tempFiles.Add($valueFile)
            [System.IO.File]::WriteAllText($valueFile, $FileVariables[$key], [System.Text.UTF8Encoding]::new($false))
            $arguments += "-F"
            $arguments += "${key}=@$valueFile"
        }

        return Invoke-GhJson -Arguments $arguments
    } finally {
        foreach ($tempFile in $tempFiles) {
            if (Test-Path -LiteralPath $tempFile) {
                Remove-Item -LiteralPath $tempFile -Force
            }
        }
    }
}

function Add-ProjectDraftIssue {
    param(
        [string]$ProjectId,
        [string]$Title,
        [string]$Body
    )

    $query = @"
mutation AddProjectDraftIssue(`$projectId: ID!, `$title: String!, `$body: String!) {
  addProjectV2DraftIssue(input: { projectId: `$projectId, title: `$title, body: `$body }) {
    projectItem {
      id
      content {
        ... on DraftIssue {
          id
        }
      }
    }
  }
}
"@

    $result = Invoke-GhGraphQl -Query $query -Variables @{
        projectId = $ProjectId
        title = $Title
    } -FileVariables @{
        body = $Body
    }

    return $result.data.addProjectV2DraftIssue.projectItem
}

function Update-ProjectDraftIssue {
    param(
        [string]$DraftIssueId,
        [string]$Title,
        [string]$Body
    )

    $query = @"
mutation UpdateProjectDraftIssue(`$draftIssueId: ID!, `$title: String!, `$body: String!) {
  updateProjectV2DraftIssue(input: { draftIssueId: `$draftIssueId, title: `$title, body: `$body }) {
    draftIssue {
      id
    }
  }
}
"@

    [void](Invoke-GhGraphQl -Query $query -Variables @{
        draftIssueId = $DraftIssueId
        title = $Title
    } -FileVariables @{
        body = $Body
    })
}

function Get-FrontMatter {
    param([string]$Content)

    $result = @{
        Values = @{}
        Body = $Content
        HasFrontMatter = $false
    }

    if (-not $Content.StartsWith("---")) {
        return [pscustomobject]$result
    }

    $normalized = $Content -replace "`r`n", "`n"
    $lines = $normalized -split "`n", -1
    if ($lines.Count -lt 3 -or $lines[0] -ne "---") {
        return [pscustomobject]$result
    }

    $endIndex = $null
    for ($i = 1; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -eq "---") {
            $endIndex = $i
            break
        }
    }

    if ($null -eq $endIndex) {
        return [pscustomobject]$result
    }

    for ($i = 1; $i -lt $endIndex; $i++) {
        if ($lines[$i] -match "^\s*([A-Za-z0-9_-]+)\s*:\s*(.*)\s*$") {
            $key = $Matches[1]
            $value = $Matches[2].Trim()
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }
            $result.Values[$key] = $value
        }
    }

    $bodyLines = @()
    if ($endIndex + 1 -lt $lines.Count) {
        $bodyLines = $lines[($endIndex + 1)..($lines.Count - 1)]
    }

    $result.HasFrontMatter = $true
    $result.Body = ($bodyLines -join "`n").TrimStart("`n")
    return [pscustomobject]$result
}

function Set-FrontMatterValues {
    param(
        [string]$Path,
        [hashtable]$ValuesToSet
    )

    $content = Get-Content -LiteralPath $Path -Raw
    $frontMatter = Get-FrontMatter -Content $content
    $values = @{}

    foreach ($key in $frontMatter.Values.Keys) {
        $values[$key] = $frontMatter.Values[$key]
    }
    foreach ($key in $ValuesToSet.Keys) {
        if ($ValuesToSet[$key]) {
            $values[$key] = $ValuesToSet[$key]
        }
    }

    $preferredOrder = @("ticket_id", "title", "status", "project_item_id", "draft_issue_id")
    $orderedKeys = New-Object System.Collections.Generic.List[string]
    foreach ($key in $preferredOrder) {
        if ($values.ContainsKey($key)) {
            $orderedKeys.Add($key)
        }
    }
    foreach ($key in ($values.Keys | Sort-Object)) {
        if (-not $orderedKeys.Contains($key)) {
            $orderedKeys.Add($key)
        }
    }

    $frontMatterLines = @("---")
    foreach ($key in $orderedKeys) {
        $frontMatterLines += "${key}: $($values[$key])"
    }
    $frontMatterLines += "---"
    $frontMatterLines += ""
    $newContent = (($frontMatterLines -join "`r`n") + ($frontMatter.Body.TrimStart() -replace "`n", "`r`n"))

    Set-Content -LiteralPath $Path -Value $newContent -Encoding UTF8
}

function Get-TicketIdFromText {
    param([string]$Text)

    if ([string]::IsNullOrWhiteSpace($Text)) {
        return $null
    }

    if ($Text -match "(?i)([A-Z]+-\d+)") {
        return $Matches[1]
    }

    return $null
}

function Normalize-Title {
    param([string]$Title)

    if ($null -eq $Title) { return "" }
    return ($Title -replace "^\s*([A-Z]+-\d+)\s*:\s*", '$1 ' -replace "\s+", " ").Trim().ToLowerInvariant()
}

function Get-FirstHeading {
    param([string]$Content)

    foreach ($line in ($Content -replace "`r`n", "`n" -split "`n")) {
        if ($line -match "^\s*#{1,6}\s+(.+?)\s*$") {
            return $Matches[1].Trim()
        }
    }

    return $null
}

function Get-StatusFromContent {
    param([string]$Content)

    $lines = $Content -replace "`r`n", "`n" -split "`n"
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match "^\s*##\s+Status\s*$") {
            for ($j = $i + 1; $j -lt $lines.Count; $j++) {
                $candidate = $lines[$j].Trim()
                if ($candidate) {
                    return $candidate.TrimEnd(".")
                }
            }
        }
    }

    return $null
}

function Convert-ToProjectStatus {
    param([string]$Status)

    if ($null -eq $Status) {
        $Status = ""
    }
    $normalized = $Status.Trim().TrimEnd(".").ToLowerInvariant()
    switch ($normalized) {
        "done" { return "Done" }
        "done, unverified" { return "Done" }
        "in review" { return "In Review" }
        "review" { return "In Review" }
        "in progress" { return "In Progress" }
        "partial" { return "In Progress" }
        "open" { return "Todo" }
        "todo" { return "Todo" }
        "to do" { return "Todo" }
        default { return "Todo" }
    }
}

function Get-TrackerStatuses {
    param([string[]]$TicketDirs)

    $statuses = @{}
    foreach ($ticketDir in $TicketDirs) {
        $trackerPath = Join-Path $ticketDir "TICKET_PROGRESS.md"
        if (-not (Test-Path -LiteralPath $trackerPath)) {
            continue
        }

        $lines = Get-Content -LiteralPath $trackerPath
        foreach ($line in $lines) {
            if ($line -match "^\|\s*([A-Z]+-\d+)\s*\|\s*[^|]+\|\s*([^|]+?)\s*\|") {
                $ticketId = $Matches[1].Trim()
                $status = $Matches[2].Trim()
                if ($ticketId -ne "Ticket" -and $status -ne "Status") {
                    $statuses[$ticketId] = $status
                }
            }
        }
    }

    return $statuses
}

function Normalize-Body {
    param([string]$Body)

    if ($null -eq $Body) { return "" }
    return (($Body -replace "`r`n", "`n").Trim() -replace "[ `t]+`n", "`n")
}

function Get-CanonicalTicketBody {
    param([string]$Body)

    if ($null -eq $Body) { return "" }

    $normalized = $Body -replace "`r`n", "`n"
    $lines = $normalized -split "`n", -1

    $firstContentIndex = $null
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if (-not [string]::IsNullOrWhiteSpace($lines[$i])) {
            $firstContentIndex = $i
            break
        }
    }

    if ($null -eq $firstContentIndex) {
        return ""
    }

    $firstContentLine = $lines[$firstContentIndex].Trim()
    $startsWithProjectMetadata = $firstContentLine -match "^(Tracker status|Project status|Notes|Verification|Source):"

    if ($startsWithProjectMetadata) {
        for ($i = $firstContentIndex; $i -lt $lines.Count; $i++) {
            if ($lines[$i].Trim() -eq "---") {
                if ($i + 1 -lt $lines.Count) {
                    $canonical = ($lines[($i + 1)..($lines.Count - 1)] -join "`n").Trim()
                    return ($canonical -replace "(?s)\n---\s*$", "").Trim()
                }
                return ""
            }
        }

        return ""
    }

    return (($normalized.Trim()) -replace "(?s)\n---\s*$", "").Trim()
}

function Normalize-CanonicalBody {
    param([string]$Body)

    return Normalize-Body -Body (Get-CanonicalTicketBody -Body $Body)
}

function Get-RelativePath {
    param(
        [string]$Root,
        [string]$Path
    )

    $rootFull = [System.IO.Path]::GetFullPath($Root)
    $pathFull = [System.IO.Path]::GetFullPath($Path)
    if (-not $rootFull.EndsWith([System.IO.Path]::DirectorySeparatorChar)) {
        $rootFull += [System.IO.Path]::DirectorySeparatorChar
    }

    $rootUri = [Uri]::new($rootFull)
    $pathUri = [Uri]::new($pathFull)
    return [Uri]::UnescapeDataString($rootUri.MakeRelativeUri($pathUri).ToString()).Replace("/", [System.IO.Path]::DirectorySeparatorChar)
}

function Get-LocalTickets {
    param(
        [string[]]$TicketDirs,
        [hashtable]$StatusByTicketId
    )

    $repoRoot = (Get-Location).Path
    $tickets = @()

    foreach ($ticketDir in $TicketDirs) {
        if (-not (Test-Path -LiteralPath $ticketDir)) {
            throw "Ticket directory '$ticketDir' does not exist."
        }

        $files = Get-ChildItem -LiteralPath $ticketDir -Filter "*.md" -File |
            Where-Object {
                $_.Name -ne "README.md" -and
                $_.Name -notlike "ticket-sync-*.md" -and
                $_.BaseName -match "(?i)^(PG|B|R)-\d+"
            }

        foreach ($file in $files) {
            $content = Get-Content -LiteralPath $file.FullName -Raw
            $frontMatter = Get-FrontMatter -Content $content
            $body = $frontMatter.Body.Trim()
            $title = $frontMatter.Values["title"]
            if (-not $title) { $title = Get-FirstHeading -Content $body }
            if (-not $title) { $title = [System.IO.Path]::GetFileNameWithoutExtension($file.Name) }

            $ticketId = $frontMatter.Values["ticket_id"]
            if (-not $ticketId) { $ticketId = Get-TicketIdFromText -Text $title }
            if (-not $ticketId) { $ticketId = Get-TicketIdFromText -Text $file.Name }

            $rawStatus = $frontMatter.Values["status"]
            if (-not $rawStatus) { $rawStatus = Get-StatusFromContent -Content $body }
            if (-not $rawStatus -and $ticketId -and $StatusByTicketId.ContainsKey($ticketId)) {
                $rawStatus = $StatusByTicketId[$ticketId]
            }
            if (-not $rawStatus) { $rawStatus = "Todo" }
            $projectStatus = Convert-ToProjectStatus -Status $rawStatus
            $relativePath = Get-RelativePath -Root $repoRoot -Path $file.FullName
            $sourcePath = $file.FullName

            $projectBody = @"
Tracker status: $rawStatus

Project status: $projectStatus

Source: $sourcePath

---

$body
"@.Trim()

            $tickets += [pscustomobject]@{
                TicketId = $ticketId
                Title = $title
                RawStatus = $rawStatus
                ProjectStatus = $projectStatus
                Path = $file.FullName
                RelativePath = $relativePath
                Body = $body
                ProjectBody = $projectBody
                ProjectItemId = $frontMatter.Values["project_item_id"]
                DraftIssueId = $frontMatter.Values["draft_issue_id"]
            }
        }
    }

    return $tickets
}

function Get-ProjectContext {
    param(
        [string]$Owner,
        [int]$ProjectNumber
    )

    $projectList = Invoke-GhJson -Arguments @("project", "list", "--owner", $Owner, "--format", "json")
    $project = $projectList.projects | Where-Object { $_.number -eq $ProjectNumber } | Select-Object -First 1
    if (-not $project) {
        throw "Project $ProjectNumber for owner '$Owner' was not found."
    }

    $fields = Invoke-GhJson -Arguments @("project", "field-list", "$ProjectNumber", "--owner", $Owner, "--format", "json")
    $statusField = $fields.fields | Where-Object { $_.name -eq "Status" } | Select-Object -First 1
    if (-not $statusField) {
        throw "Project $ProjectNumber has no Status field."
    }

    $statusOptions = @{}
    foreach ($option in $statusField.options) {
        $statusOptions[$option.name] = $option.id
    }

    return [pscustomobject]@{
        ProjectId = $project.id
        StatusFieldId = $statusField.id
        StatusOptions = $statusOptions
    }
}

function Get-ProjectItems {
    param(
        [string]$Owner,
        [int]$ProjectNumber
    )

    $itemsJson = Invoke-GhJson -Arguments @("project", "item-list", "$ProjectNumber", "--owner", $Owner, "--limit", "100", "--format", "json")
    $items = @()

    foreach ($item in $itemsJson.items) {
        $contentId = $null
        $body = ""
        if ($item.content) {
            $contentId = $item.content.id
            $body = $item.content.body
        }

        $ticketId = Get-TicketIdFromText -Text $item.title
        if (-not $ticketId -and $body) {
            $ticketId = Get-TicketIdFromText -Text $body
        }

        $items += [pscustomobject]@{
            TicketId = $ticketId
            Title = $item.title
            Status = $item.status
            ProjectItemId = $item.id
            ContentId = $contentId
            Body = $body
            NormalizedTitle = Normalize-Title -Title $item.title
        }
    }

    return $items
}

function Find-MatchingProjectItem {
    param(
        [pscustomobject]$Ticket,
        [object[]]$ProjectItems
    )

    if ($Ticket.ProjectItemId) {
        $byId = $ProjectItems | Where-Object { $_.ProjectItemId -eq $Ticket.ProjectItemId } | Select-Object -First 1
        if ($byId) { return $byId }
    }
    if ($Ticket.DraftIssueId) {
        $byContentId = $ProjectItems | Where-Object { $_.ContentId -eq $Ticket.DraftIssueId } | Select-Object -First 1
        if ($byContentId) { return $byContentId }
    }
    if ($Ticket.TicketId) {
        $byTicketId = $ProjectItems | Where-Object { $_.TicketId -eq $Ticket.TicketId } | Select-Object -First 1
        if ($byTicketId) { return $byTicketId }
    }

    $localTitle = Normalize-Title -Title $Ticket.Title
    return $ProjectItems | Where-Object { $_.NormalizedTitle -eq $localTitle } | Select-Object -First 1
}

function Get-DriftReport {
    param(
        [object[]]$LocalTickets,
        [object[]]$ProjectItems
    )

    $matchedProjectIds = New-Object System.Collections.Generic.HashSet[string]
    $rows = @()

    foreach ($ticket in $LocalTickets) {
        $projectItem = Find-MatchingProjectItem -Ticket $ticket -ProjectItems $ProjectItems
        if (-not $projectItem) {
            $rows += [pscustomobject]@{
                Ticket = $ticket.TicketId
                Title = $ticket.Title
                Drift = "Only local"
                LocalStatus = $ticket.ProjectStatus
                ProjectStatus = ""
                Path = $ticket.RelativePath
            }
            continue
        }

        [void]$matchedProjectIds.Add($projectItem.ProjectItemId)
        $drifts = New-Object System.Collections.Generic.List[string]
        if ((Normalize-Title -Title $ticket.Title) -ne (Normalize-Title -Title $projectItem.Title)) {
            $drifts.Add("Different title")
        }
        if ($ticket.ProjectStatus -ne $projectItem.Status) {
            $drifts.Add("Different status")
        }
        if ((Normalize-CanonicalBody -Body $ticket.Body) -ne (Normalize-CanonicalBody -Body $projectItem.Body)) {
            $drifts.Add("Different content")
        }
        if ($drifts.Count -gt 0) {
            $rows += [pscustomobject]@{
                Ticket = $ticket.TicketId
                Title = $ticket.Title
                Drift = ($drifts -join ", ")
                LocalStatus = $ticket.ProjectStatus
                ProjectStatus = $projectItem.Status
                Path = $ticket.RelativePath
            }
        }
    }

    foreach ($item in $ProjectItems) {
        if ($matchedProjectIds.Contains($item.ProjectItemId)) {
            continue
        }

        $rows += [pscustomobject]@{
            Ticket = $item.TicketId
            Title = $item.Title
            Drift = "Only GitHub"
            LocalStatus = ""
            ProjectStatus = $item.Status
            Path = ""
        }
    }

    return $rows
}

function Set-ProjectStatus {
    param(
        [pscustomobject]$ProjectContext,
        [string]$ProjectItemId,
        [string]$Status
    )

    $optionId = $ProjectContext.StatusOptions[$Status]
    if (-not $optionId) {
        throw "Project has no status option '$Status'."
    }

    [void](Invoke-GhJson -Arguments @(
        "project", "item-edit",
        "--id", $ProjectItemId,
        "--project-id", $ProjectContext.ProjectId,
        "--field-id", $ProjectContext.StatusFieldId,
        "--single-select-option-id", $optionId,
        "--format", "json"
    ))
}

function Push-LocalTickets {
    param(
        [object[]]$LocalTickets,
        [object[]]$ProjectItems,
        [pscustomobject]$ProjectContext,
        [string]$Owner,
        [int]$ProjectNumber
    )

    foreach ($ticket in $LocalTickets) {
        $projectItem = Find-MatchingProjectItem -Ticket $ticket -ProjectItems $ProjectItems
        if (-not $projectItem) {
            Write-Host "Creating GitHub Project item: $($ticket.Title)"
            $created = Add-ProjectDraftIssue -ProjectId $ProjectContext.ProjectId -Title $ticket.Title -Body $ticket.ProjectBody

            $projectItemId = $created.id
            $contentId = $null
            if ($created.content) {
                $contentId = $created.content.id
            }

            Set-ProjectStatus -ProjectContext $ProjectContext -ProjectItemId $projectItemId -Status $ticket.ProjectStatus
            Set-FrontMatterValues -Path $ticket.Path -ValuesToSet @{
                ticket_id = $ticket.TicketId
                title = $ticket.Title
                status = $ticket.ProjectStatus
                project_item_id = $projectItemId
                draft_issue_id = $contentId
            }
            continue
        }

        $bodyId = if ($projectItem.ContentId) { $projectItem.ContentId } else { $projectItem.ProjectItemId }

        $titleDiffers = (Normalize-Title -Title $ticket.Title) -ne (Normalize-Title -Title $projectItem.Title)
        $bodyDiffers = (Normalize-CanonicalBody -Body $ticket.Body) -ne (Normalize-CanonicalBody -Body $projectItem.Body)
        if ($titleDiffers -or $bodyDiffers) {
            if (-not $projectItem.ContentId) {
                throw "Cannot update title/body for '$($ticket.Title)' because the project item has no draft issue content ID."
            }
            Write-Host "Updating title/body: $($ticket.Title)"
            Update-ProjectDraftIssue -DraftIssueId $bodyId -Title $ticket.Title -Body $ticket.ProjectBody
        }

        if ($ticket.ProjectStatus -ne $projectItem.Status) {
            Write-Host "Updating status: $($ticket.Title) -> $($ticket.ProjectStatus)"
            Set-ProjectStatus -ProjectContext $ProjectContext -ProjectItemId $projectItem.ProjectItemId -Status $ticket.ProjectStatus
        }

        if (-not $ticket.ProjectItemId -or -not $ticket.DraftIssueId) {
            Write-Host "Writing GitHub IDs into local front matter: $($ticket.RelativePath)"
            Set-FrontMatterValues -Path $ticket.Path -ValuesToSet @{
                ticket_id = $ticket.TicketId
                title = $ticket.Title
                status = $ticket.ProjectStatus
                project_item_id = $projectItem.ProjectItemId
                draft_issue_id = $projectItem.ContentId
            }
        }
    }
}

$options = Read-Options @args
$ticketDirs = @($options.TicketDirs | ForEach-Object { (Resolve-Path -LiteralPath $_).Path })

Write-Host "Ticket directories:"
foreach ($ticketDir in $ticketDirs) {
    Write-Host "  - $ticketDir"
}
Write-Host "GitHub Project: $($options.Owner) / #$($options.ProjectNumber)"
Write-Host "Mode: $($options.Mode)"
Write-Host ""

$statusByTicketId = Get-TrackerStatuses -TicketDirs $ticketDirs
$localTickets = @(Get-LocalTickets -TicketDirs $ticketDirs -StatusByTicketId $statusByTicketId)
$projectContext = Get-ProjectContext -Owner $options.Owner -ProjectNumber $options.ProjectNumber
$projectItems = @(Get-ProjectItems -Owner $options.Owner -ProjectNumber $options.ProjectNumber)
$drift = @(Get-DriftReport -LocalTickets $localTickets -ProjectItems $projectItems)

if ($drift.Count -eq 0) {
    Write-Host "No drift found."
} else {
    Write-Host "Drift found:"
    $table = $drift |
        Sort-Object Drift, Ticket, Title |
        Format-Table Ticket, Title, Drift, LocalStatus, ProjectStatus, Path -AutoSize -Wrap |
        Out-String -Width 240
    Write-Host $table
}

if ($options.Mode -eq "Push") {
    Write-Host ""
    Write-Host "Pushing local tickets to GitHub Project..."
    Push-LocalTickets -LocalTickets $localTickets -ProjectItems $projectItems -ProjectContext $projectContext -Owner $options.Owner -ProjectNumber $options.ProjectNumber
    Write-Host "Push complete."
} else {
    Write-Host ""
    Write-Host "Dry run only. Re-run with --push to create/update GitHub Project items from local markdown."
}

# Prints every team document in this folder to a PDF in docs\team\pdf\.
#
# The HTML files are the source. The PDFs are what gets sent round, because they
# open the same on a phone and print the same on any machine. Run this again after
# editing any of the HTML.

$ErrorActionPreference = 'Stop'
$here = $PSScriptRoot
$out = Join-Path $here 'pdf'
New-Item -ItemType Directory -Force $out | Out-Null

$chrome = @(
    'C:\Program Files\Google\Chrome\Application\chrome.exe',
    'C:\Program Files (x86)\Google\Chrome\Application\chrome.exe',
    'C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe'
) | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $chrome) { throw 'Chrome or Edge is needed to print the PDFs' }

Get-ChildItem $here -Filter '*.html' | Sort-Object Name | ForEach-Object {
    $pdf = Join-Path $out ($_.BaseName + '.pdf')
    $url = ([System.Uri]$_.FullName).AbsoluteUri
    # Chrome reports its progress on stderr, which a strict shell reads as a failure,
    # so it is started as its own process and only the file it writes is checked.
    Start-Process -FilePath $chrome -Wait -WindowStyle Hidden -ArgumentList @(
        '--headless=new', '--disable-gpu', '--no-pdf-header-footer',
        "--print-to-pdf=`"$pdf`"", "`"$url`"")
    # Chrome returns before the file is fully flushed on some machines.
    for ($i = 0; $i -lt 20 -and -not (Test-Path $pdf); $i++) { Start-Sleep -Milliseconds 250 }
    if (Test-Path $pdf) {
        Write-Host ("  {0,-38} {1,6} KB" -f $_.BaseName, [math]::Round((Get-Item $pdf).Length / 1KB))
    }
    else {
        Write-Host "  $($_.BaseName) FAILED" -ForegroundColor Red
    }
}
Write-Host "PDFs are in $out"

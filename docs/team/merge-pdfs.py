"""Joins the eight team PDFs into one bookmarked file, Team-Pack.pdf.

Run by build-pdfs.ps1 after the individual PDFs exist. Needs PyMuPDF
(pip install pymupdf); the script is skipped, not failed, when it is missing.
"""
import pathlib
import sys

try:
    import fitz
except ImportError:
    print("PyMuPDF is not installed, so Team-Pack.pdf was not built (pip install pymupdf)")
    sys.exit(0)

here = pathlib.Path(__file__).parent
pdfs = here / "pdf"
order = [
    ("Start here", "00-start-here"),
    ("Karan: the app itself", "01-karan-app-overview"),
    ("Yuvraj: MySQL and JDBC", "02-yuvraj-mysql-jdbc"),
    ("Tiya: accounts and security", "03-tiya-auth-security"),
    ("Kinjal: views and validation", "04-kinjal-views-validation"),
    ("Chandra: entities and EJB", "05-chandra-entities-ejb"),
    ("Viva questions", "06-viva-questions"),
    ("Demo run sheet", "07-demo-run-sheet"),
]

out = fitz.open()
toc = []
for title, stem in order:
    source = pdfs / f"{stem}.pdf"
    if not source.exists():
        print("missing", source.name)
        continue
    toc.append([1, title, out.page_count + 1])
    with fitz.open(source) as part:
        out.insert_pdf(part)
out.set_toc(toc)
out.set_metadata({"title": "Campus Placement and Training Cell: team pack",
                  "subject": "Advanced Java team briefs"})
target = pdfs / "Team-Pack.pdf"
out.save(target, deflate=True, garbage=3)
print(f"  {'Team-Pack':<38} {round(target.stat().st_size / 1024):>4} KB, {out.page_count} pages")

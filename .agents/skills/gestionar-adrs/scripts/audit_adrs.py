#!/usr/bin/env python3
"""Comprueba la estructura y trazabilidad básica de los ADRs del proyecto."""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path


ALLOWED_STATUSES = {"Propuesto", "Aceptado", "Reemplazado", "Descartado"}
REQUIRED_SECTIONS = (
    "Contexto",
    "Decisión",
    "Alternativas consideradas",
    "Consecuencias",
    "Requisitos relacionados",
    "Decisiones de Fase 1 relacionadas",
    "Validación prevista",
    "Decisiones pendientes",
)
ADR_FILENAME = re.compile(r"^(?P<number>\d{4})-(?P<slug>[a-z0-9]+(?:-[a-z0-9]+)*)\.md$")


@dataclass(frozen=True)
class Finding:
    level: str
    path: Path
    message: str


def finding(level: str, path: Path, message: str) -> Finding:
    return Finding(level=level, path=path, message=message)


def audit_adr(path: Path, root: Path) -> list[Finding]:
    findings: list[Finding] = []
    match = ADR_FILENAME.match(path.name)
    if match is None:
        return [finding("ERROR", path, "El nombre debe seguir NNNN-titulo-en-kebab-case.md.")]

    text = path.read_text(encoding="utf-8")
    number = match.group("number")
    expected_heading = f"# ADR-{number}:"
    if not text.startswith(expected_heading):
        findings.append(finding("ERROR", path, f"El título debe empezar por '{expected_heading}'."))

    status = re.search(r"^\*\*Estado:\*\*\s*(.+?)\s*$", text, re.MULTILINE)
    if status is None:
        findings.append(finding("ERROR", path, "Falta '**Estado:**'."))
    elif status.group(1) not in ALLOWED_STATUSES:
        findings.append(finding("ERROR", path, f"Estado no permitido: '{status.group(1)}'."))

    if not re.search(r"^\*\*Fecha:\*\*\s*\d{4}-\d{2}-\d{2}\s*$", text, re.MULTILINE):
        findings.append(finding("ERROR", path, "Falta una fecha ISO en '**Fecha:** YYYY-MM-DD'."))
    if not re.search(r"^\*\*Responsable de revisión:\*\*\s*.+$", text, re.MULTILINE):
        findings.append(finding("ERROR", path, "Falta el responsable de revisión."))

    required_sections = REQUIRED_SECTIONS
    if number == "0001":
        # ADR-0001 registra las decisiones de Fase 1 dentro de requisitos.
        required_sections = tuple(
            section for section in REQUIRED_SECTIONS if section != "Decisiones de Fase 1 relacionadas"
        )
    for section in required_sections:
        if not re.search(rf"^##\s+{re.escape(section)}\s*$", text, re.MULTILINE):
            findings.append(finding("ERROR", path, f"Falta la sección '## {section}'."))

    if "RF-XX" in text or "D-XX" in text or "YYYY-MM-DD" in text:
        findings.append(finding("ERROR", path, "Contiene marcadores sin completar de la plantilla."))
    if not re.search(r"RF-\d{2}|Todos los requisitos\s+`RF-01`", text):
        findings.append(finding("WARNING", path, "No se detectaron requisitos RF relacionados."))
    if path.relative_to(root).as_posix().endswith("0001-record-architecture-decisions.md"):
        return findings
    if not re.search(r"D-\d{2}", text):
        findings.append(finding("WARNING", path, "No se detectaron decisiones de Fase 1 relacionadas."))
    return findings


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd(), help="Raíz del repositorio.")
    parser.add_argument("--strict", action="store_true", help="Convierte avisos en errores.")
    args = parser.parse_args()

    root = args.root.resolve()
    adr_dir = root / "docs" / "adr"
    index = adr_dir / "README.md"
    if not adr_dir.is_dir() or not index.is_file():
        print("ERROR docs/adr/README.md: No se encontró el índice de ADRs.")
        return 1

    files = sorted(path for path in adr_dir.glob("*.md") if path.name not in {"README.md", "adr-template.md"})
    if not files:
        print("ERROR docs/adr: No hay ADRs para auditar.")
        return 1

    index_text = index.read_text(encoding="utf-8")
    findings = [item for path in files for item in audit_adr(path, root)]
    for path in files:
        number = ADR_FILENAME.match(path.name)
        if number is None:
            continue
        adr_id = f"ADR-{number.group('number')}"
        expected_link = f"[{adr_id}]({path.name})"
        if expected_link not in index_text:
            findings.append(finding("ERROR", index, f"Falta la entrada de {adr_id} para '{path.name}'."))

    errors = 0
    warnings = 0
    for item in findings:
        print(f"{item.level} {item.path.relative_to(root).as_posix()}: {item.message}")
        if item.level == "ERROR":
            errors += 1
        elif item.level == "WARNING":
            warnings += 1

    print(f"Resultado: {len(files)} ADR(s), {errors} error(es), {warnings} aviso(s).")
    return 1 if errors or (args.strict and warnings) else 0


if __name__ == "__main__":
    sys.exit(main())

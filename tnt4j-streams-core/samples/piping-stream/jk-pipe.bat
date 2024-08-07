@echo off
setlocal

set PARSERS_CFG=%*
IF ["%1"] EQU [""] (
  set PARSERS_CFG=parsers.xml
)

@echo off
..\..\bin\tnt4j-streams.bat -p -f:%PARSERS_CFG%

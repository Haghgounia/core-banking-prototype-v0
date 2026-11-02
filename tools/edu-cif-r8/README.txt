EDU-CIF R8 -- direct CMD controller installer

Run from project root:
  call tools\edu-cif-r8\INSTALL.cmd
  echo INSTALL_EXIT_CODE=%%ERRORLEVEL%%

The installer modifies ONLY EduLookupController.java and creates a single backup. It refuses unexpected mixed source. Does not modify Descriptor, Oracle or GEO. Build after PASS. If the Descriptor has not been changed to CIF, set SCHEMA=CIF separately before considering cutover complete.

Note: This patch is based on the confirmed R3 controller present in the user output. The installer uses a scoped guard for EDU view references and refuses mixed CIF references; it cannot guarantee no other changes to the Controller since R3. Review Git diff after installing.

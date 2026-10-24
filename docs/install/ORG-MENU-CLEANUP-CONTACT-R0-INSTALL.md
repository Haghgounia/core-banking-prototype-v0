# ORG: retired location forms hidden from menu, contact baseline started

The patch is a file overlay on the verified R2-A Hash URL Fix source tree.

1. Back up the existing project directory.
2. Overlay the small patch ZIP onto the project root, preserving relative paths.
3. From Windows Command Prompt in the project root run:
   `call tools\VERIFY-ORG-MENU-CONTACT-R0.cmd`
4. Rebuild and restart the application using the existing local `build-production.cmd` workflow. Merely extracting changed Angular source files will not update an already-running JAR.
5. Open `http://localhost:8091/#/organization` and confirm the three generic location/address forms no longer appear in ORG group or quick links; the canonical page remains accessible. Confirm the organization contact resources remain in their own group.
6. For contact data discovery run `database/oracle/org/contact-points/01-ORG-BRANCH-CONTACTS-PREVIEW-READONLY.sql` (SQL Developer F5) and share its results before a contact CRUD migration is designed.

No SQL migration is necessary to install the menu patch. No legacy tables or backend descriptors were dropped. Direct old URLs are not disabled, and canonical/source synchronization has not yet been implemented.

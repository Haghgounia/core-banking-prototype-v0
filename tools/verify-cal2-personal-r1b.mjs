#!/usr/bin/env node
// Compatibility entrypoint. R1B's ITEMS naming was superseded by R1C ENTRIES.
// Call the current contract verifier so old QA commands never falsely report success.
console.log('CAL2_PERSONAL_R1B_SUPERSEDED: running R1C ENTRIES verifier instead.');
await import('./verify-cal2-personal-r1c.mjs');

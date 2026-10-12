import {run} from './fee-r3a/installer-core.mjs';
run('install',{checkOnly:process.argv.includes('--check')});

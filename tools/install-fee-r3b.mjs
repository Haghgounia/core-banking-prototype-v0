import {run} from './fee-r3b/installer-core.mjs';
run('install',{checkOnly:process.argv.includes('--check')});

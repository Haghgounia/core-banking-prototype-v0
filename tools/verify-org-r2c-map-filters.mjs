import fs from 'node:fs';
import path from 'node:path';
import {createRequire} from 'node:module';
import {fileURLToPath} from 'node:url';
import assert from 'node:assert/strict';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const read=relative=>fs.readFileSync(path.join(root,relative),'utf8');
const source=read('frontend/src/app/features/organization/organization-branch-map.component.ts');
const helper='frontend/src/app/features/organization/organization-map-viewport.ts';
const service=read('backend/src/main/java/com/behsazan/corebanking/organization/location/map/OrganizationBranchMapService.java');
const controller=read('backend/src/main/java/com/behsazan/corebanking/organization/location/map/OrganizationBranchMapController.java');
let count=0;
function check(description,fn){try{fn();count++;console.log(`PASS | ${description}`);}catch(e){console.error(`FAIL | ${description}: ${e.message}`);process.exitCode=1;}}
check('01 Map camera follows successful filtered results',()=>{
  assert.ok(source.includes('this.fitToFilteredResults(data.points);'));
  assert.ok(source.includes('this.followResults=true;'));
  assert.ok(source.includes('if(token!==this.fetchToken)return;'));
});
check('02 Manual navigation does not fight camera auto-fit',()=>{
  assert.ok(source.includes('this.followResults=false;'));
  assert.ok(source.includes('if(this.followResults && this.result()?.points.length)'));
});
check('03 Empty filter results cannot show stale pins',()=>{
  assert.ok(source.includes('this.result.set(data)'));
  assert.ok(source.includes('this.mapStatus.set(data.totalUnits'));
  assert.ok(source.includes('this.center={latitude:32.5,longitude:53.5};this.zoom=5;'));
});
check('04 Unit type choices come from actual Oracle reference',()=>{
  assert.ok(source.includes('this.geo(\'unitType\',null,null)'));
  assert.ok(source.includes('@for (item of unitTypes(); track item.code)'));
  assert.ok(service.includes('FROM ORG.ORGANIZATION_UNIT_TYPES'));
  assert.ok(service.includes('TYPE_CODE AS CODE'));
  assert.ok(service.includes('"unitType"'));
  assert.ok(controller.includes('service.options(level,provinceId,countyId)'));
  assert.ok(!source.includes('<option value="HEAD_OFFICE_UNIT">'));
});
check('05 Cascading geography is reactive and race-safe',()=>{
  assert.ok(source.includes('this.provinceId.set(id);this.countyId.set(null);this.cityId.set(null);this.ruralDistrictId.set(null)'));
  assert.ok(source.includes('this.countyId.set(id);this.cityId.set(null);this.ruralDistrictId.set(null)'));
  assert.ok(source.includes('if(token===this.geoToken)this.counties.set(data)'));
  assert.ok(source.includes('if(token===this.geoToken){this.cities.set(cities);this.ruralDistricts.set(rurals);}'));
  assert.ok(source.includes('this.countiesLoading.set(false)'));
  assert.ok(source.includes('this.localitiesLoading.set(false)'));
});
check('06 Province county city rural and type are sent as independent server filters',()=>{
  for(const key of ['provinceId','countyId','cityId','ruralDistrictId']){
    assert.ok(source.includes(`${key}:this.${key}()`));
    assert.ok(service.includes(`:${key}`));
  }
  assert.ok(source.includes("params=params.set('typeCode',this.unitType())"));
  assert.ok(service.includes('UNIT_TYPE_CODE=:typeCode'));
});
check('07 No Oracle mutations or inferred geography in filter patch',()=>{
  assert.ok(service.includes('@Transactional(readOnly=true)'));
  assert.ok(!/\b(?:INSERT\s+INTO|UPDATE\s+ORG\.|DELETE\s+FROM|MERGE\s+INTO|TRUNCATE\s+TABLE)\b/i.test(service));
  assert.ok(!/@(?:PostMapping|PutMapping|PatchMapping|DeleteMapping)/.test(controller));
});

const require=createRequire(import.meta.url);
const tsCandidates=[
  path.join(root,'frontend','node_modules','typescript','lib','typescript.js'),
  path.resolve(path.dirname(process.execPath),'../lib/node_modules/typescript/lib/typescript.js'),
  ...(process.env.APPDATA?[path.join(process.env.APPDATA,'npm','node_modules','typescript','lib','typescript.js')]:[])
];
const tsFile=tsCandidates.find(file=>fs.existsSync(file));
if(tsFile){
  try{
  // Parse and execute the actual TS viewport helper when TypeScript is available.
  const ts=require(tsFile);
  const compiled=ts.transpileModule(read(helper),{
    fileName:helper,reportDiagnostics:true,
    compilerOptions:{target:ts.ScriptTarget.ES2022,module:ts.ModuleKind.CommonJS,strict:true}
  });
  assert.ok(!compiled.diagnostics?.length,'TypeScript helper must parse');
  const module={exports:{}};
  new Function('module','exports',compiled.outputText)(module,module.exports);
  const {fitBranchMapViewport}=module.exports;
  function projected(point,zoom){
    const sin=Math.sin(point.latitude*Math.PI/180),scale=256*2**zoom;
    return{x:(point.longitude+180)/360*scale,y:(.5-Math.log((1+sin)/(1-sin))/(4*Math.PI))*scale};
  }
  function assertInside(points,cam,width,height){
    const center=projected(cam.center,cam.zoom);
    for(const p of points){const pt=projected(p,cam.zoom);
      assert.ok(Math.abs(pt.x-center.x)<=width/2-20,`x not visible at ${cam.zoom}`);
      assert.ok(Math.abs(pt.y-center.y)<=height/2-20,`y not visible at ${cam.zoom}`);
    }
  }
  check('08 Tehran results move camera away from Shahrekord to Tehran',()=>{
    const p=[{latitude:35.682,longitude:51.417},{latitude:35.733,longitude:51.288}];
    const camera=fitBranchMapViewport(p,850,560);
    assert.ok(camera&&camera.center.latitude>35.5&&camera.center.latitude<36);
    assert.ok(camera.center.longitude>51.2&&camera.center.longitude<51.6);
    assert.ok(camera.zoom>=10);
    assertInside(p,camera,850,560);
  });
  check('09 Province and country-wide points all fit the visible map',()=>{
    for(const points of [
      [{latitude:35.69,longitude:51.39},{latitude:35.9,longitude:50.9},{latitude:35.2,longitude:51.8}],
      [{latitude:35.7,longitude:51.4},{latitude:38.09,longitude:46.3},
       {latitude:27.2,longitude:56.28},{latitude:36.3,longitude:59.6}]
    ]){
      const camera=fitBranchMapViewport(points,850,560);
      assert.ok(camera&&camera.zoom>=4&&camera.zoom<=14);
      assertInside(points,camera,850,560);
    }
  });
  check('10 Single branch centers precisely, bounded zoom',()=>{
    const p={latitude:35.6892,longitude:51.389};
    const camera=fitBranchMapViewport([p],820,530);
    assert.ok(camera&&camera.zoom===14);
    assert.ok(Math.abs(camera.center.latitude-p.latitude)<1e-8);
    assert.ok(Math.abs(camera.center.longitude-p.longitude)<1e-8);
  });
  check('11 Out-of-country and invalid coordinates never create a location',()=>{
    assert.equal(fitBranchMapViewport([{latitude:0,longitude:0},{latitude:Infinity,longitude:51}],820,530),null);
    assert.equal(fitBranchMapViewport([],820,530),null);
  });
  check('12 Layouts remain usable on narrow viewports',()=>{
    const p=[{latitude:35.69,longitude:51.39},{latitude:35.77,longitude:51.65}];
    const cam=fitBranchMapViewport(p,320,350);
    assert.ok(cam);
    assertInside(p,cam,320,350);
  });
  }catch(e){console.error(`FAIL | TS camera behavior tests could not run: ${e.message}`);process.exitCode=1;}
}else{console.log('INFO | TypeScript not installed; runtime viewport checks 08-12 are skipped (static checks 01-07 remain).');}
if(!process.exitCode)console.log(`ORG_R2C_MAP_FILTERS_VERIFY_PASS=${count}`);

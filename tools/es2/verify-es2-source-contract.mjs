#!/usr/bin/env node
// Non-invasive static assertions for the source baseline. No broker/DB access.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..', '..');
const read = relative => fs.readFileSync(path.join(root, relative), 'utf8');
const base = 'backend/src/main/java/com/behsazan/corebanking/posting/streaming/';
const producer = read(base + 'EventStreamPublisher.java');
const consumer = read(base + 'EventStreamAuditConsumer.java');
const kafkaConfig = read(base + 'EventStreamKafkaConfiguration.java');
const store = read(base + 'OracleEventStreamStore.java');
const sourceConfig = read('backend/src/main/resources/application.yml');
const externalConfig = read('config/application.yml');
const getDefault = value => (value.match(/core-banking\.event-stream\.topic:([A-Za-z0-9._-]+)/) || [])[1];
const p = getDefault(producer);
const c = getDefault(consumer);
const checks = [
  ['Publisher/Consumer topic defaults match', !!p && p === c],
  ['Expected baseline topic', p === 'corebanking.pst.events.v1'],
  ['Publisher remains opt-in', /name\s*=\s*\{"enabled",\s*"producer-enabled"\}/.test(producer)],
  ['Audit consumer remains opt-in', /name\s*=\s*\{"enabled",\s*"consumer-enabled"\}/.test(consumer)],
  ['Audit consumer makes no posting writes', !/(INSERT\s+INTO|UPDATE\s+|DELETE\s+FROM)\s+(?:ACC|LGR|DPS2)\./i.test(consumer)],
  ['No REST financial streaming writer', !/@(?:PostMapping|PutMapping|PatchMapping|DeleteMapping)/.test([producer,consumer,store].join('\n'))],
  ['Producer requires Kafka ack/idempotence configuration', kafkaConfig.includes('ENABLE_IDEMPOTENCE_CONFIG') && kafkaConfig.includes('ACKS_CONFIG')],
  ['No enabled:true streaming flags in packaged application YML', !/(?:event-stream:\s*\n\s*enabled:\s*true)/.test(sourceConfig + '\n' + externalConfig)],
  ['No automatic Kafka topic provisioning', !/NewTopic|KafkaAdmin|AdminClient\.createTopics/.test(kafkaConfig)],
];
let bad = 0;
for (const [label, ok] of checks) {
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${label}`);
  if (!ok) bad++;
}
console.log(`ES2_SOURCE_CONTRACT_PASS=${checks.length-bad} FAIL=${bad}`);
if (bad) process.exitCode = 1;

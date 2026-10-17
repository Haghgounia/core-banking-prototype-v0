#!/usr/bin/env node
// R1-D4/ES1 immutable transport contract; source-only and no Oracle/Kafka access.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const base = 'backend/src/main/java/com/behsazan/corebanking/posting/streaming';
const read = p => fs.readFileSync(path.join(root, p), 'utf8');
const env = read(base+'/EventStreamEnvelope.java');
const codec = read(base+'/EventStreamCodec.java');
const policy = read(base+'/EventStreamPolicy.java');
const store = read(base+'/OracleEventStreamStore.java');
const producer = read(base+'/EventStreamPublisher.java');
const consumer = read(base+'/EventStreamAuditConsumer.java');
const config = read(base+'/EventStreamKafkaConfiguration.java');
const schema = read(base+'/EventStreamSchemaGuard.java');
const pom = read('backend/pom.xml');
const t1 = read('backend/src/test/java/com/behsazan/corebanking/posting/streaming/EventStreamPolicyTest.java');
const t2 = read('backend/src/test/java/com/behsazan/corebanking/posting/streaming/EventStreamDeliveryTest.java');
const checks = [
  ['Spring Boot 4 Kafka starter is present', /<artifactId>spring-boot-starter-kafka<\/artifactId>/.test(pom)],
  ['versioned envelope', /int schemaVersion/.test(env) && /schemaVersion != 1/.test(env)],
  ['SHA256 exact-byte validation', /MessageDigest\.isEqual/.test(env) && /SHA-256/.test(env)],
  ['base64 opaque payload', /payloadBase64/.test(env) && /Base64\.getDecoder/.test(env)],
  ['bounded payload size', /MAX_PAYLOAD_BYTES = 65536/.test(env)],
  ['invalid JSON rejected', /readTree/.test(codec) && /isObject\(\)/.test(codec)],
  ['event equivalence excludes changed payload', /payloadSha256\(\)\.equals/.test(policy)],
  ['existing PST outbox is used', store.includes('INSERT INTO PST.POSTING_OUTBOX_EVENTS')],
  ['business transaction required', /Propagation\.MANDATORY/.test(store) && /isActualTransactionActive/.test(store)],
  ['existing PST inbox deduplicates', store.includes('INSERT INTO PST.POSTING_INBOX_RECEIPTS')],
  ['no new financial domain writer', !/\b(?:INSERT\s+INTO|UPDATE|DELETE\s+FROM)\s+(?:ACC|LGR|DPS2)\./i.test([store,producer,consumer].join('\n'))],
  ['conditional feature opt-in', [store,config].every(x=>x.includes('ConditionalOnProperty'))],
  ['producer has independent opt-in', /"producer-enabled"/.test(producer)],
  ['consumer has independent opt-in', /"consumer-enabled"/.test(consumer)],
  ['lease obtained by conditional Oracle UPDATE', /LEASE_TOKEN=\?/.test(store) && /IN_FLIGHT/.test(store)],
  ['expired lease recovery', /LEASE_UNTIL <= SYSTIMESTAMP/.test(store)],
  ['per-aggregate ordering blocks unresolved earlier events', store.split('PREVIOUS.IDENTIFIER<O.IDENTIFIER').length === 3],
  ['persistence after producer broker ack', producer.indexOf('.get(ackTimeoutSeconds') >= 0 && producer.indexOf('store.published(') > producer.indexOf('.get(ackTimeoutSeconds')],
  ['failed publish is retried and quarantined', store.includes('QUARANTINED') && store.includes('RETRY')],
  ['no automatic consumer offset commit', config.includes('ENABLE_AUTO_COMMIT_CONFIG, false')],
  ['producer safety checks enforce ACK and idempotence', config.includes('ENABLE_IDEMPOTENCE_CONFIG') && config.includes('ACKS_CONFIG')],
  ['oracle schema activation gate fails closed', schema.includes('afterSingletonsInstantiated') && schema.includes('STREAM_PAYLOAD') && schema.includes('requireUnique') && schema.includes('throw new IllegalStateException')],
  ['consumer ack after durable receipt', consumer.indexOf('store.receipt(') >= 0 && consumer.indexOf('acknowledgment.acknowledge()') > consumer.indexOf('store.receipt(')],
  ['poison events are not skipped automatically', config.includes('FixedBackOff.UNLIMITED_ATTEMPTS')],
  ['no deployed REST write endpoint', !/\@(?:PostMapping|PutMapping|PatchMapping|DeleteMapping)/.test([store,producer,consumer,config].join('\n'))],
  ['broker ACK mock regression retained', t2.includes('brokerAckMustPrecedePublishedFlag')],
  ['broker failure mock regression retained', t2.includes('brokerFailureNeverMarksPublished')],
  ['consumer order mock regression retained', t2.includes('auditConsumerPersistsBeforeManualAck')],
  ['payload tamper regression retained', t1.includes('payloadTamperIsRejected')],
  ['no financial activation change in existing code', !fs.existsSync(path.join(root,'backend/src/main/java/com/behsazan/corebanking/posting/streaming/FinancialPostingConsumer.java'))],
];
let failures = 0;
for(const [name,ok] of checks){ console.log(`${ok?'PASS':'FAIL'} | ${name}`); if(!ok) failures++; }
console.log(`R1D4_ES1_STATIC_PASS=${checks.length-failures} FAIL=${failures}`);
if(failures)process.exit(1);

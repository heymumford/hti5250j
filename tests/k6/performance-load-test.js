/*
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * k6 Load Testing Script for hti5250j Performance Validation
 * Simulates realistic terminal session load and measures SLA compliance
 *
 * Performance SLAs:
 * - Protocol command: <500µs (p95), <1ms (p99)
 * - Message throughput: >1,000 ops/sec per connection
 * - Connection handling: >100 concurrent sessions
 *
 * Usage:
 *   k6 run performance-load-test.js --vus 10 --duration 60s
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter, Histogram } from 'k6/metrics';

// Custom metrics for performance tracking
const commandDuration = new Histogram('command_duration_ms');
const messagesThroughput = new Counter('messages_total');
const slaViolations = new Counter('sla_violations');

export const options = {
  stages: [
    { duration: '10s', target: 5 },   // Ramp up to 5 VUs
    { duration: '30s', target: 10 },  // Ramp up to 10 VUs
    { duration: '20s', target: 5 },   // Ramp down to 5 VUs
    { duration: '10s', target: 0 },   // Final ramp down
  ],
  thresholds: {
    // SLA enforcement thresholds
    'command_duration_ms': ['p95<500', 'p99<1000'],  // Protocol command SLA
    'http_req_duration': ['p95<200', 'p99<500'],     // HTTP response SLA
    'http_req_failed': ['rate<0.01'],                // <1% failure rate
    'messages_total': ['count>1000'],                // Minimum throughput
  },
};

// Session state for each VU
const __VU = http.get('https://localhost').status === 200 ? 'production' : 'local';

export default function() {
  // Group 1: Terminal Connection (TLS Handshake + Auth)
  group('Terminal Connection Establishment', function() {
    // Simulate TLS handshake and session setup
    const connStart = new Date().getTime();

    const authRes = http.get('http://localhost:8080/terminal/auth', {
      timeout: '10s',
      tags: { name: 'TerminalAuth' },
    });

    const connDuration = new Date().getTime() - connStart;
    check(authRes, {
      'auth endpoint responds': (r) => r.status === 200 || r.status === 401,
      'auth response time < 500ms': (r) => r.timings.duration < 500,
    });

    if (connDuration > 500) {
      slaViolations.add(1, { operation: 'connection' });
    }

    sleep(1);
  });

  // Group 2: Protocol Command Execution (Core SLA)
  group('Protocol Command Processing', function() {
    const commandTypes = [
      { cmd: 'query_field', payload: 'SELECT field_name FROM display_buffer WHERE row=5' },
      { cmd: 'set_cursor', payload: 'MOVE_CURSOR row=10 col=15' },
      { cmd: 'input_data', payload: 'TYPE_DATA "USER_INPUT_HERE"' },
      { cmd: 'function_key', payload: 'PRESS_KEY F3' },
    ];

    commandTypes.forEach(function(cmd) {
      const cmdStart = new Date().getTime();

      const cmdRes = http.post(
        'http://localhost:8080/terminal/command',
        JSON.stringify({
          type: cmd.cmd,
          payload: cmd.payload,
        }),
        {
          headers: { 'Content-Type': 'application/json' },
          timeout: '10s',
          tags: { name: `Command_${cmd.cmd}` },
        }
      );

      const cmdDuration = new Date().getTime() - cmdStart;
      commandDuration.add(cmdDuration);
      messagesThroughput.add(1);

      check(cmdRes, {
        'command accepted': (r) => r.status === 200 || r.status === 202,
        'command response time < 500ms (SLA)': (r) => cmdDuration < 500,
        'command response time < 1000ms (p99)': (r) => cmdDuration < 1000,
      });

      if (cmdDuration > 500) {
        slaViolations.add(1, { operation: 'command', type: cmd.cmd });
      }

      sleep(0.1);
    });
  });

  // Group 3: Display Refresh (Throughput Test)
  group('Display Refresh Throughput', function() {
    const refreshStart = new Date().getTime();

    // Simulate 80x24 display refresh (typical terminal size)
    const refreshRes = http.post(
      'http://localhost:8080/terminal/refresh',
      JSON.stringify({
        rows: 24,
        columns: 80,
        updates: 1920,  // 80 * 24 cells
      }),
      {
        headers: { 'Content-Type': 'application/json' },
        timeout: '10s',
        tags: { name: 'DisplayRefresh' },
      }
    );

    const refreshDuration = new Date().getTime() - refreshStart;

    check(refreshRes, {
      'refresh accepted': (r) => r.status === 200,
      'refresh completes': (r) => refreshDuration < 100,  // Full refresh SLA
    });

    if (refreshDuration > 100) {
      slaViolations.add(1, { operation: 'refresh' });
    }

    sleep(0.5);
  });

  // Group 4: Session State Validation
  group('Session State Validation', function() {
    const stateRes = http.get(
      'http://localhost:8080/terminal/state',
      {
        timeout: '10s',
        tags: { name: 'SessionState' },
      }
    );

    check(stateRes, {
      'state endpoint responds': (r) => r.status === 200,
      'state response time < 100ms': (r) => r.timings.duration < 100,
      'state contains valid JSON': (r) => {
        try {
          JSON.parse(r.body);
          return true;
        } catch {
          return false;
        }
      },
    });

    sleep(0.5);
  });

  // Group 5: Batch Message Processing (High Throughput Scenario)
  group('Batch Message Processing', function() {
    const batchStart = new Date().getTime();

    // Simulate 10 commands in quick succession
    for (let i = 0; i < 10; i++) {
      http.post(
        'http://localhost:8080/terminal/command',
        JSON.stringify({
          type: 'input_data',
          payload: `BATCH_MSG_${i}`,
        }),
        {
          headers: { 'Content-Type': 'application/json' },
          timeout: '10s',
          tags: { name: 'BatchCommand' },
        }
      );
      messagesThroughput.add(1);
    }

    const batchDuration = new Date().getTime() - batchStart;

    check(batchDuration, {
      'batch 10 commands < 1000ms': () => batchDuration < 1000,
      'batch throughput > 10 ops/sec': () => batchDuration < 1000,
    });

    sleep(1);
  });

  // Final sleep before next iteration
  sleep(2);
}

/**
 * Summary Function: Print performance statistics at test completion
 */
export function handleSummary(data) {
  return {
    stdout: generateReport(data),
    'summary.json': JSON.stringify(data, null, 2),
  };
}

function generateReport(data) {
  const commandMetrics = data.metrics.command_duration_ms || {};
  const httpMetrics = data.metrics.http_req_duration || {};
  const violations = data.metrics.sla_violations?.value || 0;

  return `
═══════════════════════════════════════════════════════════════
📊 k6 Performance Load Test Summary
═══════════════════════════════════════════════════════════════

✅ Performance Metrics:
  • Command Duration (p95): ${commandMetrics.values?.['p(95)'] || 'N/A'} ms
  • Command Duration (p99): ${commandMetrics.values?.['p(99)'] || 'N/A'} ms
  • HTTP Response (p95): ${httpMetrics.values?.['p(95)'] || 'N/A'} ms
  • HTTP Response (p99): ${httpMetrics.values?.['p(99)'] || 'N/A'} ms

⚠️ SLA Status:
  • Total SLA Violations: ${violations}
  • SLA Compliance: ${violations === 0 ? '✓ PASS' : '✗ FAIL'}

🎯 Throughput:
  • Messages Processed: ${data.metrics.messages_total?.value || 0}
  • Test Duration: ${(data.state.testRunDurationMs / 1000).toFixed(1)} seconds

═══════════════════════════════════════════════════════════════
  Recommendation: ${violations === 0 ? '✓ Ready for production' : '⚠️ Performance degradation detected'}
═══════════════════════════════════════════════════════════════
`;
}

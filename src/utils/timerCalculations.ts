import { StaminaCalc, IdleCalc } from '../types';

export function abyssMaxFor(rank: number): number {
  const r = Math.max(1, Math.min(200, Math.floor(rank) || 1));
  return 240 + (r - 1) * 5;
}

export function remainingAfter40(current: number): number {
  return Math.max(0, current) % 40;
}

export function calculateStamina(
  current: number,
  max: number,
  intervalMin: number,
  start: number,
  now: number = Date.now()
): StaminaCalc {
  const intervalMs = Math.max(1, intervalMin) * 60_000;
  if (current >= max) {
    return { current: max, remainMs: 0, isFull: true, fullAt: start };
  }
  const elapsed = Math.max(0, now - start);
  const recovered = Math.floor(elapsed / intervalMs);
  const cur = Math.min(max, current + recovered);
  if (cur >= max) {
    const fullAt = start + Math.max(0, max - current) * intervalMs;
    return { current: max, remainMs: 0, isFull: true, fullAt: Math.min(now, fullAt) };
  }
  const nextIn = intervalMs - (elapsed % intervalMs);
  const need = max - cur;
  const remainMs = (need - 1) * intervalMs + nextIn;
  const fullAt = now + remainMs;
  return { current: cur, remainMs, isFull: false, fullAt };
}

export function calculateIdle(
  durationMin: number,
  start: number,
  now: number = Date.now()
): IdleCalc {
  const durMs = Math.max(1, durationMin) * 60_000;
  const elapsed = Math.max(0, now - start);
  const remainMs = Math.max(0, durMs - elapsed);
  return {
    remainMs,
    isFull: remainMs <= 0,
    fullAt: start + durMs,
  };
}

export function preserveCycle(
  intervalMin: number,
  start: number,
  now: number = Date.now()
): number {
  const intervalMs = Math.max(1, intervalMin) * 60_000;
  const phase = (((now - start) % intervalMs) + intervalMs) % intervalMs;
  return now - phase;
}

export function formatHM(timestamp: number): string {
  const d = new Date(timestamp);
  const h = String(d.getHours()).padStart(2, '0');
  const m = String(d.getMinutes()).padStart(2, '0');
  return `${h}:${m}`;
}

export function formatCountdown(remainMs: number): string {
  const safeMs = Math.max(0, remainMs);
  const totalMin = Math.floor((safeMs + 59_999) / 60_000);
  const h = Math.floor(totalMin / 60);
  const m = totalMin % 60;
  return `${h}:${String(m).padStart(2, '0')}`;
}

export function isNearFull(remainMs: number, isFull: boolean): boolean {
  return isFull || (remainMs > 0 && remainMs < 7_200_000); // under 2 hours
}

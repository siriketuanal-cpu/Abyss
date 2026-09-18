import { TimerEntry, GroupItem, TimerCardItem, AbyssItem } from '../types';
import { abyssMaxFor } from './timerCalculations';

const KEY_ITEMS = 'freetimer_items_json';
const KEY_ITEMS_V1 = 'freetimer:v1';

export function loadItems(): TimerEntry[] {
  try {
    const raw = localStorage.getItem(KEY_ITEMS) || localStorage.getItem(KEY_ITEMS_V1);
    if (!raw) return [];

    const parsed = JSON.parse(raw);
    const rawItems: TimerEntry[] = Array.isArray(parsed?.items)
      ? parsed.items
      : Array.isArray(parsed)
      ? parsed
      : [];

    // Normalize and validate items
    return rawItems
      .filter((it): it is TimerEntry => Boolean(it && it.id && it.type))
      .map((it) => {
        if (it.type === 'abyss') {
          const rank = Math.max(1, Math.min(200, Number(it.rank) || 1));
          const max = abyssMaxFor(rank);
          const current = Math.min(max, Math.max(0, Number(it.current) || 0));
          return {
            ...it,
            rank,
            max,
            current,
            intervalMin: Math.max(1, Number(it.intervalMin) || 3),
            start: Number(it.start) || Date.now(),
          } as AbyssItem;
        }
        if (it.type === 'group') {
          const group = it as GroupItem;
          const children = (Array.isArray(group.children) ? group.children : []).map((c) => {
            if (c.type === 'abyss') {
              const rank = Math.max(1, Math.min(200, Number(c.rank) || 1));
              const max = abyssMaxFor(rank);
              const current = Math.min(max, Math.max(0, Number(c.current) || 0));
              return {
                ...c,
                rank,
                max,
                current,
                intervalMin: Math.max(1, Number(c.intervalMin) || 3),
                start: Number(c.start) || Date.now(),
              } as AbyssItem;
            }
            return c;
          });
          return {
            ...group,
            children,
          };
        }
        return it;
      });
  } catch (err) {
    console.error('Failed to load items from storage:', err);
    return [];
  }
}

export function saveItems(items: TimerEntry[]): void {
  try {
    const payload = JSON.stringify({ items });
    localStorage.setItem(KEY_ITEMS, payload);
    localStorage.setItem(KEY_ITEMS_V1, payload);
  } catch (err) {
    console.error('Failed to save items to storage:', err);
  }
}

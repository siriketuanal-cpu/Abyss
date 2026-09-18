export type ItemType = 'stam' | 'abyss' | 'idle' | 'group' | 'header' | 'rule';

export interface StamItem {
  id: string;
  type: 'stam';
  name: string;
  current: number;
  max: number;
  intervalMin: number;
  start: number;
}

export interface AbyssItem {
  id: string;
  type: 'abyss';
  name: string;
  rank: number;
  current: number;
  max: number;
  intervalMin: number;
  start: number;
}

export interface IdleItem {
  id: string;
  type: 'idle';
  name: string;
  durationMin: number;
  state: 'running' | 'claim';
  start: number;
}

export type TimerCardItem = StamItem | AbyssItem | IdleItem;

export interface GroupItem {
  id: string;
  type: 'group';
  name: string;
  children: TimerCardItem[];
}

export interface HeaderItem {
  id: string;
  type: 'header';
  name: string;
  color: string;
}

export interface RuleItem {
  id: string;
  type: 'rule';
  color: string;
}

export type TimerEntry = GroupItem | HeaderItem | RuleItem | TimerCardItem;

export interface StaminaCalc {
  current: number;
  remainMs: number;
  isFull: boolean;
  fullAt: number;
}

export interface IdleCalc {
  remainMs: number;
  isFull: boolean;
  fullAt: number;
}

export type DialogState =
  | { type: 'add_panel'; parentGroupId?: string | null }
  | { type: 'setup_timer'; parentGroupId?: string | null; timerType: 'stam' | 'abyss' | 'idle' }
  | { type: 'setup_header'; initialColor?: string }
  | { type: 'setup_rule'; initialColor?: string }
  | { type: 'edit_name'; id: string; title: string; currentName: string }
  | {
      type: 'edit_number';
      id: string;
      title: string;
      currentValue: number;
      min: number;
      max: number;
      onConfirm: (val: number) => void;
    }
  | { type: 'edit_rank'; id: string; currentRank: number }
  | { type: 'edit_header_color'; id: string; currentColor: string }
  | { type: 'action_menu'; item: TimerEntry; parentGroupId?: string | null };

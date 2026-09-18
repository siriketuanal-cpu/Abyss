import React, { useState, useEffect, useCallback, useId } from 'react';
import {
  AbyssItem,
  DialogState,
  GroupItem,
  HeaderItem,
  IdleItem,
  RuleItem,
  StamItem,
  TimerCardItem,
  TimerEntry,
} from './types';
import { loadItems, saveItems } from './utils/storage';
import {
  abyssMaxFor,
  calculateStamina,
  preserveCycle,
  remainingAfter40,
} from './utils/timerCalculations';
import { TimerHeader } from './components/TimerHeader';
import { GroupCard } from './components/GroupCard';
import { HeaderCard } from './components/HeaderCard';
import { RuleCard } from './components/RuleCard';
import { StamCard } from './components/StamCard';
import { AbyssCard } from './components/AbyssCard';
import { IdleCard } from './components/IdleCard';
import {
  ActionMenuDialog,
  AddPanelDialog,
  EditNameDialog,
  EditNumberDialog,
  EditRankDialog,
  HeaderColorDialog,
  RuleColorDialog,
  SetupTimerDialog,
} from './components/Dialogs';

function generateId(): string {
  return 'id_' + Date.now().toString(36) + '_' + Math.random().toString(36).slice(2, 7);
}

export const App: React.FC = () => {
  const [items, setItems] = useState<TimerEntry[]>(() => loadItems());
  const [now, setNow] = useState<number>(() => Date.now());
  const [pending40Id, setPending40Id] = useState<string | null>(null);
  const [dialogState, setDialogState] = useState<DialogState | null>(null);

  // 1-second interval clock
  useEffect(() => {
    const timer = setInterval(() => {
      setNow(Date.now());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // Save to storage on item changes
  useEffect(() => {
    saveItems(items);
  }, [items]);

  // Cancel pending 40 or claim states
  const cancelPendingActions = useCallback(() => {
    setPending40Id(null);
    setItems((prev) => {
      let changed = false;
      const updated = prev.map((entry) => {
        if (entry.type === 'group') {
          const group = entry as GroupItem;
          let groupChanged = false;
          const newChildren = group.children.map((child) => {
            if (child.type === 'idle' && child.state === 'claim') {
              groupChanged = true;
              return { ...child, state: 'running' as const };
            }
            return child;
          });
          if (groupChanged) {
            changed = true;
            return { ...group, children: newChildren };
          }
          return entry;
        }
        if (entry.type === 'idle' && (entry as IdleItem).state === 'claim') {
          changed = true;
          return { ...(entry as IdleItem), state: 'running' as const };
        }
        return entry;
      });
      return changed ? updated : prev;
    });
  }, []);

  // Dialog management
  const dismissDialog = useCallback(() => {
    setDialogState(null);
  }, []);

  // Item additions
  const addGroup = useCallback((name = '') => {
    const newGroup: GroupItem = {
      id: generateId(),
      type: 'group',
      name,
      children: [],
    };
    setItems((prev) => [...prev, newGroup]);
    dismissDialog();
  }, [dismissDialog]);

  const addHeader = useCallback((color = '#9B8BFF') => {
    const newHeader: HeaderItem = {
      id: generateId(),
      type: 'header',
      name: '',
      color,
    };
    setItems((prev) => [...prev, newHeader]);
    dismissDialog();
  }, [dismissDialog]);

  const addRule = useCallback((color = '#52617A') => {
    const newRule: RuleItem = {
      id: generateId(),
      type: 'rule',
      color,
    };
    setItems((prev) => [...prev, newRule]);
    dismissDialog();
  }, [dismissDialog]);

  const insertChildTimer = useCallback((parentGroupId: string | null | undefined, child: TimerCardItem) => {
    setItems((prev) => {
      if (parentGroupId) {
        return prev.map((entry) => {
          if (entry.type === 'group' && entry.id === parentGroupId) {
            const group = entry as GroupItem;
            if (group.children.length < 2) {
              return { ...group, children: [...group.children, child] };
            }
          }
          return entry;
        });
      } else {
        const newGroup: GroupItem = {
          id: generateId(),
          type: 'group',
          name: '',
          children: [child],
        };
        return [...prev, newGroup];
      }
    });
    dismissDialog();
  }, [dismissDialog]);

  const addStam = useCallback((parentGroupId: string | null | undefined, intervalMin: number) => {
    const child: StamItem = {
      id: generateId(),
      type: 'stam',
      name: '',
      current: 0,
      max: 100,
      intervalMin,
      start: Date.now(),
    };
    insertChildTimer(parentGroupId, child);
  }, [insertChildTimer]);

  const addAbyss = useCallback((parentGroupId: string | null | undefined, intervalMin: number) => {
    const rank = 1;
    const child: AbyssItem = {
      id: generateId(),
      type: 'abyss',
      name: '',
      rank,
      current: 0,
      max: abyssMaxFor(rank),
      intervalMin,
      start: Date.now(),
    };
    insertChildTimer(parentGroupId, child);
  }, [insertChildTimer]);

  const addIdle = useCallback((parentGroupId: string | null | undefined, durationMin: number) => {
    const child: IdleItem = {
      id: generateId(),
      type: 'idle',
      name: '',
      durationMin,
      state: 'running',
      start: Date.now(),
    };
    insertChildTimer(parentGroupId, child);
  }, [insertChildTimer]);

  // Item deletions
  const deleteItem = useCallback((id: string, parentGroupId?: string | null) => {
    setItems((prev) => {
      if (parentGroupId) {
        return prev.map((entry) => {
          if (entry.type === 'group' && entry.id === parentGroupId) {
            const group = entry as GroupItem;
            return {
              ...group,
              children: group.children.filter((c) => c.id !== id),
            };
          }
          return entry;
        });
      }
      return prev.filter((entry) => entry.id !== id);
    });
    setPending40Id((current) => (current === id ? null : current));
    dismissDialog();
  }, [dismissDialog]);

  // Updates
  const updateName = useCallback((id: string, newName: string) => {
    setItems((prev) =>
      prev.map((entry) => {
        if (entry.id === id) {
          return { ...entry, name: newName };
        }
        if (entry.type === 'group') {
          const group = entry as GroupItem;
          return {
            ...group,
            children: group.children.map((c) =>
              c.id === id ? { ...c, name: newName } : c
            ),
          };
        }
        return entry;
      })
    );
    dismissDialog();
  }, [dismissDialog]);

  const updateHeaderColor = useCallback((id: string, newColor: string) => {
    setItems((prev) =>
      prev.map((entry) =>
        entry.id === id && entry.type === 'header'
          ? { ...entry, color: newColor }
          : entry
      )
    );
    dismissDialog();
  }, [dismissDialog]);

  const updateCardItem = useCallback(
    (id: string, transform: (card: TimerCardItem) => TimerCardItem) => {
      setItems((prev) =>
        prev.map((entry) => {
          if (entry.id === id && entry.type !== 'group' && entry.type !== 'header' && entry.type !== 'rule') {
            return transform(entry as TimerCardItem);
          }
          if (entry.type === 'group') {
            const group = entry as GroupItem;
            return {
              ...group,
              children: group.children.map((c) =>
                c.id === id ? transform(c) : c
              ),
            };
          }
          return entry;
        })
      );
    },
    []
  );

  const updateStamCurrent = useCallback((id: string, newCurrent: number) => {
    const currentTime = Date.now();
    updateCardItem(id, (card) => {
      if (card.type === 'stam') {
        const clamped = Math.max(0, Math.min(card.max, newCurrent));
        const newStart = preserveCycle(card.intervalMin, card.start, currentTime);
        return { ...card, current: clamped, start: newStart };
      }
      return card;
    });
  }, [updateCardItem]);

  const updateStamMax = useCallback((id: string, newMax: number) => {
    const currentTime = Date.now();
    updateCardItem(id, (card) => {
      if (card.type === 'stam') {
        const validMax = Math.max(1, Math.min(999, newMax));
        const calc = calculateStamina(card.current, card.max, card.intervalMin, card.start, currentTime);
        const newCur = Math.min(calc.current, validMax);
        const newStart = preserveCycle(card.intervalMin, card.start, currentTime);
        return { ...card, max: validMax, current: newCur, start: newStart };
      }
      return card;
    });
  }, [updateCardItem]);

  const updateAbyssRank = useCallback((id: string, newRank: number) => {
    const currentTime = Date.now();
    const validRank = Math.max(1, Math.min(200, newRank));
    const newMax = abyssMaxFor(validRank);
    updateCardItem(id, (card) => {
      if (card.type === 'abyss') {
        const calc = calculateStamina(card.current, card.max, card.intervalMin, card.start, currentTime);
        const newCur = Math.min(calc.current, newMax);
        const newStart = preserveCycle(card.intervalMin, card.start, currentTime);
        return {
          ...card,
          rank: validRank,
          max: newMax,
          current: newCur,
          start: newStart,
        };
      }
      return card;
    });
    setPending40Id(null);
  }, [updateCardItem]);

  const updateAbyssCurrent = useCallback((id: string, newCurrent: number) => {
    const currentTime = Date.now();
    updateCardItem(id, (card) => {
      if (card.type === 'abyss') {
        const clamped = Math.max(0, Math.min(card.max, newCurrent));
        const newStart = preserveCycle(card.intervalMin, card.start, currentTime);
        return { ...card, current: clamped, start: newStart };
      }
      return card;
    });
    setPending40Id(null);
  }, [updateCardItem]);

  const onAbyssCardTap = useCallback((item: AbyssItem) => {
    const currentTime = Date.now();
    if (pending40Id === item.id) {
      // Confirm 40 consumption
      const calc = calculateStamina(item.current, item.max, item.intervalMin, item.start, currentTime);
      const newCur = remainingAfter40(calc.current);
      const newStart = preserveCycle(item.intervalMin, item.start, currentTime);
      updateCardItem(item.id, (c) =>
        c.type === 'abyss' ? { ...c, current: newCur, start: newStart } : c
      );
      setPending40Id(null);
    } else {
      setPending40Id(item.id);
    }
  }, [pending40Id, updateCardItem]);

  const onIdleCardTap = useCallback((item: IdleItem) => {
    const currentTime = Date.now();
    if (item.state === 'claim') {
      // Confirm claim
      updateCardItem(item.id, (c) =>
        c.type === 'idle' ? { ...c, state: 'running', start: currentTime } : c
      );
    } else {
      // Enter claim wait state
      updateCardItem(item.id, (c) =>
        c.type === 'idle' ? { ...c, state: 'claim' } : c
      );
    }
  }, [updateCardItem]);

  return (
    <div
      className="h-full w-full flex flex-col bg-[#0b0b14] text-[#eceef2] select-none"
      onClick={(e) => {
        // Only cancel when clicking on screen background outside of interactive elements
        if (e.target === e.currentTarget) {
          cancelPendingActions();
        }
      }}
    >
      <TimerHeader
        onRefresh={cancelPendingActions}
        onAddClick={() => setDialogState({ type: 'add_panel' })}
      />

      <main
        className="flex-1 min-h-0 overflow-x-hidden overflow-y-auto overscroll-none px-2.5 py-2"
        onClick={(e) => {
          if (e.target === e.currentTarget) {
            cancelPendingActions();
          }
        }}
      >
        <div className="max-w-4xl mx-auto w-full grid grid-cols-4 gap-1 sm:gap-1.5 pb-8 items-start">
          {items.length === 0 ? (
            <div className="col-span-4 flex items-center justify-center min-h-[50vh]">
              <span
                data-testid="empty_state_text"
                className="text-[14px] font-medium text-[#8a8ea3]"
              >
                ＋ でタイマーを追加
              </span>
            </div>
          ) : (
            items.map((entry) => {
              if (entry.type === 'group') {
                const group = entry as GroupItem;
                return (
                  <GroupCard
                    key={group.id}
                    item={group}
                    now={now}
                    pending40Id={pending40Id}
                    onLongClick={() =>
                      setDialogState({ type: 'action_menu', item: group })
                    }
                    onEditGroupName={() =>
                      setDialogState({
                        type: 'edit_name',
                        id: group.id,
                        title: 'アカウント名の編集',
                        currentName: group.name,
                      })
                    }
                    onAddChildTimer={() =>
                      setDialogState({
                        type: 'add_panel',
                        parentGroupId: group.id,
                      })
                    }
                    onCardClick={(child) => {
                      if (child.type === 'abyss') onAbyssCardTap(child);
                      if (child.type === 'idle') onIdleCardTap(child);
                    }}
                    onChildLongClick={(child) =>
                      setDialogState({
                        type: 'action_menu',
                        item: child,
                        parentGroupId: group.id,
                      })
                    }
                    onUpdateStamCurrent={updateStamCurrent}
                    onUpdateStamMax={updateStamMax}
                    onUpdateAbyssRank={updateAbyssRank}
                    onUpdateAbyssCurrent={updateAbyssCurrent}
                  />
                );
              }

              if (entry.type === 'header') {
                const header = entry as HeaderItem;
                return (
                  <div key={header.id} className="col-span-4">
                    <HeaderCard
                      item={header}
                      onEditName={() =>
                        setDialogState({
                          type: 'edit_name',
                          id: header.id,
                          title: '見出しの編集',
                          currentName: header.name,
                        })
                      }
                      onLongClick={() =>
                        setDialogState({ type: 'action_menu', item: header })
                      }
                    />
                  </div>
                );
              }

              if (entry.type === 'rule') {
                const rule = entry as RuleItem;
                return (
                  <div key={rule.id} className="col-span-4">
                    <RuleCard
                      item={rule}
                      onLongClick={() =>
                        setDialogState({ type: 'action_menu', item: rule })
                      }
                    />
                  </div>
                );
              }

              if (entry.type === 'stam') {
                const stam = entry as StamItem;
                return (
                  <div key={stam.id} className="col-span-1">
                    <StamCard
                      item={stam}
                      now={now}
                      onLongClick={() =>
                        setDialogState({ type: 'action_menu', item: stam })
                      }
                      onUpdateCurrent={(val) => updateStamCurrent(stam.id, val)}
                      onUpdateMax={(val) => updateStamMax(stam.id, val)}
                    />
                  </div>
                );
              }

              if (entry.type === 'abyss') {
                const abyss = entry as AbyssItem;
                return (
                  <div key={abyss.id} className="col-span-1">
                    <AbyssCard
                      item={abyss}
                      now={now}
                      isPending40={pending40Id === abyss.id}
                      onCardClick={() => onAbyssCardTap(abyss)}
                      onLongClick={() =>
                        setDialogState({ type: 'action_menu', item: abyss })
                      }
                      onUpdateRank={(rank) => updateAbyssRank(abyss.id, rank)}
                      onUpdateCurrent={(val) => updateAbyssCurrent(abyss.id, val)}
                    />
                  </div>
                );
              }

              if (entry.type === 'idle') {
                const idle = entry as IdleItem;
                return (
                  <div key={idle.id} className="col-span-1">
                    <IdleCard
                      item={idle}
                      now={now}
                      onCardClick={() => onIdleCardTap(idle)}
                      onLongClick={() =>
                        setDialogState({ type: 'action_menu', item: idle })
                      }
                    />
                  </div>
                );
              }

              return null;
            })
          )}
        </div>
      </main>

      {/* Dialogs Routing */}
      {dialogState?.type === 'add_panel' && (
        <AddPanelDialog
          parentGroupId={dialogState.parentGroupId}
          onSelectGroup={() => addGroup()}
          onSelectHeader={() => setDialogState({ type: 'setup_header' })}
          onSelectRule={() => setDialogState({ type: 'setup_rule' })}
          onSelectStam={() =>
            setDialogState({
              type: 'setup_timer',
              parentGroupId: dialogState.parentGroupId,
              timerType: 'stam',
            })
          }
          onSelectAbyss={() =>
            setDialogState({
              type: 'setup_timer',
              parentGroupId: dialogState.parentGroupId,
              timerType: 'abyss',
            })
          }
          onSelectIdle={() =>
            setDialogState({
              type: 'setup_timer',
              parentGroupId: dialogState.parentGroupId,
              timerType: 'idle',
            })
          }
          onDismiss={dismissDialog}
        />
      )}

      {dialogState?.type === 'setup_timer' && (
        <SetupTimerDialog
          type={dialogState.timerType}
          onConfirmStam={(interval) => addStam(dialogState.parentGroupId, interval)}
          onConfirmAbyss={(interval) => addAbyss(dialogState.parentGroupId, interval)}
          onConfirmIdle={(duration) => addIdle(dialogState.parentGroupId, duration)}
          onDismiss={dismissDialog}
        />
      )}

      {dialogState?.type === 'setup_header' && (
        <HeaderColorDialog
          initialColor={dialogState.initialColor || '#9B8BFF'}
          onConfirm={(color) => addHeader(color)}
          onDismiss={dismissDialog}
        />
      )}

      {dialogState?.type === 'setup_rule' && (
        <RuleColorDialog
          initialColor={dialogState.initialColor || '#52617A'}
          onConfirm={(color) => addRule(color)}
          onDismiss={dismissDialog}
        />
      )}

      {dialogState?.type === 'edit_name' && (
        <EditNameDialog
          title={dialogState.title}
          currentName={dialogState.currentName}
          onConfirm={(name) => updateName(dialogState.id, name)}
          onDismiss={dismissDialog}
        />
      )}

      {dialogState?.type === 'edit_number' && (
        <EditNumberDialog
          title={dialogState.title}
          currentValue={dialogState.currentValue}
          min={dialogState.min}
          max={dialogState.max}
          onConfirm={(val) => {
            dialogState.onConfirm(val);
            dismissDialog();
          }}
          onDismiss={dismissDialog}
        />
      )}

      {dialogState?.type === 'edit_rank' && (
        <EditRankDialog
          currentRank={dialogState.currentRank}
          onConfirm={(rank) => {
            updateAbyssRank(dialogState.id, rank);
            dismissDialog();
          }}
          onDismiss={dismissDialog}
        />
      )}

      {dialogState?.type === 'edit_header_color' && (
        <HeaderColorDialog
          initialColor={dialogState.currentColor}
          title="文字色の変更"
          onConfirm={(color) => updateHeaderColor(dialogState.id, color)}
          onDismiss={dismissDialog}
        />
      )}

      {dialogState?.type === 'action_menu' && (
        <ActionMenuDialog
          item={dialogState.item}
          parentGroupId={dialogState.parentGroupId}
          onAddTimerToGroup={() => {
            const groupId = dialogState.item.id;
            dismissDialog();
            setDialogState({ type: 'add_panel', parentGroupId: groupId });
          }}
          onChangeHeaderColor={() => {
            if (dialogState.item.type === 'header') {
              const header = dialogState.item as HeaderItem;
              setDialogState({
                type: 'edit_header_color',
                id: header.id,
                currentColor: header.color,
              });
            }
          }}
          onEditName={() => {
            const curName =
              dialogState.item.type === 'group' || dialogState.item.type === 'header'
                ? dialogState.item.name
                : '';
            setDialogState({
              type: 'edit_name',
              id: dialogState.item.id,
              title: '名前を変更',
              currentName: curName,
            });
          }}
          onDelete={() => {
            deleteItem(dialogState.item.id, dialogState.parentGroupId);
          }}
          onDismiss={dismissDialog}
        />
      )}
    </div>
  );
};

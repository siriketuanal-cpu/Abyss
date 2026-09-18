import React from 'react';
import { AbyssItem, GroupItem, StamItem, TimerCardItem } from '../types';
import { StamCard } from './StamCard';
import { AbyssCard } from './AbyssCard';
import { IdleCard } from './IdleCard';
import { useLongPress } from '../utils/useLongPress';

interface GroupCardProps {
  item: GroupItem;
  now: number;
  pending40Id: string | null;
  onLongClick: () => void;
  onEditGroupName: () => void;
  onAddChildTimer: () => void;
  onCardClick: (child: TimerCardItem) => void;
  onChildLongClick: (child: TimerCardItem) => void;
  onUpdateStamCurrent: (id: string, val: number) => void;
  onUpdateStamMax: (id: string, val: number) => void;
  onUpdateAbyssRank: (id: string, rank: number) => void;
  onUpdateAbyssCurrent: (id: string, val: number) => void;
}

export const GroupCard: React.FC<GroupCardProps> = ({
  item,
  now,
  pending40Id,
  onLongClick,
  onEditGroupName,
  onAddChildTimer,
  onCardClick,
  onChildLongClick,
  onUpdateStamCurrent,
  onUpdateStamMax,
  onUpdateAbyssRank,
  onUpdateAbyssCurrent,
}) => {
  const headerHandlers = useLongPress({
    onClick: () => onEditGroupName(),
    onLongPress: () => onLongClick(),
  });

  const canAddMore = item.children.length < 2;

  return (
    <div
      data-testid={`group_${item.id}`}
      className="col-span-2 rounded-[8px] bg-[#171724]/80 border border-[#3e4250] p-1 sm:p-1.5 flex flex-col gap-1 select-none"
    >
      {/* Group header row */}
      <div
        {...headerHandlers}
        className="px-1 py-0.5 cursor-pointer hover:opacity-80 transition-opacity flex items-center justify-between"
      >
        <span className="text-[11px] font-bold text-[#8a8ea3] truncate max-w-full">
          {item.name || 'アカウント'}
        </span>
      </div>

      {/* Children grid: exactly 2 slots side by side */}
      <div className="grid grid-cols-2 gap-1">
        {item.children.map((child) => {
          if (child.type === 'stam') {
            const stam = child as StamItem;
            return (
              <StamCard
                key={stam.id}
                item={stam}
                now={now}
                onLongClick={() => onChildLongClick(stam)}
                onUpdateCurrent={(val) => onUpdateStamCurrent(stam.id, val)}
                onUpdateMax={(val) => onUpdateStamMax(stam.id, val)}
              />
            );
          }
          if (child.type === 'abyss') {
            const abyss = child as AbyssItem;
            return (
              <AbyssCard
                key={abyss.id}
                item={abyss}
                now={now}
                isPending40={pending40Id === abyss.id}
                onCardClick={() => onCardClick(abyss)}
                onLongClick={() => onChildLongClick(abyss)}
                onUpdateRank={(rank) => onUpdateAbyssRank(abyss.id, rank)}
                onUpdateCurrent={(val) => onUpdateAbyssCurrent(abyss.id, val)}
              />
            );
          }
          if (child.type === 'idle') {
            return (
              <IdleCard
                key={child.id}
                item={child}
                now={now}
                onCardClick={() => onCardClick(child)}
                onLongClick={() => onChildLongClick(child)}
              />
            );
          }
          return null;
        })}

        {/* If only 1 child exists in this 2-timer account frame, show an add slot */}
        {canAddMore && (
          <button
            type="button"
            data-testid={`btn_add_to_group_${item.id}`}
            onClick={onAddChildTimer}
            className="h-[58px] rounded-[10px] border border-dashed border-[#2f3342] bg-[#12121c]/50 hover:bg-[#181826] hover:border-[#9b8bff] text-[#6b7082] hover:text-[#eceef2] flex flex-col items-center justify-center transition-colors text-[10px] font-bold gap-0.5"
            title="タイマーを追加"
          >
            <span className="text-[14px] leading-none">＋</span>
            <span>タイマー</span>
          </button>
        )}
      </div>
    </div>
  );
};

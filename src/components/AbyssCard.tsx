import React, { useState } from 'react';
import { AbyssItem } from '../types';
import {
  calculateStamina,
  formatHM,
  isNearFull,
  remainingAfter40,
} from '../utils/timerCalculations';
import { useLongPress } from '../utils/useLongPress';
import { InlineNumberEditor } from './InlineNumberEditor';

interface AbyssCardProps {
  item: AbyssItem;
  now: number;
  isPending40: boolean;
  onCardClick: () => void;
  onLongClick: () => void;
  onUpdateRank: (newRank: number) => void;
  onUpdateCurrent: (newCurrent: number) => void;
}

export const AbyssCard: React.FC<AbyssCardProps> = ({
  item,
  now,
  isPending40,
  onCardClick,
  onLongClick,
  onUpdateRank,
  onUpdateCurrent,
}) => {
  const [isEditingNumber, setIsEditingNumber] = useState(false);
  const calc = calculateStamina(item.current, item.max, item.intervalMin, item.start, now);
  const isNear = isNearFull(calc.remainMs, calc.isFull);
  const displayCur = isPending40 ? remainingAfter40(calc.current) : calc.current;

  const cardHandlers = useLongPress({
    onClick: () => {
      // If not inline editing, toggle pending 40 consumption
      if (!isEditingNumber) {
        onCardClick();
      }
    },
    onLongPress: () => onLongClick(),
  });

  const borderStyle = isPending40
    ? { borderColor: '#5cd68a', borderWidth: '2px', borderStyle: 'dashed' as const }
    : calc.isFull
    ? { borderColor: '#ffab5c', borderWidth: '1px', borderStyle: 'solid' as const }
    : { borderColor: '#b48cff', borderWidth: '1px', borderStyle: 'solid' as const };

  return (
    <div
      {...cardHandlers}
      data-testid={`card_abyss_${item.id}`}
      style={borderStyle}
      className="relative h-[58px] rounded-[10px] bg-[#15151f] px-1.5 py-1 flex flex-col justify-between select-none cursor-pointer transition-colors hover:bg-[#181825]"
    >
      {/* Top row: Rank on left, Full time on right */}
      <div className="flex items-center justify-between">
        <InlineNumberEditor
          value={item.rank}
          prefix="Lv."
          min={1}
          max={200}
          textColor="#ffab5c"
          className="text-[10px] font-extrabold px-0.5"
          onConfirm={onUpdateRank}
          onLongClick={onLongClick}
          onEditingChange={setIsEditingNumber}
        />
        <span
          className="text-[10px] font-bold tabular-nums"
          style={{ color: calc.isFull ? '#ff6b6b' : '#9b8bff' }}
        >
          {formatHM(calc.fullAt)}
        </span>
      </div>

      {/* Bottom row: values */}
      <div className="flex items-center justify-center -mt-1 pb-0.5">
        <InlineNumberEditor
          value={displayCur}
          min={0}
          max={item.max}
          textColor={isNear ? '#ff6b6b' : '#eceef2'}
          className="text-[17px] font-bold px-1"
          onConfirm={onUpdateCurrent}
          onLongClick={onLongClick}
          disabled={isPending40}
          onEditingChange={setIsEditingNumber}
        />
        <span className="text-[14px] font-bold text-[#8a8ea3] mx-0.5 select-none">/</span>
        <span className="text-[17px] font-bold text-[#5aa9ff] tabular-nums px-1 select-none">
          {item.max}
        </span>
      </div>
    </div>
  );
};

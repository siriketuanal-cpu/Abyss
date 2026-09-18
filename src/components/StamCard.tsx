import React from 'react';
import { StamItem } from '../types';
import { calculateStamina, formatHM, isNearFull } from '../utils/timerCalculations';
import { useLongPress } from '../utils/useLongPress';
import { InlineNumberEditor } from './InlineNumberEditor';

interface StamCardProps {
  item: StamItem;
  now: number;
  onLongClick: () => void;
  onUpdateCurrent: (val: number) => void;
  onUpdateMax: (val: number) => void;
}

export const StamCard: React.FC<StamCardProps> = ({
  item,
  now,
  onLongClick,
  onUpdateCurrent,
  onUpdateMax,
}) => {
  const calc = calculateStamina(item.current, item.max, item.intervalMin, item.start, now);
  const isNear = isNearFull(calc.remainMs, calc.isFull);

  const cardHandlers = useLongPress({
    onClick: () => {
      // Tapping outer card area: safe tap / does nothing or closes
    },
    onLongPress: () => onLongClick(),
  });

  const borderColor = calc.isFull ? '#ffab5c' : '#5aa9ff';

  return (
    <div
      {...cardHandlers}
      data-testid={`card_stam_${item.id}`}
      style={{ borderColor }}
      className="relative h-[58px] rounded-[10px] bg-[#15151f] border px-1.5 py-1 flex flex-col justify-between select-none cursor-pointer transition-colors hover:bg-[#181825]"
    >
      {/* Top right: Full time */}
      <div className="flex justify-end">
        <span
          className="text-[10px] font-bold tabular-nums"
          style={{ color: calc.isFull ? '#ff6b6b' : '#9b8bff' }}
        >
          {formatHM(calc.fullAt)}
        </span>
      </div>

      {/* Center/bottom values: current / max */}
      <div className="flex items-center justify-center -mt-1 pb-0.5">
        <InlineNumberEditor
          value={calc.current}
          min={0}
          max={item.max}
          textColor={isNear ? '#ff6b6b' : '#eceef2'}
          className="text-[17px] font-bold px-1"
          onConfirm={onUpdateCurrent}
          onLongClick={onLongClick}
        />
        <span className="text-[14px] font-bold text-[#8a8ea3] mx-0.5 select-none">/</span>
        <InlineNumberEditor
          value={item.max}
          min={1}
          max={999}
          textColor="#5aa9ff"
          className="text-[17px] font-bold px-1"
          onConfirm={onUpdateMax}
          onLongClick={onLongClick}
        />
      </div>
    </div>
  );
};

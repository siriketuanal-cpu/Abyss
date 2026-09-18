import React from 'react';
import { IdleItem } from '../types';
import {
  calculateIdle,
  formatCountdown,
  formatHM,
  isNearFull,
} from '../utils/timerCalculations';
import { useLongPress } from '../utils/useLongPress';

interface IdleCardProps {
  item: IdleItem;
  now: number;
  onCardClick: () => void;
  onLongClick: () => void;
}

export const IdleCard: React.FC<IdleCardProps> = ({
  item,
  now,
  onCardClick,
  onLongClick,
}) => {
  const calc = calculateIdle(item.durationMin, item.start, now);
  const isClaim = item.state === 'claim';
  const isNear = isClaim || isNearFull(calc.remainMs, calc.isFull);

  const cardHandlers = useLongPress({
    onClick: () => onCardClick(),
    onLongPress: () => onLongClick(),
  });

  const borderStyle = isClaim
    ? { borderColor: '#5cd68a', borderWidth: '2px', borderStyle: 'dashed' as const }
    : calc.isFull
    ? { borderColor: '#ffab5c', borderWidth: '1px', borderStyle: 'solid' as const }
    : { borderColor: '#f0a85a', borderWidth: '1px', borderStyle: 'solid' as const };

  const centerText = isClaim
    ? '受取'
    : calc.isFull
    ? formatHM(calc.fullAt)
    : formatCountdown(calc.remainMs);

  return (
    <div
      {...cardHandlers}
      data-testid={`card_idle_${item.id}`}
      style={borderStyle}
      className="relative h-[58px] rounded-[10px] bg-[#15151f] px-2 py-1 flex flex-col justify-between select-none cursor-pointer transition-colors hover:bg-[#181825]"
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

      {/* Center/bottom text */}
      <div className="flex items-center justify-center -mt-1 pb-0.5">
        <span
          className={`font-bold tabular-nums text-center ${
            isClaim ? 'text-[17px]' : 'text-[18px]'
          }`}
          style={{ color: isNear ? '#ff6b6b' : '#eceef2' }}
        >
          {centerText}
        </span>
      </div>
    </div>
  );
};

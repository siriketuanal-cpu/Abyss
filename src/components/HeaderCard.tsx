import React from 'react';
import { HeaderItem } from '../types';
import { useLongPress } from '../utils/useLongPress';

interface HeaderCardProps {
  item: HeaderItem;
  onEditName: () => void;
  onLongClick: () => void;
}

export const HeaderCard: React.FC<HeaderCardProps> = ({
  item,
  onEditName,
  onLongClick,
}) => {
  const handlers = useLongPress({
    onClick: () => onEditName(),
    onLongPress: () => onLongClick(),
  });

  return (
    <div
      {...handlers}
      data-testid={`header_${item.id}`}
      className="w-full py-1 cursor-pointer select-none group"
    >
      <div className="flex flex-col gap-0.5">
        <span
          className="text-[14px] font-bold truncate group-hover:opacity-85 transition-opacity"
          style={{ color: item.color || '#9B8BFF' }}
        >
          {item.name || '見出し(ゲーム名など)'}
        </span>
        <div className="w-full h-[1px] bg-[#2a2a3a]" />
      </div>
    </div>
  );
};

import React from 'react';
import { RuleItem } from '../types';
import { useLongPress } from '../utils/useLongPress';

interface RuleCardProps {
  item: RuleItem;
  onLongClick: () => void;
}

export const RuleCard: React.FC<RuleCardProps> = ({ item, onLongClick }) => {
  const handlers = useLongPress({
    onClick: () => onLongClick(),
    onLongPress: () => onLongClick(),
  });

  return (
    <div
      {...handlers}
      data-testid={`rule_${item.id}`}
      className="w-full py-2 cursor-pointer select-none group"
    >
      <div
        className="w-full h-[2px] rounded-full group-hover:opacity-85 transition-opacity"
        style={{ backgroundColor: item.color || '#52617A' }}
      />
    </div>
  );
};

import React from 'react';
import { RefreshCw, Plus } from 'lucide-react';

interface TimerHeaderProps {
  onRefresh: () => void;
  onAddClick: () => void;
}

export const TimerHeader: React.FC<TimerHeaderProps> = ({ onRefresh, onAddClick }) => {
  return (
    <header className="flex items-center justify-between px-3 py-2 bg-[#0b0b14] border-b border-[#2a2a3a]/40 select-none z-10 shrink-0">
      <button
        type="button"
        onClick={onRefresh}
        title="アプリを更新"
        data-testid="btn_refresh"
        className="w-9 h-9 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#eceef2] flex items-center justify-center active:scale-95 transition-transform hover:bg-[#252536]"
      >
        <RefreshCw className="w-4 h-4" />
      </button>

      <h1 className="text-lg font-bold text-[#eceef2] tracking-wide">
        タイマー
      </h1>

      <button
        type="button"
        onClick={onAddClick}
        title="追加"
        data-testid="btn_add"
        className="w-9 h-9 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#eceef2] flex items-center justify-center active:scale-95 transition-transform hover:bg-[#252536]"
      >
        <Plus className="w-5 h-5" />
      </button>
    </header>
  );
};

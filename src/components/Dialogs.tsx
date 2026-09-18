import React, { useState, useEffect, useRef } from 'react';
import { DialogState, GroupItem, HeaderItem, TimerEntry } from '../types';
import { abyssMaxFor } from '../utils/timerCalculations';

export const HEADER_COLORS = [
  '#9B8BFF',
  '#B48CFF',
  '#6FC7FF',
  '#70D6B0',
  '#FFD166',
  '#FF9F68',
  '#FF7B9C',
  '#D7DBE7',
];

export const RULE_COLORS = [
  '#52617A',
  '#9B8BFF',
  '#5AA9FF',
  '#FFAB5C',
  '#5CD68A',
  '#FF6B6B',
];

interface DialogModalProps {
  children: React.ReactNode;
  onDismiss: () => void;
}

const DialogModal: React.FC<DialogModalProps> = ({ children, onDismiss }) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onDismiss();
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onDismiss]);

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-[2px] animate-in fade-in duration-150"
      onClick={(e) => {
        if (e.target === e.currentTarget) onDismiss();
      }}
    >
      <div
        className="w-full max-w-sm rounded-[14px] bg-[#15151f] border border-[#2a2a3a] p-5 shadow-2xl flex flex-col items-center select-none"
        onClick={(e) => e.stopPropagation()}
      >
        {children}
      </div>
    </div>
  );
};

// 1. Add Panel Dialog
export const AddPanelDialog: React.FC<{
  parentGroupId?: string | null;
  onSelectGroup: () => void;
  onSelectHeader: () => void;
  onSelectRule: () => void;
  onSelectStam: () => void;
  onSelectAbyss: () => void;
  onSelectIdle: () => void;
  onDismiss: () => void;
}> = ({
  parentGroupId,
  onSelectGroup,
  onSelectHeader,
  onSelectRule,
  onSelectStam,
  onSelectAbyss,
  onSelectIdle,
  onDismiss,
}) => {
  const isInsideGroup = Boolean(parentGroupId);

  return (
    <DialogModal onDismiss={onDismiss}>
      <h2 className="text-[16px] font-bold text-[#eceef2] mb-3.5">
        {isInsideGroup ? 'タイマーの種類を選択' : '追加する項目を選択'}
      </h2>

      <div className="w-full flex flex-col gap-2">
        {isInsideGroup ? (
          <>
            <button
              type="button"
              data-testid="btn_add_スタミナ"
              onClick={onSelectStam}
              className="w-full h-12 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#5aa9ff] text-[15px] font-bold hover:bg-[#252536] transition-colors"
            >
              スタミナ
            </button>
            <button
              type="button"
              data-testid="btn_add_Abyssスタミナ"
              onClick={onSelectAbyss}
              className="w-full h-12 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#b48cff] text-[15px] font-bold hover:bg-[#252536] transition-colors"
            >
              Abyssスタミナ
            </button>
            <button
              type="button"
              data-testid="btn_add_放置報酬"
              onClick={onSelectIdle}
              className="w-full h-12 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#ffab5c] text-[15px] font-bold hover:bg-[#252536] transition-colors"
            >
              放置報酬
            </button>
          </>
        ) : (
          <>
            <button
              type="button"
              data-testid="btn_add_アカウント枠"
              onClick={onSelectGroup}
              className="w-full h-12 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#9b8bff] text-[15px] font-bold hover:bg-[#252536] transition-colors"
            >
              アカウント枠
            </button>
            <button
              type="button"
              data-testid="btn_add_見出し"
              onClick={onSelectHeader}
              className="w-full h-12 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#9b8bff] text-[15px] font-bold hover:bg-[#252536] transition-colors"
            >
              見出し
            </button>
            <button
              type="button"
              data-testid="btn_add_仕切り線"
              onClick={onSelectRule}
              className="w-full h-12 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#8a8ea3] text-[15px] font-bold hover:bg-[#252536] transition-colors"
            >
              仕切り線
            </button>
          </>
        )}
      </div>

      <button
        type="button"
        onClick={onDismiss}
        className="w-full mt-3 py-2 text-[14px] text-[#8a8ea3] hover:text-[#eceef2] transition-colors"
      >
        閉じる
      </button>
    </DialogModal>
  );
};

// 2. Setup Timer Dialog
export const SetupTimerDialog: React.FC<{
  type: 'stam' | 'abyss' | 'idle';
  onConfirmStam: (interval: number) => void;
  onConfirmAbyss: (interval: number) => void;
  onConfirmIdle: (durationMin: number) => void;
  onDismiss: () => void;
}> = ({ type, onConfirmStam, onConfirmAbyss, onConfirmIdle, onDismiss }) => {
  const [intervalStr, setIntervalStr] = useState(type === 'abyss' ? '3' : '5');
  const [hoursStr, setHoursStr] = useState('12');
  const [minsStr, setMinsStr] = useState('0');
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
    inputRef.current?.select();
  }, []);

  const handleConfirm = () => {
    if (type === 'stam') {
      const val = Math.max(1, Math.min(999, parseInt(intervalStr, 10) || 5));
      onConfirmStam(val);
    } else if (type === 'abyss') {
      const val = Math.max(1, Math.min(999, parseInt(intervalStr, 10) || 3));
      onConfirmAbyss(val);
    } else {
      const h = Math.max(0, Math.min(999, parseInt(hoursStr, 10) || 12));
      const m = Math.max(0, Math.min(59, parseInt(minsStr, 10) || 0));
      const total = Math.max(1, h * 60 + m);
      onConfirmIdle(total);
    }
  };

  const title =
    type === 'abyss'
      ? 'Abyssスタミナ回復間隔'
      : type === 'stam'
      ? 'スタミナ回復間隔'
      : '満タンまでの時間';

  return (
    <DialogModal onDismiss={onDismiss}>
      <h2 className="text-[14px] font-medium text-[#8a8ea3] mb-3">{title}</h2>

      <div className="w-full flex items-center justify-center my-2">
        {type === 'idle' ? (
          <div className="flex items-center gap-2">
            <input
              ref={inputRef}
              type="number"
              min="0"
              max="999"
              value={hoursStr}
              onChange={(e) => setHoursStr(e.target.value.replace(/\D/g, '').slice(0, 3))}
              className="w-16 h-11 text-center text-[16px] font-bold rounded-[8px] bg-[#1b1b28] border border-[#2a2a3a] text-[#eceef2] focus:border-[#9b8bff] focus:outline-none tabular-nums"
            />
            <span className="text-[#8a8ea3] text-[14px] font-medium">時間</span>
            <input
              type="number"
              min="0"
              max="59"
              value={minsStr}
              onChange={(e) => setMinsStr(e.target.value.replace(/\D/g, '').slice(0, 2))}
              className="w-16 h-11 text-center text-[16px] font-bold rounded-[8px] bg-[#1b1b28] border border-[#2a2a3a] text-[#eceef2] focus:border-[#9b8bff] focus:outline-none tabular-nums"
            />
            <span className="text-[#8a8ea3] text-[14px] font-medium">分</span>
          </div>
        ) : (
          <div className="flex items-center gap-2">
            <input
              ref={inputRef}
              type="number"
              min="1"
              max="999"
              value={intervalStr}
              onChange={(e) => setIntervalStr(e.target.value.replace(/\D/g, '').slice(0, 3))}
              className="w-20 h-11 text-center text-[16px] font-bold rounded-[8px] bg-[#1b1b28] border border-[#2a2a3a] text-[#eceef2] focus:border-[#9b8bff] focus:outline-none tabular-nums"
            />
            <span className="text-[#8a8ea3] text-[14px] font-medium">分で1回復</span>
          </div>
        )}
      </div>

      <button
        type="button"
        onClick={handleConfirm}
        className="w-full h-11 mt-4 rounded-[10px] bg-[#9b8bff] text-[#100c26] text-[15px] font-bold hover:bg-[#8d7aff] transition-colors"
      >
        追加
      </button>

      <button
        type="button"
        onClick={onDismiss}
        className="w-full mt-2 py-2 text-[14px] text-[#8a8ea3] hover:text-[#eceef2] transition-colors"
      >
        キャンセル
      </button>
    </DialogModal>
  );
};

// 3. Header Color Dialog
export const HeaderColorDialog: React.FC<{
  initialColor: string;
  title?: string;
  onConfirm: (color: string) => void;
  onDismiss: () => void;
}> = ({ initialColor, title = 'ゲーム名ヘッダーの文字色', onConfirm, onDismiss }) => {
  const [selectedColor, setSelectedColor] = useState(initialColor);

  return (
    <DialogModal onDismiss={onDismiss}>
      <h2 className="text-[14px] font-medium text-[#8a8ea3] mb-4">{title}</h2>

      <div className="flex flex-col gap-3 my-1">
        <div className="flex justify-center gap-3">
          {HEADER_COLORS.slice(0, 4).map((hex) => (
            <button
              key={hex}
              type="button"
              onClick={() => setSelectedColor(hex)}
              style={{ backgroundColor: hex }}
              className={`w-9 h-9 rounded-full transition-transform active:scale-95 ${
                selectedColor.toLowerCase() === hex.toLowerCase()
                  ? 'ring-2 ring-white ring-offset-2 ring-offset-[#15151f] scale-105'
                  : 'border border-[#2a2a3a]'
              }`}
            />
          ))}
        </div>
        <div className="flex justify-center gap-3">
          {HEADER_COLORS.slice(4).map((hex) => (
            <button
              key={hex}
              type="button"
              onClick={() => setSelectedColor(hex)}
              style={{ backgroundColor: hex }}
              className={`w-9 h-9 rounded-full transition-transform active:scale-95 ${
                selectedColor.toLowerCase() === hex.toLowerCase()
                  ? 'ring-2 ring-white ring-offset-2 ring-offset-[#15151f] scale-105'
                  : 'border border-[#2a2a3a]'
              }`}
            />
          ))}
        </div>
      </div>

      <button
        type="button"
        onClick={() => onConfirm(selectedColor)}
        className="w-full h-11 mt-5 rounded-[10px] bg-[#9b8bff] text-[#100c26] text-[15px] font-bold hover:bg-[#8d7aff] transition-colors"
      >
        決定
      </button>

      <button
        type="button"
        onClick={onDismiss}
        className="w-full mt-2 py-2 text-[14px] text-[#8a8ea3] hover:text-[#eceef2] transition-colors"
      >
        キャンセル
      </button>
    </DialogModal>
  );
};

// 4. Rule Color Dialog
export const RuleColorDialog: React.FC<{
  initialColor?: string;
  onConfirm: (color: string) => void;
  onDismiss: () => void;
}> = ({ initialColor = '#52617A', onConfirm, onDismiss }) => {
  const [selectedColor, setSelectedColor] = useState(initialColor);

  return (
    <DialogModal onDismiss={onDismiss}>
      <h2 className="text-[14px] font-medium text-[#8a8ea3] mb-4">仕切り線の色</h2>

      <div className="flex justify-center gap-2.5 my-1">
        {RULE_COLORS.map((hex) => (
          <button
            key={hex}
            type="button"
            onClick={() => setSelectedColor(hex)}
            style={{ backgroundColor: hex }}
            className={`w-8 h-8 rounded-full transition-transform active:scale-95 ${
              selectedColor.toLowerCase() === hex.toLowerCase()
                ? 'ring-2 ring-white ring-offset-2 ring-offset-[#15151f] scale-105'
                : 'border border-[#2a2a3a]'
            }`}
          />
        ))}
      </div>

      <button
        type="button"
        onClick={() => onConfirm(selectedColor)}
        className="w-full h-11 mt-5 rounded-[10px] bg-[#9b8bff] text-[#100c26] text-[15px] font-bold hover:bg-[#8d7aff] transition-colors"
      >
        追加
      </button>

      <button
        type="button"
        onClick={onDismiss}
        className="w-full mt-2 py-2 text-[14px] text-[#8a8ea3] hover:text-[#eceef2] transition-colors"
      >
        キャンセル
      </button>
    </DialogModal>
  );
};

// 5. Edit Name Dialog
export const EditNameDialog: React.FC<{
  title: string;
  currentName: string;
  onConfirm: (name: string) => void;
  onDismiss: () => void;
}> = ({ title, currentName, onConfirm, onDismiss }) => {
  const [name, setName] = useState(currentName);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
    inputRef.current?.select();
  }, []);

  return (
    <DialogModal onDismiss={onDismiss}>
      <h2 className="text-[14px] font-medium text-[#8a8ea3] mb-3">{title}</h2>

      <input
        ref={inputRef}
        type="text"
        value={name}
        onChange={(e) => setName(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === 'Enter') onConfirm(name);
        }}
        className="w-full h-11 px-3 text-[15px] rounded-[8px] bg-[#1b1b28] border border-[#2a2a3a] text-[#eceef2] focus:border-[#9b8bff] focus:outline-none"
      />

      <button
        type="button"
        onClick={() => onConfirm(name)}
        className="w-full h-11 mt-4 rounded-[10px] bg-[#9b8bff] text-[#100c26] text-[15px] font-bold hover:bg-[#8d7aff] transition-colors"
      >
        保存
      </button>

      <button
        type="button"
        onClick={onDismiss}
        className="w-full mt-2 py-2 text-[14px] text-[#8a8ea3] hover:text-[#eceef2] transition-colors"
      >
        キャンセル
      </button>
    </DialogModal>
  );
};

// 6. Edit Number Dialog
export const EditNumberDialog: React.FC<{
  title: string;
  currentValue: number;
  min: number;
  max: number;
  onConfirm: (val: number) => void;
  onDismiss: () => void;
}> = ({ title, currentValue, min, max, onConfirm, onDismiss }) => {
  const [valStr, setValStr] = useState('');
  const [hasInteracted, setHasInteracted] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const handleConfirm = () => {
    if (!hasInteracted || valStr.trim() === '') {
      onConfirm(currentValue);
      return;
    }
    const num = parseInt(valStr, 10);
    const clamped = isNaN(num) ? currentValue : Math.max(min, Math.min(max, num));
    onConfirm(clamped);
  };

  return (
    <DialogModal onDismiss={onDismiss}>
      <h2 className="text-[14px] font-medium text-[#8a8ea3] mb-3">{title}</h2>

      <input
        ref={inputRef}
        type="text"
        inputMode="numeric"
        pattern="[0-9]*"
        placeholder={String(currentValue)}
        value={valStr}
        onFocus={() => {
          if (!hasInteracted) {
            setValStr('');
            setHasInteracted(true);
          }
        }}
        onChange={(e) => {
          setHasInteracted(true);
          setValStr(e.target.value.replace(/\D/g, '').slice(0, 4));
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter') handleConfirm();
        }}
        className="w-28 h-11 text-center text-[18px] font-bold rounded-[8px] bg-[#1b1b28] border border-[#2a2a3a] text-[#eceef2] placeholder:text-[#555b68] focus:border-[#9b8bff] focus:outline-none tabular-nums"
      />

      <button
        type="button"
        onClick={handleConfirm}
        className="w-full h-11 mt-4 rounded-[10px] bg-[#9b8bff] text-[#100c26] text-[15px] font-bold hover:bg-[#8d7aff] transition-colors"
      >
        保存
      </button>

      <button
        type="button"
        onClick={onDismiss}
        className="w-full mt-2 py-2 text-[14px] text-[#8a8ea3] hover:text-[#eceef2] transition-colors"
      >
        キャンセル
      </button>
    </DialogModal>
  );
};

// 7. Edit Rank Dialog
export const EditRankDialog: React.FC<{
  currentRank: number;
  onConfirm: (rank: number) => void;
  onDismiss: () => void;
}> = ({ currentRank, onConfirm, onDismiss }) => {
  const [rankStr, setRankStr] = useState('');
  const [hasInteracted, setHasInteracted] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const rankVal =
    !hasInteracted || rankStr.trim() === ''
      ? currentRank
      : Math.max(1, Math.min(200, parseInt(rankStr, 10) || currentRank));
  const previewMax = abyssMaxFor(rankVal);

  return (
    <DialogModal onDismiss={onDismiss}>
      <h2 className="text-[14px] font-medium text-[#8a8ea3] mb-1">
        ランク編集 (Lv.1〜200)
      </h2>
      <p className="text-[13px] font-bold text-[#ffab5c] mb-3">
        最大スタミナ: {previewMax}
      </p>

      <input
        ref={inputRef}
        type="text"
        inputMode="numeric"
        pattern="[0-9]*"
        placeholder={String(currentRank)}
        value={rankStr}
        onFocus={() => {
          if (!hasInteracted) {
            setRankStr('');
            setHasInteracted(true);
          }
        }}
        onChange={(e) => {
          setHasInteracted(true);
          setRankStr(e.target.value.replace(/\D/g, '').slice(0, 3));
        }}
        onKeyDown={(e) => {
          if (e.key === 'Enter') onConfirm(rankVal);
        }}
        className="w-24 h-11 text-center text-[18px] font-bold rounded-[8px] bg-[#1b1b28] border border-[#2a2a3a] text-[#eceef2] placeholder:text-[#555b68] focus:border-[#9b8bff] focus:outline-none tabular-nums"
      />

      <button
        type="button"
        onClick={() => onConfirm(rankVal)}
        className="w-full h-11 mt-4 rounded-[10px] bg-[#9b8bff] text-[#100c26] text-[15px] font-bold hover:bg-[#8d7aff] transition-colors"
      >
        保存
      </button>

      <button
        type="button"
        onClick={onDismiss}
        className="w-full mt-2 py-2 text-[14px] text-[#8a8ea3] hover:text-[#eceef2] transition-colors"
      >
        キャンセル
      </button>
    </DialogModal>
  );
};

// 8. Action Menu Dialog with 2-step Delete
export const ActionMenuDialog: React.FC<{
  item: TimerEntry;
  parentGroupId?: string | null;
  onAddTimerToGroup: () => void;
  onChangeHeaderColor: () => void;
  onEditName: () => void;
  onDelete: () => void;
  onDismiss: () => void;
}> = ({
  item,
  onAddTimerToGroup,
  onChangeHeaderColor,
  onEditName,
  onDelete,
  onDismiss,
}) => {
  const [armedDelete, setArmedDelete] = useState(false);

  const title =
    item.type === 'group'
      ? 'アカウントの操作'
      : item.type === 'header'
      ? '見出しの操作'
      : item.type === 'rule'
      ? '仕切り線の操作'
      : 'タイマーの操作';

  const isGroupWithSpace = item.type === 'group' && (item as GroupItem).children.length < 2;
  const isHeader = item.type === 'header';
  const hasName = item.type === 'group' || item.type === 'header';

  return (
    <DialogModal onDismiss={onDismiss}>
      <h2 className="text-[16px] font-bold text-[#eceef2] mb-3.5">{title}</h2>

      <div className="w-full flex flex-col gap-2">
        {isGroupWithSpace && (
          <button
            type="button"
            onClick={onAddTimerToGroup}
            className="w-full h-11 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#9b8bff] text-[15px] font-bold hover:bg-[#252536] transition-colors"
          >
            タイマー追加
          </button>
        )}

        {isHeader && (
          <button
            type="button"
            onClick={onChangeHeaderColor}
            className="w-full h-11 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#9b8bff] text-[15px] font-bold hover:bg-[#252536] transition-colors"
          >
            色を変更
          </button>
        )}

        {hasName && (
          <button
            type="button"
            onClick={onEditName}
            className="w-full h-11 rounded-[10px] bg-[#1b1b28] border border-[#2a2a3a] text-[#eceef2] text-[15px] font-bold hover:bg-[#252536] transition-colors"
          >
            名前を変更
          </button>
        )}

        {/* 2-step armed delete button */}
        <button
          type="button"
          data-testid="btn_delete_confirm"
          onClick={() => {
            if (!armedDelete) {
              setArmedDelete(true);
            } else {
              onDelete();
            }
          }}
          className={`w-full h-11 rounded-[10px] text-[15px] font-bold transition-colors ${
            armedDelete
              ? 'bg-[#ff6b6b] text-white border-2 border-white animate-pulse'
              : 'bg-[#1b1b28] border border-[#2a2a3a] text-[#ff6b6b] hover:bg-[#252536]'
          }`}
        >
          {armedDelete ? '本当に削除しますか？ (タップで確定)' : '削除'}
        </button>
      </div>

      <button
        type="button"
        onClick={onDismiss}
        className="w-full mt-3 py-2 text-[14px] text-[#8a8ea3] hover:text-[#eceef2] transition-colors"
      >
        やめる
      </button>
    </DialogModal>
  );
};

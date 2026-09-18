import React, { useState, useRef, useEffect } from 'react';
import { useLongPress } from '../utils/useLongPress';

interface InlineNumberEditorProps {
  value: number;
  min?: number;
  max?: number;
  prefix?: string;
  textColor?: string;
  className?: string;
  disabled?: boolean;
  onConfirm: (val: number) => void;
  onLongClick?: () => void;
  onEditingChange?: (isEditing: boolean) => void;
}

export const InlineNumberEditor: React.FC<InlineNumberEditorProps> = ({
  value,
  min = 0,
  max = 9999,
  prefix = '',
  textColor = '#eceef2',
  className = '',
  disabled = false,
  onConfirm,
  onLongClick,
  onEditingChange,
}) => {
  const [isEditing, setIsEditing] = useState(false);
  const [textVal, setTextVal] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  const containerRef = useRef<HTMLSpanElement>(null);
  const initialValRef = useRef(value);

  // Notify parent if editing state changes
  useEffect(() => {
    onEditingChange?.(isEditing);
  }, [isEditing, onEditingChange]);

  const startEdit = () => {
    if (disabled) return;
    initialValRef.current = value;
    setTextVal(''); // Immediately clear to blank on tap
    setIsEditing(true);

    // Focus hidden input to open soft keyboard
    setTimeout(() => {
      inputRef.current?.focus();
    }, 10);
  };

  const longPressHandlers = useLongPress({
    onClick: (e) => {
      e.stopPropagation();
      startEdit();
    },
    onLongPress: (e) => {
      e.stopPropagation();
      if (onLongClick) {
        onLongClick();
      }
    },
  });

  const commitOrCancel = () => {
    if (!isEditing) return;

    const trimmed = textVal.trim();
    if (trimmed === '') {
      // User tapped outside without typing: restore original value
      setIsEditing(false);
      return;
    }

    const parsed = parseInt(trimmed, 10);
    if (isNaN(parsed)) {
      setIsEditing(false);
      return;
    }

    const clamped = Math.max(min, Math.min(max, parsed));
    if (clamped !== initialValRef.current) {
      onConfirm(clamped);
    }
    setIsEditing(false);
  };

  // Listen to pointerdown outside the editor while editing to immediately commit or cancel
  useEffect(() => {
    if (!isEditing) return;

    const handlePointerDown = (e: PointerEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        commitOrCancel();
      }
    };

    window.addEventListener('pointerdown', handlePointerDown, { capture: true });
    return () => {
      window.removeEventListener('pointerdown', handlePointerDown, { capture: true });
    };
  }, [isEditing, textVal]);

  return (
    <span
      ref={containerRef}
      className="relative inline-flex items-center"
      onClick={(e) => e.stopPropagation()}
    >
      {/* Hidden input to receive soft keyboard typing without Android teardrop cursor */}
      {isEditing && (
        <input
          ref={inputRef}
          type="text"
          inputMode="numeric"
          pattern="[0-9]*"
          value={textVal}
          onChange={(e) => {
            const digits = e.target.value.replace(/\D/g, '').slice(0, 4);
            setTextVal(digits);
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              commitOrCancel();
            } else if (e.key === 'Escape') {
              setIsEditing(false);
            }
          }}
          onBlur={() => {
            commitOrCancel();
          }}
          style={{
            position: 'fixed',
            bottom: 0,
            left: 0,
            width: '1px',
            height: '1px',
            opacity: 0.001,
            fontSize: '16px',
            border: 'none',
            padding: 0,
            margin: 0,
            caretColor: 'transparent',
            pointerEvents: 'none',
            zIndex: -1,
          }}
          tabIndex={-1}
          aria-hidden="true"
        />
      )}

      {isEditing ? (
        <span className="relative inline-flex items-center justify-center min-w-[28px] h-[22px] px-1 rounded bg-[#1e1e2d] border border-[#9b8bff] select-none">
          {prefix && (
            <span className="text-[#8a8ea3] text-[10px] mr-0.5 font-bold">{prefix}</span>
          )}
          {textVal === '' ? (
            <span className="inline-block w-[1.5px] h-3.5 bg-[#9b8bff] animate-pulse" />
          ) : (
            <>
              <span className="text-[17px] font-bold text-[#eceef2] tabular-nums leading-none">
                {textVal}
              </span>
              <span className="inline-block w-[1.5px] h-3.5 bg-[#9b8bff] animate-pulse ml-0.5" />
            </>
          )}
        </span>
      ) : (
        <span
          {...longPressHandlers}
          style={{ color: textColor }}
          className={`tabular-nums cursor-pointer hover:opacity-80 transition-opacity select-none ${className}`}
        >
          {prefix}
          {value}
        </span>
      )}
    </span>
  );
};

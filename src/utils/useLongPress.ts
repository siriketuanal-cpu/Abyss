import { useCallback, useRef } from 'react';

interface LongPressOptions {
  onClick?: (e: React.MouseEvent | React.TouchEvent) => void;
  onLongPress: (e: React.MouseEvent | React.TouchEvent) => void;
  delay?: number;
}

export function useLongPress({ onClick, onLongPress, delay = 500 }: LongPressOptions) {
  const timeoutRef = useRef<number | null>(null);
  const isLongPressRef = useRef(false);
  const startPosRef = useRef<{ x: number; y: number } | null>(null);

  const start = useCallback(
    (e: React.MouseEvent | React.TouchEvent) => {
      isLongPressRef.current = false;
      if ('touches' in e && e.touches.length > 0) {
        startPosRef.current = { x: e.touches[0].clientX, y: e.touches[0].clientY };
      } else if ('clientX' in e) {
        startPosRef.current = { x: e.clientX, y: e.clientY };
      }

      timeoutRef.current = window.setTimeout(() => {
        isLongPressRef.current = true;
        onLongPress(e);
      }, delay);
    },
    [onLongPress, delay]
  );

  const clear = useCallback(
    (e: React.MouseEvent | React.TouchEvent, shouldTriggerClick = true) => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }
      if (shouldTriggerClick && !isLongPressRef.current && onClick) {
        onClick(e);
      }
      startPosRef.current = null;
    },
    [onClick]
  );

  const move = useCallback((e: React.TouchEvent | React.MouseEvent) => {
    if (!startPosRef.current) return;
    let currentX = 0;
    let currentY = 0;
    if ('touches' in e && e.touches.length > 0) {
      currentX = e.touches[0].clientX;
      currentY = e.touches[0].clientY;
    } else if ('clientX' in e) {
      currentX = e.clientX;
      currentY = e.clientY;
    }
    const diffX = Math.abs(currentX - startPosRef.current.x);
    const diffY = Math.abs(currentY - startPosRef.current.y);
    // If moved more than 10px, cancel long press
    if (diffX > 10 || diffY > 10) {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
        timeoutRef.current = null;
      }
    }
  }, []);

  return {
    onMouseDown: (e: React.MouseEvent) => {
      if (e.button !== 0) return;
      start(e);
    },
    onMouseUp: (e: React.MouseEvent) => {
      if (e.button !== 0) return;
      clear(e, true);
    },
    onMouseLeave: (e: React.MouseEvent) => clear(e, false),
    onTouchStart: (e: React.TouchEvent) => start(e),
    onTouchEnd: (e: React.TouchEvent) => clear(e, true),
    onTouchMove: (e: React.TouchEvent) => move(e),
    onContextMenu: (e: React.MouseEvent) => {
      e.preventDefault();
      onLongPress(e);
    },
  };
}

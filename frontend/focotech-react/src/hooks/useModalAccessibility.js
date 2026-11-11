import { useEffect, useRef } from 'react';
/** Focus containment, Escape and focus restoration for payment dialogs. */
export default function useModalAccessibility(enabled, onClose) {
  const ref = useRef(null);
  const close = useRef(onClose);
  close.current = onClose;
  useEffect(() => {
    if (!enabled || !ref.current) return undefined;
    const previous = document.activeElement;
    const root = ref.current;
    const focusable = () => [...root.querySelectorAll('button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex="0"]')].filter(node => node.getClientRects().length > 0);
    (focusable()[0] || root).focus();
    const keydown = event => {
      if (event.key === 'Escape') { event.preventDefault(); close.current?.(); }
      if (event.key !== 'Tab') return;
      const nodes = focusable();
      if (!nodes.length) { event.preventDefault(); root.focus(); return; }
      const first = nodes[0]; const last = nodes[nodes.length - 1];
      if (event.shiftKey && (document.activeElement === first || !root.contains(document.activeElement))) { event.preventDefault(); last.focus(); }
      else if (!event.shiftKey && (document.activeElement === last || !root.contains(document.activeElement))) { event.preventDefault(); first.focus(); }
    };
    document.addEventListener('keydown', keydown);
    return () => { document.removeEventListener('keydown', keydown); if (previous?.isConnected) previous.focus(); };
  }, [enabled]);
  return ref;
}

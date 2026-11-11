/** Explicit development-only payment accessibility review; excluded from production builds. */
import axe from 'axe-core';
export function enablePaymentAccessibilityReview() {
  const report = document.createElement('output'); report.id = 'payment-a11y-report'; report.hidden = true; document.body.appendChild(report);
  let timer; let running = false;
  const check = async () => {
    if (running) return;
    running = true;
    try {
      const context = document.querySelector('[role="dialog"]') || document.querySelector('main') || document;
      const result = await axe.run(context, { runOnly: { type: 'tag', values: ['wcag2a', 'wcag2aa', 'wcag21aa'] } });
      report.textContent = JSON.stringify({ at: new Date().toISOString(), violations: result.violations.map(v => ({ id: v.id, impact: v.impact, description: v.description, nodes: v.nodes.map(n => ({ target: n.target, html: n.html, summary: n.failureSummary })) })), passes: result.passes.length, incomplete: result.incomplete.length });
    } catch (error) { report.textContent = JSON.stringify({ error: error.message }); }
    finally { running = false; }
  };
  const observer = new MutationObserver(changes => {
    if (changes.every(change => change.target === report || report.contains(change.target))) return;
    clearTimeout(timer); timer = setTimeout(check, 400);
  });
  observer.observe(document.getElementById('root'), { childList: true, subtree: true });
  check();
}

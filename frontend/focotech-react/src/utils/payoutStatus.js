export const payoutStatusLabel = status => ({
  paid: 'Paid out', unpaid: 'Accrued (unpaid)', halted: 'On hold', refunded: 'Refunded',
  refunded_paid: 'Refunded after payout', failed: 'Payout failed', pending: 'Payout pending',
  processing: 'Payout processing', delayed: 'Payout delayed', disputed: 'Disputed — on hold',
  disputed_paid: 'Disputed after payout', reversed: 'Reversed',
}[status] || 'Unknown payout status');

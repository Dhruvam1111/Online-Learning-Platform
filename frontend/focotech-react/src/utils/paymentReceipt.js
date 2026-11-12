import api from '../services/api';

export async function openPaymentReceipt(transaction, print = false) {
  const preview = print ? window.open('about:blank', '_blank') : null;
  if (print && !preview) throw new Error('Allow pop-ups to print your receipt.');
  try {
    const response = await api.get(`/payments/invoices/${encodeURIComponent(transaction.orderId)}/download`, {responseType:'blob'});
    const url = URL.createObjectURL(new Blob([response.data], {type:'text/html'}));
    if (print) { preview.onload = () => preview.print(); preview.location.href = url; }
    else {
      const a = document.createElement('a'); a.href = url;
      a.download = `Receipt-${transaction.invoiceNumber || transaction.orderId}.html`;
      document.body.appendChild(a); a.click(); a.remove();
    }
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch (error) { preview?.close(); throw error; }
}

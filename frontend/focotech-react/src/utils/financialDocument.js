import api from '../services/api';
export async function downloadFinancialDocument(path, name) {
  const response = await api.get(path, { responseType: 'blob' });
  const url = URL.createObjectURL(new Blob([response.data], { type: 'text/html' }));
  const link = document.createElement('a'); link.href = url; link.download = name;
  document.body.appendChild(link); link.click(); link.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

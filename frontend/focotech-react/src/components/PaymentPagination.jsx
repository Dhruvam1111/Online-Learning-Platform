import React from 'react';
export default function PaymentPagination({ page, totalPages, onPage, label }) {
  const pages = Math.max(1, Number(totalPages) || 1);
  return <nav aria-label={`${label} pagination`} className="flex flex-wrap items-center gap-3 my-3 text-sm">
    <button className="border rounded px-3 py-2 disabled:opacity-50" disabled={page <= 0} onClick={() => onPage(page - 1)}>Previous {label}</button>
    <span>Page {page + 1} of {pages}</span>
    <button className="border rounded px-3 py-2 disabled:opacity-50" disabled={page + 1 >= pages} onClick={() => onPage(page + 1)}>Next {label}</button>
  </nav>;
}

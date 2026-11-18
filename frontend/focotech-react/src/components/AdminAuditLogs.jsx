import React, { useState, useEffect } from 'react';
import { adminAPI } from '../services/api';

/**
 * Enterprise Admin Action Audit Log Viewer (FR-A31, ADM-015)
 * Displays filterable, immutable audit history of all administrative mutations.
 */
export default function AdminAuditLogs() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Pagination & Filters
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  const [filterAction, setFilterAction] = useState('');
  const [filterTargetType, setFilterTargetType] = useState('');
  const [searchQuery, setSearchQuery] = useState('');

  // Details Modal
  const [selectedLog, setSelectedLog] = useState(null);

  const loadAuditLogs = async () => {
    setLoading(true);
    setError(null);
    try {
      const params = {
        page,
        size,
        sortBy: 'createdAt',
        sortDir: 'desc',
      };
      if (filterAction) params.action = filterAction;
      if (filterTargetType) params.targetType = filterTargetType;
      if (searchQuery.trim()) params.search = searchQuery.trim();

      const response = await adminAPI.getAuditLogs(params);
      const data = response.data;
      setLogs(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load audit logs:', err);
      setError('Unable to load audit logs. Please ensure you have administrative privileges.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAuditLogs();
  }, [page, size, filterAction, filterTargetType]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(0);
    loadAuditLogs();
  };

  const getActionBadgeClass = (action = '') => {
    if (action.includes('APPROVED') || action.includes('ACTIVATED')) {
      return 'bg-emerald-100 text-emerald-800 border-emerald-200';
    }
    if (action.includes('REJECTED') || action.includes('SUSPENDED') || action.includes('DELETED')) {
      return 'bg-rose-100 text-rose-800 border-rose-200';
    }
    if (action.includes('CHANGES_REQUESTED') || action.includes('UNPUBLISHED')) {
      return 'bg-amber-100 text-amber-800 border-amber-200';
    }
    return 'bg-purple-100 text-purple-800 border-purple-200';
  };

  return (
    <div className="bg-white rounded-3xl p-6 border border-[#fae8dc] shadow-xs flex flex-col gap-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-[#faeae0]">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-[20px] font-extrabold text-[#1e1916]">Administrative Audit Log</h2>
            <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-neutral-100 text-[#6d635c] border border-neutral-200">
              FR-A31 • Immutable
            </span>
          </div>
          <p className="text-xs text-[#8e8076] mt-0.5">
            Strict append-only record of all administrative operations, entity changes, and governance events.
          </p>
        </div>

        <button
          onClick={() => { setPage(0); loadAuditLogs(); }}
          className="flex items-center gap-1.5 px-3 py-2 rounded-xl border border-[#faeae0] text-xs font-bold text-[#6d635c] hover:bg-[#fbf7f4] transition cursor-pointer self-start md:self-auto"
        >
          <span className="material-symbols-outlined text-[16px]">refresh</span>
          <span>Refresh</span>
        </button>
      </div>

      {/* Filter Bar */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-3 bg-[#fdfbf9] p-4 rounded-2xl border border-[#faeae0]">
        <div>
          <label className="block text-[11px] font-bold text-[#8e8076] uppercase mb-1">
            Action Type
          </label>
          <select
            value={filterAction}
            onChange={(e) => { setFilterAction(e.target.value); setPage(0); }}
            className="w-full p-2 rounded-xl border border-[#faeae0] bg-white text-xs font-medium focus:border-[#9e431a] focus:outline-none"
          >
            <option value="">All Actions</option>
            <option value="INSTRUCTOR_APPROVED">Instructor Approved</option>
            <option value="INSTRUCTOR_REJECTED">Instructor Rejected</option>
            <option value="USER_SUSPENDED">User Suspended</option>
            <option value="USER_ACTIVATED">User Activated</option>
            <option value="USER_DELETED">User Deleted</option>
            <option value="COURSE_APPROVED">Course Approved</option>
            <option value="COURSE_REJECTED">Course Rejected</option>
            <option value="COURSE_CHANGES_REQUESTED">Course Changes Requested</option>
            <option value="COURSE_UNPUBLISHED">Course Unpublished</option>
            <option value="COURSE_DELETED">Course Deleted</option>
          </select>
        </div>

        <div>
          <label className="block text-[11px] font-bold text-[#8e8076] uppercase mb-1">
            Target Type
          </label>
          <select
            value={filterTargetType}
            onChange={(e) => { setFilterTargetType(e.target.value); setPage(0); }}
            className="w-full p-2 rounded-xl border border-[#faeae0] bg-white text-xs font-medium focus:border-[#9e431a] focus:outline-none"
          >
            <option value="">All Targets</option>
            <option value="USER">User / Instructor</option>
            <option value="COURSE">Course</option>
            <option value="PLATFORM">Platform</option>
          </select>
        </div>

        <form onSubmit={handleSearchSubmit} className="md:col-span-2 flex flex-col justify-end">
          <label className="block text-[11px] font-bold text-[#8e8076] uppercase mb-1">
            Search Target / Details
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              placeholder="Search by target name, ID, or details..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="flex-1 p-2 rounded-xl border border-[#faeae0] bg-white text-xs font-medium focus:border-[#9e431a] focus:outline-none"
            />
            <button
              type="submit"
              className="px-4 py-2 rounded-xl bg-[#9e431a] text-white text-xs font-bold hover:bg-[#863713] transition cursor-pointer"
            >
              Search
            </button>
          </div>
        </form>
      </div>

      {/* Error Message */}
      {error && (
        <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-semibold">
          {error}
        </div>
      )}

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead>
            <tr className="border-b border-[#faeae0] text-[#8e8076] font-bold uppercase tracking-wider">
              <th className="py-3 px-3">Timestamp</th>
              <th className="py-3 px-3">Actor (Admin)</th>
              <th className="py-3 px-3">Action</th>
              <th className="py-3 px-3">Target</th>
              <th className="py-3 px-3">Change Summary</th>
              <th className="py-3 px-3 text-right">Details</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[#f7f0eb]">
            {loading ? (
              <tr>
                <td colSpan="6" className="py-8 text-center text-xs text-[#8e8076]">
                  Loading audit log records...
                </td>
              </tr>
            ) : logs.length === 0 ? (
              <tr>
                <td colSpan="6" className="py-8 text-center text-xs text-[#8e8076]">
                  No audit log entries matching criteria.
                </td>
              </tr>
            ) : (
              logs.map((log) => (
                <tr key={log.id} className="hover:bg-[#fbf7f4] transition-colors">
                  <td className="py-3 px-3 font-mono text-[#8e8076] whitespace-nowrap">
                    {log.createdAt ? new Date(log.createdAt).toLocaleString() : '—'}
                  </td>
                  <td className="py-3 px-3">
                    <div className="font-bold text-[#1e1916]">{log.actorName || 'System Admin'}</div>
                    <div className="text-[11px] text-[#8e8076]">{log.actorEmail}</div>
                  </td>
                  <td className="py-3 px-3">
                    <span
                      className={`px-2 py-0.5 rounded-full text-[10px] font-bold border uppercase tracking-wider inline-block ${getActionBadgeClass(
                        log.action
                      )}`}
                    >
                      {log.action?.replace(/_/g, ' ')}
                    </span>
                  </td>
                  <td className="py-3 px-3">
                    <div className="font-medium text-[#1e1916] truncate max-w-[160px]">
                      {log.targetName || `#${log.targetId}`}
                    </div>
                    <div className="text-[10px] text-[#8e8076] uppercase tracking-wider font-semibold">
                      {log.targetType} #{log.targetId}
                    </div>
                  </td>
                  <td className="py-3 px-3">
                    {log.previousState || log.newState ? (
                      <div className="flex items-center gap-1.5 text-[11px]">
                        <span className="line-through text-[#8e8076]">{log.previousState || 'none'}</span>
                        <span className="text-[#8e8076]">➔</span>
                        <span className="font-bold text-emerald-700">{log.newState || 'none'}</span>
                      </div>
                    ) : (
                      <div className="text-[11px] text-[#6d635c] truncate max-w-[200px]">
                        {log.details || '—'}
                      </div>
                    )}
                  </td>
                  <td className="py-3 px-3 text-right">
                    <button
                      onClick={() => setSelectedLog(log)}
                      className="px-2.5 py-1 rounded-lg border border-[#faeae0] text-[11px] font-bold text-[#6d635c] hover:bg-white hover:border-[#9e431a] transition cursor-pointer"
                    >
                      View
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Footer */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4 pt-4 border-t border-[#faeae0] text-xs text-[#8e8076]">
        <div>
          Showing page <span className="font-bold text-[#1e1916]">{page + 1}</span> of{' '}
          <span className="font-bold text-[#1e1916]">{totalPages}</span> ({totalElements} total entries)
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0 || loading}
            className="px-3 py-1.5 rounded-xl border border-[#faeae0] font-bold disabled:opacity-40 disabled:cursor-not-allowed hover:bg-[#fbf7f4] transition cursor-pointer"
          >
            Previous
          </button>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1 || loading}
            className="px-3 py-1.5 rounded-xl border border-[#faeae0] font-bold disabled:opacity-40 disabled:cursor-not-allowed hover:bg-[#fbf7f4] transition cursor-pointer"
          >
            Next
          </button>
        </div>
      </div>

      {/* Detail Modal / Drawer */}
      {selectedLog && (
        <div className="fixed inset-0 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-3xl p-6 border border-[#fae8dc] shadow-xl max-w-lg w-full flex flex-col gap-4">
            <div className="flex items-center justify-between pb-3 border-b border-[#faeae0]">
              <div>
                <h3 className="text-base font-extrabold text-[#1e1916]">Audit Log Record #{selectedLog.id}</h3>
                <p className="text-[11px] text-[#8e8076] font-mono">
                  {new Date(selectedLog.createdAt).toLocaleString()}
                </p>
              </div>
              <button
                onClick={() => setSelectedLog(null)}
                className="p-1 rounded-full text-[#8e8076] hover:bg-neutral-100 transition cursor-pointer"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-2 bg-[#fdfbf9] p-3 rounded-xl border border-[#faeae0]">
                <div>
                  <span className="text-[10px] uppercase font-bold text-[#8e8076] block">Action</span>
                  <span className="font-bold text-[#1e1916]">{selectedLog.action}</span>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-bold text-[#8e8076] block">Actor</span>
                  <span className="font-bold text-[#1e1916]">{selectedLog.actorName} ({selectedLog.actorEmail})</span>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-bold text-[#8e8076] block">Target Type</span>
                  <span className="font-semibold text-[#1e1916]">{selectedLog.targetType}</span>
                </div>
                <div>
                  <span className="text-[10px] uppercase font-bold text-[#8e8076] block">Target ID</span>
                  <span className="font-mono text-[#1e1916]">#{selectedLog.targetId}</span>
                </div>
              </div>

              <div>
                <span className="text-[10px] uppercase font-bold text-[#8e8076] block mb-1">Target Name</span>
                <p className="p-2.5 bg-neutral-50 rounded-xl border border-[#faeae0] text-[#1e1916] font-medium">
                  {selectedLog.targetName || 'None'}
                </p>
              </div>

              <div>
                <span className="text-[10px] uppercase font-bold text-[#8e8076] block mb-1">Details &amp; Comments</span>
                <p className="p-2.5 bg-neutral-50 rounded-xl border border-[#faeae0] text-[#1e1916] whitespace-pre-wrap">
                  {selectedLog.details || 'No additional details.'}
                </p>
              </div>

              {(selectedLog.previousState || selectedLog.newState) && (
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <span className="text-[10px] uppercase font-bold text-[#8e8076] block mb-1">Previous State</span>
                    <div className="p-2 bg-rose-50 border border-rose-200 rounded-xl font-mono text-rose-800 text-[11px]">
                      {selectedLog.previousState || 'None'}
                    </div>
                  </div>
                  <div>
                    <span className="text-[10px] uppercase font-bold text-[#8e8076] block mb-1">New State</span>
                    <div className="p-2 bg-emerald-50 border border-emerald-200 rounded-xl font-mono text-emerald-800 text-[11px]">
                      {selectedLog.newState || 'None'}
                    </div>
                  </div>
                </div>
              )}
            </div>

            <div className="pt-2 flex justify-end">
              <button
                onClick={() => setSelectedLog(null)}
                className="px-4 py-2 rounded-xl bg-[#1e1916] text-white text-xs font-bold hover:bg-neutral-800 transition cursor-pointer"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

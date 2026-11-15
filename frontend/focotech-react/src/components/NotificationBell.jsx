import React, { useState, useEffect, useRef, useCallback } from 'react';
import { notificationAPI } from '../services/api';

/**
 * Step 5 / FR-I70: Notification Centre Bell with Unread Badge & Dropdown Drawer.
 */
export default function NotificationBell({ isAuthenticated = true }) {
  const [open, setOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [loading, setLoading] = useState(false);
  const dropdownRef = useRef(null);

  const loadUnreadCount = useCallback(() => {
    if (!isAuthenticated) return;
    notificationAPI
      .getUnreadCount()
      .then((res) => {
        if (typeof res.data?.unreadCount === 'number') {
          setUnreadCount(res.data.unreadCount);
        }
      })
      .catch(() => {});
  }, [isAuthenticated]);

  const loadNotifications = useCallback(
    (filterUnread = unreadOnly) => {
      if (!isAuthenticated) return;
      setLoading(true);
      notificationAPI
        .getAll(filterUnread)
        .then((res) => {
          if (Array.isArray(res.data)) {
            setNotifications(res.data);
          }
        })
        .catch(() => {})
        .finally(() => setLoading(false));
    },
    [isAuthenticated, unreadOnly]
  );

  useEffect(() => {
    loadUnreadCount();
    const interval = setInterval(loadUnreadCount, 30000);
    return () => clearInterval(interval);
  }, [loadUnreadCount]);

  useEffect(() => {
    if (open) {
      loadNotifications(unreadOnly);
      loadUnreadCount();
    }
  }, [open, unreadOnly, loadNotifications, loadUnreadCount]);

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setOpen(false);
      }
    };
    if (open) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  if (!isAuthenticated) return null;

  const handleMarkAsRead = async (notification) => {
    if (!notification.read) {
      try {
        await notificationAPI.markAsRead(notification.id);
        setNotifications((prev) =>
          unreadOnly
            ? prev.filter((n) => n.id !== notification.id)
            : prev.map((n) => (n.id === notification.id ? { ...n, read: true } : n))
        );
        setUnreadCount((prev) => Math.max(0, prev - 1));
      } catch {
        // Ignore transient network errors
      }
    }
    if (notification.link) {
      setOpen(false);
      if (notification.link.startsWith('#')) {
        window.location.hash = notification.link;
      } else {
        window.location.href = notification.link;
      }
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationAPI.markAllAsRead();
      setUnreadCount(0);
      setNotifications((prev) =>
        unreadOnly ? [] : prev.map((n) => ({ ...n, read: true }))
      );
    } catch {
      // Ignore transient network errors
    }
  };

  const formatTime = (isoString) => {
    if (!isoString) return '';
    try {
      const date = new Date(isoString);
      return date.toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return '';
    }
  };

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setOpen((prev) => !prev)}
        className="relative p-2 rounded-full bg-surface-container hover:bg-surface-container-high text-on-surface transition-colors cursor-pointer flex items-center justify-center"
        aria-label="Notification Centre"
        title="Notification Centre"
      >
        <span className="material-symbols-outlined text-[20px]">notifications</span>
        {unreadCount > 0 && (
          <span
            data-testid="notification-unread-badge"
            className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 rounded-full bg-[#ff5a36] text-white text-[10px] font-extrabold flex items-center justify-center shadow-sm"
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-2xl shadow-2xl border border-[#fae8dc] z-50 overflow-hidden">
          <div className="px-4 py-3 bg-[#fcf6f2] border-b border-[#fae8dc] flex items-center justify-between">
            <div className="flex items-center gap-2">
              <span className="text-xs font-extrabold text-[#1e1916]">
                Notifications
              </span>
              {unreadCount > 0 && (
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-[#ff5a36]/15 text-[#b52603]">
                  {unreadCount} unread
                </span>
              )}
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setUnreadOnly((prev) => !prev)}
                className={`px-2 py-0.5 rounded-full text-[10px] font-bold cursor-pointer transition ${
                  unreadOnly
                    ? 'bg-blue-600 text-white'
                    : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200'
                }`}
              >
                {unreadOnly ? 'Unread Only' : 'All'}
              </button>
              {unreadCount > 0 && (
                <button
                  type="button"
                  onClick={handleMarkAllRead}
                  className="text-[11px] font-bold text-blue-600 hover:text-blue-800 cursor-pointer"
                >
                  Mark all read
                </button>
              )}
            </div>
          </div>

          <div className="max-h-80 overflow-y-auto divide-y divide-[#f5ebe4]">
            {loading ? (
              <div className="p-6 text-center text-xs text-[#786f68]">
                Loading notifications...
              </div>
            ) : notifications.length === 0 ? (
              <div className="p-6 text-center text-xs text-[#786f68]">
                {unreadOnly ? 'No unread notifications.' : 'No notifications yet.'}
              </div>
            ) : (
              notifications.map((n) => (
                <div
                  key={n.id}
                  onClick={() => handleMarkAsRead(n)}
                  className={`px-4 py-3 cursor-pointer transition-colors flex items-start gap-3 ${
                    n.read ? 'bg-white hover:bg-neutral-50' : 'bg-orange-50/50 hover:bg-orange-50'
                  }`}
                >
                  <span
                    className={`mt-1.5 w-2 h-2 rounded-full shrink-0 ${
                      n.read ? 'bg-transparent' : 'bg-[#ff5a36]'
                    }`}
                  />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between gap-2">
                      <span className="text-[10px] font-extrabold uppercase tracking-wider text-[#b52603]">
                        {n.type || 'SYSTEM'}
                      </span>
                      <span className="text-[10px] text-[#8a817a]">
                        {formatTime(n.createdAt)}
                      </span>
                    </div>
                    {n.title && (
                      <p className="text-xs font-bold text-[#1e1916] mt-0.5 truncate">
                        {n.title}
                      </p>
                    )}
                    <p className="text-xs text-[#443e3a] mt-0.5 line-clamp-2">
                      {n.message}
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}


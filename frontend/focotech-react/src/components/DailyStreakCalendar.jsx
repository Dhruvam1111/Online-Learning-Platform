import React, { useState, useEffect } from 'react';

export default function DailyStreakCalendar() {
  // Live ticking countdown for the daily challenge
  const [timeLeft, setTimeLeft] = useState({ hours: 9, minutes: 47, seconds: 26 });

  useEffect(() => {
    const timer = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev.seconds > 0) {
          return { ...prev, seconds: prev.seconds - 1 };
        } else if (prev.minutes > 0) {
          return { ...prev, minutes: prev.minutes - 1, seconds: 59 };
        } else if (prev.hours > 0) {
          return { hours: prev.hours - 1, minutes: 59, seconds: 59 };
        }
        return { hours: 23, minutes: 59, seconds: 59 };
      });
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const formatTwoDigits = (num) => String(num).padStart(2, '0');

  // Days of the week header
  const weekDays = ['S', 'M', 'T', 'W', 'T', 'F', 'S'];

  // Calendar matrix for the month (starts on Thursday = index 4)
  // Day 1 to 11 are completed (checkmarks)
  // Day 12 is active (today)
  // Day 13 to 31 are upcoming
  const daysInMonth = [];
  // 4 leading empty slots for Thursday start
  for (let i = 0; i < 4; i++) {
    daysInMonth.push({ empty: true, key: `empty-${i}` });
  }
  for (let day = 1; day <= 31; day++) {
    daysInMonth.push({
      day,
      completed: day < 12,
      isToday: day === 12,
      empty: false,
      key: `day-${day}`
    });
  }

  return (
    <div className="bg-white rounded-2xl p-4 sm:p-5 border border-[#fae9de] shadow-[0_6px_24px_rgba(255,90,54,0.06)] flex flex-col gap-3.5 relative overflow-hidden transition-all duration-300 hover:shadow-md">
      {/* ========================================================
          1. HEADER ROW: Day 12, Countdown, Month Arrows & 3D Badge
          ======================================================== */}
      <div className="flex items-center justify-between gap-2 relative z-10">
        {/* Left: Day 12 & Live Countdown */}
        <div className="flex items-baseline gap-2 min-w-0">
          <span className="font-extrabold text-[16px] text-[#1f1a17] tracking-tight whitespace-nowrap">
            Day 12
          </span>
          <span className="text-[11px] font-mono font-semibold text-[#8e8076] tabular-nums whitespace-nowrap">
            {formatTwoDigits(timeLeft.hours)}:{formatTwoDigits(timeLeft.minutes)}:
            {formatTwoDigits(timeLeft.seconds)} left
          </span>
        </div>

        {/* Right: Navigation Arrows + 3D Hexagonal Badge */}
        <div className="flex items-center gap-2.5 shrink-0">
          {/* Month Arrows */}
          <div className="flex items-center gap-0.5">
            <button
              type="button"
              className="w-6 h-6 rounded-full flex items-center justify-center text-[#9b8d84] hover:bg-[#fff2ed] hover:text-[#ff5a36] transition-colors"
              aria-label="Previous month"
            >
              <span className="material-symbols-outlined text-[15px]">chevron_left</span>
            </button>
            <button
              type="button"
              className="w-6 h-6 rounded-full flex items-center justify-center text-[#ff5a36] bg-[#fff2ed] hover:bg-[#ffe6dd] transition-colors"
              aria-label="Next month"
            >
              <span className="material-symbols-outlined text-[15px]">chevron_right</span>
            </button>
          </div>

          {/* 3D Hexagonal Achievement Badge (Fully contained, unclipped) */}
          <div
            className="relative w-10 h-11 flex items-center justify-center filter drop-shadow-[0_3px_8px_rgba(255,90,54,0.22)] cursor-pointer group transition-transform duration-300 hover:scale-105"
            title="October Milestone Badge"
          >
            <svg
              className="w-full h-full text-primary-container"
              viewBox="0 0 100 115"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <defs>
                <linearGradient id="badgeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#3d302c" />
                  <stop offset="50%" stopColor="#25201d" />
                  <stop offset="100%" stopColor="#1a1614" />
                </linearGradient>
                <linearGradient id="badgeStroke" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stopColor="#ff7a45" />
                  <stop offset="50%" stopColor="#fe7944" />
                  <stop offset="100%" stopColor="#b52603" />
                </linearGradient>
              </defs>
              <polygon
                points="50 5, 95 30, 95 85, 50 110, 5 85, 5 30"
                fill="url(#badgeGrad)"
                stroke="url(#badgeStroke)"
                strokeWidth="4"
              />
              <polygon
                points="50 14, 87 35, 87 80, 50 101, 13 80, 13 35"
                fill="none"
                stroke="#ff5a36"
                strokeWidth="1.5"
                strokeDasharray="4 3"
                opacity="0.6"
              />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center text-white pointer-events-none">
              <span className="text-[13px] font-black leading-none tracking-tighter text-[#ffb4a3]">
                10
              </span>
              <span className="text-[7.5px] font-bold tracking-widest uppercase text-white/90 mt-0.5">
                OCT
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* ========================================================
          2. DAYS OF WEEK ROW (S M T W T F S)
          ======================================================== */}
      <div className="grid grid-cols-7 gap-1 text-center font-bold text-[11px] text-[#a59991]">
        {weekDays.map((d, i) => (
          <span key={i} className="py-0.5">
            {d}
          </span>
        ))}
      </div>

      {/* ========================================================
          3. CALENDAR MONTH GRID (1 - 31)
          ======================================================== */}
      <div className="grid grid-cols-7 gap-1 text-center text-[12px] font-semibold items-center">
        {daysInMonth.map((item) => {
          if (item.empty) {
            return <div key={item.key} className="h-7 w-7" />;
          }

          // Completed Day (Checkmark in theme)
          if (item.completed) {
            return (
              <div
                key={item.key}
                className="h-7 w-7 mx-auto rounded-full flex items-center justify-center relative cursor-pointer group"
                title={`Day ${item.day} - Completed!`}
              >
                {/* Checkmark circle badge */}
                <div className="w-6 h-6 rounded-full border-2 border-[#ff5a36] flex items-center justify-center text-[#ff5a36] bg-[#fff5f2] transition-transform duration-200 group-hover:scale-110 shadow-xs">
                  <span
                    className="material-symbols-outlined text-[15px]"
                    style={{ fontVariationSettings: "'FILL' 1" }}
                  >
                    check
                  </span>
                </div>
              </div>
            );
          }

          // Active Today (Green / Radiant Highlight circle with Day 12)
          if (item.isToday) {
            return (
              <div
                key={item.key}
                className="h-7 w-7 mx-auto rounded-full flex items-center justify-center relative cursor-pointer group"
                title="Today's Active Challenge"
              >
                <div className="w-6 h-6 rounded-full bg-emerald-500 text-white font-extrabold text-[11px] flex items-center justify-center shadow-md shadow-emerald-500/30 ring-2 ring-emerald-200 animate-pulse">
                  12
                </div>
              </div>
            );
          }

          // Upcoming Days
          return (
            <div
              key={item.key}
              className="h-7 w-7 mx-auto rounded-full flex items-center justify-center text-[#6e635b] hover:bg-[#fff5f2] hover:text-[#ff5a36] transition-colors cursor-pointer text-[12px]"
            >
              {item.day}
            </div>
          );
        })}
      </div>
    </div>
  );
}

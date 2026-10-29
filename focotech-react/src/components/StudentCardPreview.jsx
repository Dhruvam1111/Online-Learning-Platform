import React, { useState, useRef } from 'react';
import { DISCIPLINES, UNIVERSITIES, ACADEMIC_YEARS } from '../data/studentDisciplines';

export default function StudentCardPreview({
  studentName = '',
  universityName = '',
  disciplineId = 'business',
  customMajor = '',
  yearId = 'sophomore',
  email = '',
  studentId = 'STU-2026-8941',
  isEduVerified = false
}) {
  const [isFlipped, setIsFlipped] = useState(false);
  const [tilt, setTilt] = useState({ x: 0, y: 0, glareX: 50, glareY: 50 });
  const cardRef = useRef(null);

  const selectedDiscipline = DISCIPLINES.find(d => d.id === disciplineId) || DISCIPLINES[0];
  const selectedYear = ACADEMIC_YEARS.find(y => y.id === yearId) || ACADEMIC_YEARS[1];
  const universityObj = UNIVERSITIES.find(u => u.name === universityName) || UNIVERSITIES[0];

  const displayName = studentName.trim() || 'Alex Scholar';
  const displayMajor = customMajor.trim() || selectedDiscipline.name;
  const displayUni = universityName.trim() || 'University Campus';

  // 3D Tilt calculation
  const handleMouseMove = (e) => {
    if (!cardRef.current) return;
    const rect = cardRef.current.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;
    const centerX = rect.width / 2;
    const centerY = rect.height / 2;

    const rotateX = ((y - centerY) / centerY) * -12;
    const rotateY = ((x - centerX) / centerX) * 14;

    setTilt({
      x: rotateX,
      y: rotateY,
      glareX: (x / rect.width) * 100,
      glareY: (y / rect.height) * 100
    });
  };

  const handleMouseLeave = () => {
    setTilt({ x: 0, y: 0, glareX: 50, glareY: 50 });
  };

  return (
    <div className="flex flex-col items-center gap-4 w-full select-none">
      {/* Top Header & Flip Toggle */}
      <div className="flex items-center justify-between w-full max-w-sm px-2 text-xs font-semibold text-on-surface-variant">
        <span className="flex items-center gap-1.5 uppercase tracking-wider text-[11px] text-primary">
          <span className="material-symbols-outlined text-[16px]">badge</span>
          Live Digital Campus Pass
        </span>
        <button
          type="button"
          onClick={() => setIsFlipped(!isFlipped)}
          className="inline-flex items-center gap-1 text-primary hover:text-secondary-container transition-colors px-2 py-0.5 rounded-full hover:bg-primary-container/10 active:scale-95"
        >
          <span className="material-symbols-outlined text-[14px]">sync</span>
          {isFlipped ? 'Show Front' : 'Flip Card'}
        </button>
      </div>

      {/* Card Container with Perspective */}
      <div
        className="relative w-full max-w-sm h-[230px] sm:h-[250px] cursor-pointer"
        style={{ perspective: '1100px' }}
        onMouseMove={handleMouseMove}
        onMouseLeave={handleMouseLeave}
        onClick={() => setIsFlipped(!isFlipped)}
      >
        <div
          ref={cardRef}
          className="relative w-full h-full rounded-2xl transition-transform duration-500 ease-out shadow-2xl"
          style={{
            transformStyle: 'preserve-3d',
            transform: `rotateY(${isFlipped ? 180 + tilt.y : tilt.y}deg) rotateX(${tilt.x}deg)`
          }}
        >
          {/* ================= CARD FRONT ================= */}
          <div
            className={`absolute inset-0 rounded-2xl overflow-hidden p-5 flex flex-col justify-between border border-white/60 backdrop-blur-xl shadow-[0_20px_40px_rgba(28,25,23,0.12)] bg-gradient-to-br from-white via-surface-container-lowest to-surface-container-low`}
            style={{
              backfaceVisibility: 'hidden',
              WebkitBackfaceVisibility: 'hidden'
            }}
          >
            {/* Holographic foil shimmer glare */}
            <div
              className="pointer-events-none absolute inset-0 opacity-40 mix-blend-overlay transition-opacity duration-300"
              style={{
                background: `radial-gradient(circle at ${tilt.glareX}% ${tilt.glareY}%, rgba(255,255,255,0.85) 0%, rgba(255,90,54,0.3) 35%, transparent 70%)`
              }}
            />

            {/* Subtle Geometric Campus Mesh Pattern */}
            <div className="pointer-events-none absolute -right-12 -top-12 w-44 h-44 rounded-full bg-primary-container/10 blur-2xl" />
            <div
              className="pointer-events-none absolute -left-12 -bottom-12 w-44 h-44 rounded-full blur-2xl opacity-60"
              style={{ backgroundColor: selectedDiscipline.accentHex }}
            />

            {/* Card Header */}
            <div className="relative z-10 flex items-start justify-between gap-2">
              <div className="flex items-center gap-2.5">
                <div
                  className="w-9 h-9 rounded-xl flex items-center justify-center text-white shadow-sm font-bold text-sm"
                  style={{ backgroundColor: universityObj.color || '#b52603' }}
                >
                  <span className="material-symbols-outlined text-[20px]">school</span>
                </div>
                <div className="flex flex-col">
                  <span className="font-bold text-xs sm:text-sm text-on-surface tracking-tight leading-tight line-clamp-1">
                    {displayUni}
                  </span>
                  <div className="flex items-center gap-1.5">
                    <span className="text-[10px] uppercase font-bold tracking-wider text-primary">
                      Official Scholar ID
                    </span>
                    {isEduVerified && (
                      <span className="inline-flex items-center gap-0.5 text-[9px] font-bold text-emerald-700 bg-emerald-100/90 px-1.5 py-0.2 rounded-full border border-emerald-300">
                        <span className="material-symbols-outlined text-[10px]">verified</span>
                        .EDU Verified
                      </span>
                    )}
                  </div>
                </div>
              </div>

              {/* NFC Contactless Wave icon */}
              <div className="flex items-center gap-1 bg-surface-container px-2 py-1 rounded-full text-on-surface-variant/80 border border-outline-variant/30 text-[10px]">
                <span className="material-symbols-outlined text-[13px] animate-pulse">contactless</span>
                <span className="font-mono text-[9px] font-semibold">{selectedYear.gradYear}</span>
              </div>
            </div>

            {/* Card Middle: Student Profile Info */}
            <div className="relative z-10 flex items-center gap-3.5 my-auto">
              {/* Photo Avatar / Initials Tile */}
              <div className="relative shrink-0">
                <div
                  className="w-14 h-14 rounded-xl flex items-center justify-center text-white font-bold text-lg shadow-md ring-2 ring-white/90 overflow-hidden"
                  style={{
                    background: `linear-gradient(135deg, ${selectedDiscipline.accentHex} 0%, #ff5a36 100%)`
                  }}
                >
                  {displayName
                    .split(' ')
                    .map(n => n[0])
                    .join('')
                    .toUpperCase()
                    .slice(0, 2) || 'AS'}
                </div>
                {/* Active status pip */}
                <span className="absolute -bottom-1 -right-1 w-4 h-4 rounded-full bg-emerald-500 border-2 border-white flex items-center justify-center shadow-xs">
                  <span className="w-1.5 h-1.5 rounded-full bg-white" />
                </span>
              </div>

              {/* Name & Academic Credentials */}
              <div className="flex flex-col min-w-0 flex-1">
                <span className="font-headline-md font-bold text-on-surface text-base sm:text-lg tracking-tight truncate leading-tight">
                  {displayName}
                </span>

                {/* Major Badge with Discipline Icon */}
                <div className="inline-flex items-center gap-1.5 mt-1 px-2.5 py-0.5 rounded-full text-[11px] font-semibold bg-surface-container border border-outline-variant/30 text-on-surface truncate w-fit max-w-full">
                  <span className="text-[12px]">{selectedDiscipline.emoji}</span>
                  <span className="truncate">{displayMajor}</span>
                </div>

                <div className="flex items-center gap-2 mt-1 text-[10px] text-on-surface-variant font-medium">
                  <span>{selectedYear.label.split(' ')[0]}</span>
                  <span>•</span>
                  <span className="font-mono font-semibold text-primary">{studentId}</span>
                </div>
              </div>
            </div>

            {/* Card Footer: Micro QR & Hologram Barcode */}
            <div className="relative z-10 flex items-center justify-between pt-2 border-t border-outline-variant/20 text-[10px]">
              <div className="flex items-center gap-2">
                <div className="w-7 h-7 bg-white rounded-md p-0.5 border border-outline-variant/30 shadow-xs flex items-center justify-center">
                  <span className="material-symbols-outlined text-[20px] text-on-surface">qr_code_2</span>
                </div>
                <div className="flex flex-col">
                  <span className="text-[9px] font-bold text-on-surface uppercase tracking-wider">
                    All-Campus Access
                  </span>
                  <span className="text-[8px] text-on-surface-variant">Tap for Library, Labs &amp; Courses</span>
                </div>
              </div>

              {/* Hologram Emblem */}
              <div className="flex items-center gap-1 px-2 py-0.5 rounded-md bg-gradient-to-r from-amber-500/10 via-rose-500/10 to-primary/10 border border-primary/20 text-[9px] font-bold text-primary">
                <span className="material-symbols-outlined text-[11px]">shield_check</span>
                <span>FOCOTECH 2026</span>
              </div>
            </div>
          </div>

          {/* ================= CARD BACK ================= */}
          <div
            className="absolute inset-0 rounded-2xl overflow-hidden p-5 flex flex-col justify-between border border-white/60 backdrop-blur-xl shadow-[0_20px_40px_rgba(28,25,23,0.12)] bg-gradient-to-br from-surface-container-high via-surface-container to-surface-container-low"
            style={{
              transform: 'rotateY(180deg)',
              backfaceVisibility: 'hidden',
              WebkitBackfaceVisibility: 'hidden'
            }}
          >
            {/* Magnetic Stripe Bar */}
            <div className="-mx-5 -mt-2 h-7 bg-on-surface/90 shadow-inner flex items-center justify-end px-4">
              <span className="font-mono text-[8px] text-white/50 tracking-widest">DIGITAL PASS • ENCRYPTED</span>
            </div>

            {/* Signature & Security Seal */}
            <div className="flex items-center justify-between gap-3 my-2">
              <div className="flex-1 border-b-2 border-dashed border-outline-variant/60 pb-1">
                <span className="font-serif italic text-xs text-on-surface-variant font-bold opacity-80 block pl-2">
                  {displayName}
                </span>
                <span className="text-[8px] text-on-surface-variant uppercase tracking-wider">
                  Authorized Student Signature
                </span>
              </div>
              <div className="w-12 h-12 rounded-full border-2 border-primary/40 flex items-center justify-center p-1 text-center bg-primary-fixed/30">
                <span className="text-[7px] font-black uppercase text-primary leading-tight">
                  ACADEMIC COUNCIL
                </span>
              </div>
            </div>

            {/* Campus Facilities Privileges List */}
            <div className="bg-white/80 rounded-lg p-2 border border-outline-variant/30 text-[9px] text-on-surface-variant space-y-0.5">
              <div className="flex justify-between font-semibold text-on-surface">
                <span>Perk Tier:</span>
                <span className="text-primary font-bold">100% Free College Scholar</span>
              </div>
              <div className="flex justify-between">
                <span>Campus Network:</span>
                <span>Global Peer Hub + Discord Pods</span>
              </div>
              <div className="flex justify-between">
                <span>Discipline Track:</span>
                <span className="font-medium text-on-surface">{selectedDiscipline.name}</span>
              </div>
            </div>

            {/* Barcode Strip */}
            <div className="flex flex-col items-center gap-0.5">
              <div className="h-6 w-full max-w-[200px] flex items-stretch justify-between px-2 bg-white rounded py-1 border border-outline-variant/30">
                {/* Fake Barcode Lines */}
                {[2, 1, 3, 1, 4, 2, 1, 3, 2, 4, 1, 2, 3, 1, 2, 4, 1, 3, 2].map((w, i) => (
                  <div
                    key={i}
                    className="bg-on-surface h-full"
                    style={{ width: `${w * 1.5}px` }}
                  />
                ))}
              </div>
              <span className="font-mono text-[8px] text-on-surface-variant">{studentId}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Helpful Hint */}
      <span className="text-[11px] text-on-surface-variant/70 italic flex items-center gap-1">
        <span className="material-symbols-outlined text-[13px]">touch_app</span>
        Move cursor to tilt 3D pass • Click or tap to flip
      </span>
    </div>
  );
}

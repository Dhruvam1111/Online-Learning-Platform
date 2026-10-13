import React, { useState } from 'react';

export default function HeroVisual() {
  const [hoveredElement, setHoveredElement] = useState(null);

  return (
    <div
      className="relative w-full max-w-[500px] sm:max-w-[540px] lg:max-w-[580px] aspect-square flex items-center justify-center mx-auto select-none animate-fadeInUp"
      style={{ animationDelay: '120ms' }}
    >
      {/* Ambient background glow */}
      <div className="pointer-events-none absolute inset-4 rounded-full bg-gradient-to-tr from-primary-fixed/40 via-secondary-fixed/30 to-tertiary-fixed/30 blur-3xl -z-10 animate-orb-glow" />

      {/* Main Composite Graphic from Image 2 */}
      {/* Matches exact proportions, lighting, colors, borders, and student subject holding laptop */}
      <div className="relative w-full h-full flex items-center justify-center">
        <img
          src="/assets/hero_full_transparent.png"
          alt="Student holding laptop with 3D rocket, trophy, and creative edtech badges"
          className="w-full h-full object-contain filter drop-shadow-[0_12px_32px_rgba(241,89,42,0.14)] transition-transform duration-700 ease-out hover:scale-[1.02]"
          draggable="false"
        />

        {/* =========================================================================
            INTERACTIVE OVERLAYS & MICRO-ANIMATED FLOATING LAYERS
            Preserves exact visual positioning of Image 2 with fluid responsive CSS
            ========================================================================= */}

        {/* 1. Floating 3D Rocket in Upper-Left Corner */}
        <div
          className="absolute top-[3%] left-[2%] w-[33%] aspect-[3/4] cursor-pointer group z-20 animate-rocket"
          onMouseEnter={() => setHoveredElement('rocket')}
          onMouseLeave={() => setHoveredElement(null)}
          title="Launch Your Career Faster"
        >
          {/* Invisible hover trigger area matching the rocket */}
          <div className="w-full h-full relative">
            {/* Interactive tooltip bubble on hover */}
            <div
              className={`absolute -top-3 left-1/2 -translate-x-1/2 bg-surface-container-lowest/95 backdrop-blur-md px-3 py-1.5 rounded-full shadow-xl border border-primary/20 text-xs font-bold text-primary flex items-center gap-1.5 whitespace-nowrap transition-all duration-300 pointer-events-none ${
                hoveredElement === 'rocket'
                  ? 'opacity-100 -translate-y-2 scale-100'
                  : 'opacity-0 translate-y-1 scale-95'
              }`}
            >
              <span>🚀</span>
              <span>Fast-Track Skills</span>
            </div>
          </div>
        </div>

        {/* 2. Floating 3D Golden Trophy in Lower-Right Corner */}
        <div
          className="absolute bottom-[2%] right-[2%] w-[34%] aspect-square cursor-pointer group z-20 animate-trophy"
          onMouseEnter={() => setHoveredElement('trophy')}
          onMouseLeave={() => setHoveredElement(null)}
          title="Certified Student Mastery"
        >
          <div className="w-full h-full relative">
            {/* Interactive tooltip bubble on hover */}
            <div
              className={`absolute -top-3 left-1/2 -translate-x-1/2 bg-surface-container-lowest/95 backdrop-blur-md px-3 py-1.5 rounded-full shadow-xl border border-secondary/20 text-xs font-bold text-secondary flex items-center gap-1.5 whitespace-nowrap transition-all duration-300 pointer-events-none ${
                hoveredElement === 'trophy'
                  ? 'opacity-100 -translate-y-2 scale-100'
                  : 'opacity-0 translate-y-1 scale-95'
              }`}
            >
              <span>🏆</span>
              <span>#02 Ranked Builder</span>
            </div>
          </div>
        </div>

        {/* 3. Upper-Right Decorative Shapes: Purple Squircle & Coral Circle */}
        <div
          className="absolute top-[8%] right-[8%] w-[25%] aspect-square cursor-pointer z-10 animate-shape"
          onMouseEnter={() => setHoveredElement('shapes-tr')}
          onMouseLeave={() => setHoveredElement(null)}
        >
          <div className="w-full h-full relative">
            <div
              className={`absolute -bottom-2 right-0 bg-surface-container-lowest/95 backdrop-blur-md px-2.5 py-1 rounded-full shadow-md text-[11px] font-bold text-on-surface whitespace-nowrap transition-all duration-300 pointer-events-none ${
                hoveredElement === 'shapes-tr'
                  ? 'opacity-100 -translate-y-1'
                  : 'opacity-0'
              }`}
            >
              Creative UI/UX
            </div>
          </div>
        </div>

        {/* 4. Lower-Left Decorative Shapes: Yellow Squircle & Blue Circle */}
        <div
          className="absolute bottom-[8%] left-[8%] w-[24%] aspect-square cursor-pointer z-10 animate-shape"
          style={{ animationDelay: '1.2s' }}
          onMouseEnter={() => setHoveredElement('shapes-bl')}
          onMouseLeave={() => setHoveredElement(null)}
        >
          <div className="w-full h-full relative">
            <div
              className={`absolute -top-3 left-0 bg-surface-container-lowest/95 backdrop-blur-md px-2.5 py-1 rounded-full shadow-md text-[11px] font-bold text-on-surface whitespace-nowrap transition-all duration-300 pointer-events-none ${
                hoveredElement === 'shapes-bl'
                  ? 'opacity-100 -translate-y-1'
                  : 'opacity-0'
              }`}
            >
              Full-Stack Code
            </div>
          </div>
        </div>

      </div>
    </div>
  );
}
